use super::management::{parts, reply};
use super::values::{c_string, input};
use std::ffi::c_char;

fn is_pairing_request_current(id: String, signature: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.is_pairing_request_current(id, signature)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_is_pairing_request_current(
    id: *const c_char,
    signature: *const c_char,
) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let id = input(id)?;
        let signature = input(signature)?;
        Ok(is_pairing_request_current(id, signature))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn reconnect_peers() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.reconnect_peers())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_reconnect_peers() -> *mut c_char {
    c_string(reconnect_peers())
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_isPairingRequestCurrent(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
        signature: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(is_pairing_request_current(
                id.to_string(),
                signature.to_string(),
            ))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reconnectPeers(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(reconnect_peers()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }
}
