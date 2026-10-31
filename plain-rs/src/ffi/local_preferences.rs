use super::management::{parts, reply};
use super::values::{c_string, input};
use std::ffi::c_char;

fn reset_password() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.reset_password()
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_reset_password() -> *mut c_char {
    c_string(reset_password())
}

fn reset_url_token() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.reset_url_token()
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_reset_url_token() -> *mut c_char {
    c_string(reset_url_token())
}

fn reset_adb_token() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.reset_adb_token()
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_reset_adb_token() -> *mut c_char {
    c_string(reset_adb_token())
}

fn set_notification_filter_mode(mode: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.set_notification_filter_mode(mode))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_set_notification_filter_mode(mode: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let mode = input(mode)?;
        Ok(set_notification_filter_mode(mode))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn clear_notification_filter_apps() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.clear_notification_filter_apps())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_clear_notification_filter_apps() -> *mut c_char {
    c_string(clear_notification_filter_apps())
}

fn scan_history() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.scan_history()
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_scan_history() -> *mut c_char {
    c_string(scan_history())
}

fn add_scan_history(text: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.add_scan_history(text)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_add_scan_history(text: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let text = input(text)?;
        Ok(add_scan_history(text))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn remove_scan_history(text: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.remove_scan_history(text)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_remove_scan_history(text: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let text = input(text)?;
        Ok(remove_scan_history(text))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resetPassword(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(reset_password()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resetUrlToken(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(reset_url_token()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resetAdbToken(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(reset_adb_token()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_setNotificationFilterMode(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        mode: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(set_notification_filter_mode(mode.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_clearNotificationFilterApps(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(clear_notification_filter_apps())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_scanHistory(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(scan_history()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_addScanHistory(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        text: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(add_scan_history(text.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_removeScanHistory(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        text: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(remove_scan_history(text.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
