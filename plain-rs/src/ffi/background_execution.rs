use super::{host::bridge, management::reply, values::{c_string, input}};
use std::ffi::c_char;

fn grant(id: String, deadline_ms: i64) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        let _entered = runtime.enter();
        host.grant_background_execution(id, deadline_ms)?;
        Ok(true)
    })())
}

fn revoke(id: &str) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.revoke_background_execution(id))?;
        Ok(true)
    })())
}

fn execution_state_changed() -> String {
    reply!((|| -> Result<bool, String> {
        let (_, host) = bridge()?;
        host.execution_state_changed()?;
        Ok(true)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_execution_state_changed() -> *mut c_char {
    c_string(execution_state_changed())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_grant_background_execution(
    id: *const c_char,
    deadline_ms: i64,
) -> *mut c_char {
    c_string(match input(id) {
        Ok(id) => grant(id, deadline_ms),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_revoke_background_execution(id: *const c_char) -> *mut c_char {
    c_string(match input(id) {
        Ok(id) => revoke(&id),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::{jlong, jstring}};

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_executionStateChanged(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(execution_state_changed()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_grantBackgroundExecution(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
        deadline_ms: jlong,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(grant(id.to_string(), deadline_ms))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_revokeBackgroundExecution(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(revoke(&id.to_string())) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }
}
