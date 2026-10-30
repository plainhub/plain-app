use super::{
    graphql::parts,
    values::{c_string, input},
};
use std::ffi::c_char;

fn open(target: Option<&str>) -> Result<u64, String> {
    let (runtime, executor) = parts()?;
    let _entered = runtime.enter();
    match target {
        Some(target) => executor.open_peer_subscriptions(serde_json::from_str(target).map_err(|error| error.to_string())?, Default::default()),
        None => executor.open_subscriptions(Default::default()),
    }
}
fn send(handle: u64, message: &str) -> Result<(), String> {
    let (runtime, executor) = parts()?;
    let _entered = runtime.enter();
    executor.subscription_session(handle)?.send(message)
}
fn read(handle: u64) -> Result<String, String> {
    let (runtime, executor) = parts()?;
    let session = executor.subscription_session(handle)?;
    runtime.block_on(session.read())
}
fn close(handle: u64) -> Result<(), String> {
    let core = super::server::CORE
        .lock()
        .map_err(|error| error.to_string())?;
    if let Some(core) = core.as_ref() {
        core.server.native_graphql()?.close_subscriptions(handle);
    }
    Ok(())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_graphql_subscription_open(target: *const c_char) -> *mut c_char {
    c_string(
        match (if target.is_null() { Ok(None) } else { input(target).map(Some) }).and_then(|target| open(target.as_deref())) {
            Ok(handle) => serde_json::json!({"handle":handle}),
            Err(error) => serde_json::json!({"error":error}),
        }
        .to_string(),
    )
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_graphql_subscription_send(
    handle: u64,
    message: *const c_char,
) -> *mut c_char {
    c_string(
        input(message)
            .and_then(|message| send(handle, &message))
            .err()
            .unwrap_or_default(),
    )
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_graphql_subscription_read(handle: u64) -> *mut c_char {
    c_string(
        match read(handle) {
            Ok(value) => serde_json::json!({"value":value}),
            Err(error) => serde_json::json!({"error":error}),
        }
        .to_string(),
    )
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_graphql_subscription_close(handle: u64) -> *mut c_char {
    c_string(close(handle).err().unwrap_or_default())
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{
        EnvUnowned,
        errors::ThrowRuntimeExAndDefault,
        objects::{JObject, JString},
        sys::{jlong, jstring},
    };
    fn fail(env: &mut jni::Env<'_>, error: String) -> jni::errors::Result<()> {
        env.throw_new(
            jni::jni_str!("java/lang/IllegalStateException"),
            jni::strings::JNIString::from(error),
        )
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_graphqlSubscriptionOpen(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        target: JString<'_>,
    ) -> jlong {
        env.with_env(|env| -> jni::errors::Result<_> {
            let target = if target.is_null() { None } else { Some(target.to_string()) };
            match open(target.as_deref()) {
                Ok(handle) => Ok(handle as jlong),
                Err(error) => {
                    fail(env, error)?;
                    Ok(0)
                }
            }
        })
        .resolve::<ThrowRuntimeExAndDefault>()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_graphqlSubscriptionSend(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        handle: jlong,
        message: JString<'_>,
    ) {
        env.with_env(|env| -> jni::errors::Result<_> {
            if let Err(error) = send(handle as u64, &message.to_string()) {
                fail(env, error)?;
            }
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_graphqlSubscriptionRead(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        handle: jlong,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let value = match read(handle as u64) {
                Ok(value) => value,
                Err(error) => {
                    fail(env, error)?;
                    String::new()
                }
            };
            env.new_string(value)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_graphqlSubscriptionClose(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        handle: jlong,
    ) {
        env.with_env(|env| -> jni::errors::Result<_> {
            if let Err(error) = close(handle as u64) {
                fail(env, error)?;
            }
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }
}
