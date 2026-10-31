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
    serde_json::json!({"errors":[{"message":error}]}).to_string()
}
fn public(request: &str) -> String {
    let result = (|| {
        let (runtime, executor) = parts()?;
        let parsed = serde_json::from_str(request).map_err(|e| e.to_string())?;
        serde_json::to_string(&runtime.block_on(executor.execute(parsed)))
            .map_err(|e| e.to_string())
    })();
    result.unwrap_or_else(errors)
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
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
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
