use crate::ffi::values::c_string;
use crate::ffi::values::input;
use plain_server::assembly::server::ContentServer;
use std::ffi::c_char;
use std::{path::PathBuf, sync::Mutex};
use tokio::runtime::Runtime;
pub(crate) struct Core {
    pub(crate) path: PathBuf,
    pub(crate) token: String,
    pub(crate) server: ContentServer,
    pub(crate) runtime: Runtime,
}
pub(crate) static CORE: Mutex<Option<Core>> = Mutex::new(None);

#[unsafe(no_mangle)]
pub extern "C" fn plain_core_start(
    path: *const c_char,
    token: *const c_char,
    config: *const c_char,
) -> *mut c_char {
    c_string(
        match input(path).and_then(|path| {
            input(token).and_then(|token| {
                input(config).and_then(|config| crate::bootstrap::start(&path, &token, &config))
            })
        }) {
            Ok(value) => value,
            Err(error) => public_error(error),
        },
    )
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_core_stop() {
    if let Ok(mut core) = CORE.lock() {
        if let Some(core) = core.take() {
            core.runtime.block_on(core.server.shutdown());
        }
    }
}
fn public_error(error: String) -> String {
    serde_json::json!({"errorCode":"START_FAILED", "error":error}).to_string()
}
fn public_stop() -> Result<(), String> {
    let core = CORE.lock().map_err(|e| e.to_string())?;
    if let Some(core) = core.as_ref() {
        core.runtime.block_on(core.server.stop_public());
    }
    Ok(())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_http_stop() -> *mut c_char {
    match public_stop() {
        Ok(()) => std::ptr::null_mut(),
        Err(e) => c_string(e),
    }
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
    pub extern "system" fn Java_com_ismartcoding_plain_api_RustCoreBridge_startNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        path: JString<'_>,
        token: JString<'_>,
        config: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = match crate::bootstrap::start(
                &path.to_string(),
                &token.to_string(),
                &config.to_string(),
            ) {
                Ok(value) => value,
                Err(e) => public_error(e),
            };
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_api_RustCoreBridge_stopNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(match public_stop() {
                Ok(()) => String::new(),
                Err(e) => e,
            })
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
