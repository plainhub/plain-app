use super::values::{c_string, input};
use plain_server::native::graphql::NativeGraphqlExecutor;
use std::ffi::c_char;
use tokio::runtime::Handle;

pub(super) fn parts() -> Result<(Handle, NativeGraphqlExecutor), String> {
    super::server::with_core(|core| {
        core.server
            .native_graphql()
            .map(|executor| (core.runtime.handle().clone(), executor))
    })?
}
fn errors(error: String) -> String {
    plain_libs::serde_json::json!({"errors":[{"message":error}]}).to_string()
}
fn execute_on_runtime<F>(runtime: &Handle, operation: F) -> Result<F::Output, String>
where
    F: std::future::Future + Send + 'static,
    F::Output: Send + 'static,
{
    runtime
        .block_on(runtime.spawn(operation))
        .map_err(|error| error.to_string())
}
fn execute_graphql(
    request: &str,
    runtime: &Handle,
    executor: NativeGraphqlExecutor,
) -> Result<String, String> {
    let mut body: plain_libs::serde_json::Value =
        plain_libs::serde_json::from_str(request).map_err(|error| error.to_string())?;
    let target = body
        .get_mut("extensions")
        .and_then(plain_libs::serde_json::Value::as_object_mut)
        .and_then(|extensions| extensions.remove("nativeTarget"))
        .map(
            plain_libs::serde_json::from_value::<
                plain_rs::infrastructure::protocols::graphql::PeerSubscriptionTarget,
            >,
        )
        .transpose()
        .map_err(|error| error.to_string())?;
    let parsed = plain_libs::serde_json::from_value(body).map_err(|error| error.to_string())?;
    let selected = target
        .map(|target| super::server::with_core(|core| (target, core.server.runtime().clone())))
        .transpose()?;
    execute_on_runtime(runtime, async move {
        let response = if let Some((target, state)) = selected {
            let selected = || -> Result<(), String> {
                let peer = plain_rs::infrastructure::db::chat_store::peers::get(
                    &state.db,
                    &target.peer_id,
                )
                .map_err(|error| error.to_string())?
                .ok_or("PEER_REMOVED")?;
                if !peer.is_paired()
                    || peer.public_key != target.public_key
                    || peer.token.is_empty()
                {
                    return Err("PEER_IDENTITY_CHANGED".into());
                }
                Ok(())
            };
            selected()?;
            let response = executor
                .execute_peer_scoped(
                    &target.peer_id,
                    parsed,
                    Default::default(),
                    std::time::Duration::from_secs(30),
                )
                .await?;
            selected()?;
            response
        } else {
            executor.execute(parsed).await
        };
        plain_libs::serde_json::to_string(&response).map_err(|error| error.to_string())
    })?
}
fn public(request: &str) -> String {
    parts()
        .and_then(|(runtime, executor)| execute_graphql(request, &runtime, executor))
        .unwrap_or_else(errors)
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_graphql(request: *const c_char) -> *mut c_char {
    c_string(match input(request) {
        Ok(request) => public(&request),
        Err(error) => errors(error),
    })
}
#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{
        EnvUnowned,
        errors::ThrowRuntimeExAndDefault,
        objects::{JObject, JString},
        sys::jstring,
    };
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_graphql(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        request: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(public(&request.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}

#[cfg(test)]
#[path = "../../tests/unit/ffi/graphql.rs"]
mod tests;
