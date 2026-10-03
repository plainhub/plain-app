use super::{c_string, input};
use plain_rs::content_api::ContentServer;
use std::ffi::c_char;
use std::{path::PathBuf, sync::Mutex};
use tokio::runtime::Runtime;
struct Core {
    path: PathBuf,
    token: String,
    server: ContentServer,
    runtime: Runtime,
}
static CORE: Mutex<Option<Core>> = Mutex::new(None);
fn start(path: &str, token: &str) -> Result<u16, String> {
    let mut core = CORE.lock().map_err(|e| e.to_string())?;
    if let Some(core) = core.as_ref() {
        if core.path == PathBuf::from(path) && core.token == token {
            return Ok(core.server.port);
        }
        return Err("Rust core already initialized with another session".into());
    }
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(2)
        .enable_all()
        .build()
        .map_err(|e| e.to_string())?;
    let server = runtime.block_on(async {
        ContentServer::start(std::path::Path::new(path), token, super::prefs()?)
    })?;
    let port = server.port;
    *core = Some(Core {
        path: PathBuf::from(path),
        token: token.into(),
        server,
        runtime,
    });
    Ok(port)
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_core_start(path: *const c_char, token: *const c_char) -> *mut c_char {
    c_string(
        match input(path).and_then(|path| input(token).and_then(|token| start(&path, &token))) {
            Ok(port) => port.to_string(),
            Err(error) => format!("ERROR:{error}"),
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
fn public_start(config: &str) -> Result<String, String> {
    let config: serde_json::Value = serde_json::from_str(config).map_err(|e| e.to_string())?;
    let port = |name: &str| {
        config[name]
            .as_u64()
            .and_then(|v| u16::try_from(v).ok())
            .ok_or_else(|| format!("Invalid {name}"))
    };
    let (cert, key) = super::tls::identity()?;
    let core = CORE.lock().map_err(|e| e.to_string())?;
    let core = core.as_ref().ok_or("Rust core is not initialized")?;
    let (http, https) = core.runtime.block_on(core.server.start_public(
        port("httpPort")?,
        port("httpsPort")?,
        cert,
        key,
    ))?;
    Ok(serde_json::json!({"httpPort":http,"httpsPort":https}).to_string())
}
fn public_stop() -> Result<(), String> {
    let core = CORE.lock().map_err(|e| e.to_string())?;
    if let Some(core) = core.as_ref() {
        core.runtime.block_on(core.server.stop_public());
    }
    Ok(())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_http_start(config: *const c_char) -> *mut c_char {
    c_string(
        match input(config).and_then(|config| public_start(&config)) {
            Ok(value) => value,
            Err(e) => format!("ERROR:{e}"),
        },
    )
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_http_stop() -> *mut c_char {
    match public_stop() {
        Ok(()) => std::ptr::null_mut(),
        Err(e) => c_string(e),
    }
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_tls_action(config: *const c_char) -> *mut c_char {
    c_string(match input(config).and_then(|v| super::tls::action(&v)) {
        Ok(value) => value,
        Err(e) => format!("ERROR:{e}"),
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
    pub extern "system" fn Java_com_ismartcoding_plain_api_RustCoreBridge_tlsNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        config: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let value = match super::super::tls::action(&config.to_string()) {
                Ok(v) => v,
                Err(e) => format!("ERROR:{e}"),
            };
            env.new_string(value)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_api_RustCoreBridge_startNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        path: JString<'_>,
        token: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = match start(&path.to_string(), &token.to_string()) {
                Ok(port) => port.to_string(),
                Err(e) => format!("ERROR:{e}"),
            };
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_api_RustCoreBridge_startPublicNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        config: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let value = match public_start(&config.to_string()) {
                Ok(v) => v,
                Err(e) => format!("ERROR:{e}"),
            };
            env.new_string(value)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_api_RustCoreBridge_stopPublicNative(
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
