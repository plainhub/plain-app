use super::management::{parts, reply};
use super::values::{c_string, input};
use std::ffi::c_char;

fn lan_ipv4_addresses() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.lan_ipv4_addresses())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_lan_ipv4_addresses() -> *mut c_char {
    c_string(lan_ipv4_addresses())
}

fn acquire_mdns_debug_session(token: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.acquire_mdns_debug_session(token))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_acquire_mdns_debug_session(token: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let token = input(token)?;
        Ok(acquire_mdns_debug_session(token))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn release_mdns_debug_session(token: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.release_mdns_debug_session(token))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_release_mdns_debug_session(token: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let token = input(token)?;
        Ok(release_mdns_debug_session(token))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_lanIpv4Addresses(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(lan_ipv4_addresses())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_acquireMdnsDebugSession(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(acquire_mdns_debug_session(token.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_releaseMdnsDebugSession(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(release_mdns_debug_session(token.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
