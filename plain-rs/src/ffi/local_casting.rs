use super::management::{parts, reply};
use super::values::{c_string, input};
use std::ffi::c_char;

fn dlna_receiver() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.dlna_receiver())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_dlna_receiver() -> *mut c_char {
    c_string(dlna_receiver())
}

fn retry_dlna_receiver() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.retry_dlna_receiver())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_retry_dlna_receiver() -> *mut c_char {
    c_string(retry_dlna_receiver())
}

fn remove_dlna_rule(ip: String, allowed: bool) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.remove_dlna_rule(ip, allowed))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_remove_dlna_rule(ip: *const c_char, allowed: bool) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let ip = input(ip)?;
        Ok(remove_dlna_rule(ip, allowed))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn accept_dlna_cast(remember: bool) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.accept_dlna_cast(remember))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_accept_dlna_cast(remember: bool) -> *mut c_char {
    c_string(accept_dlna_cast(remember))
}

fn reject_dlna_cast(remember: bool) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.reject_dlna_cast(remember))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_reject_dlna_cast(remember: bool) -> *mut c_char {
    c_string(reject_dlna_cast(remember))
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::{jboolean, jstring}};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_dlnaReceiver(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(dlna_receiver()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_retryDlnaReceiver(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(retry_dlna_receiver()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_removeDlnaRule(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        ip: JString<'_>,
        allowed: jboolean,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(remove_dlna_rule(ip.to_string(), allowed))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_acceptDlnaCast(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        remember: jboolean,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(accept_dlna_cast(remember)) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_rejectDlnaCast(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        remember: jboolean,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(reject_dlna_cast(remember)) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }
}
