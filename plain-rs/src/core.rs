use super::{c_string, input};
use plain_server::content_api::ContentServer;
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
fn start(path: &str, token: &str, config: &str) -> Result<String, String> {
    let mut guard = CORE.lock().map_err(|e| e.to_string())?;
    let config: serde_json::Value = serde_json::from_str(config).map_err(|e| e.to_string())?;
    let port = |name: &str| {
        config[name]
            .as_u64()
            .and_then(|v| u16::try_from(v).ok())
            .ok_or_else(|| format!("Invalid {name}"))
    };
    let prefs = super::prefs()?;
    let directory = prefs.path().parent().ok_or("Missing TLS directory")?;
    let (cert, key) = plain_server::tls_identity::identity(&directory.join("tls-identity.json"))?;
    if let Some(core) = guard.as_mut() {
        if core.path != PathBuf::from(path) || core.token != token {
            return Err("Rust core already initialized with another session".into());
        }
        if core
            .runtime
            .block_on(core.server.public_generation())
            .is_none()
        {
            core.server
                .set_build_debug(config["debug"].as_bool().unwrap_or(false));
            core.server
                .set_web_root(config["webRoot"].as_str().unwrap_or_default());
            let (http, _) = core.runtime.block_on(core.server.start_public(
                port("httpPort")?,
                port("httpsPort")?,
                cert,
                key,
            ))?;
            core.server.port = http;
        }
    } else {
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .worker_threads(2)
            .enable_all()
            .build()
            .map_err(|e| e.to_string())?;
        let server = runtime.block_on(ContentServer::start_http(
            std::path::Path::new(path),
            token,
            prefs.clone(),
            port("httpPort")?,
            port("httpsPort")?,
            cert,
            key,
            config["debug"].as_bool().unwrap_or(false),
            config["webRoot"].as_str().unwrap_or_default(),
        ))?;
        *guard = Some(Core {
            path: PathBuf::from(path),
            token: token.into(),
            server,
            runtime,
        });
    }
    let core = guard.as_ref().unwrap();
    let (http, https) = core
        .runtime
        .block_on(core.server.http_ports())
        .ok_or("HTTP server stopped during startup")?;
    Ok(serde_json::json!({"httpPort":http, "httpsPort":https,
        "generation":core.runtime.block_on(core.server.public_generation()).ok_or("HTTP server stopped during startup")?}).to_string())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_core_start(
    path: *const c_char,
    token: *const c_char,
    config: *const c_char,
) -> *mut c_char {
    c_string(
        match input(path).and_then(|path| {
            input(token)
                .and_then(|token| input(config).and_then(|config| start(&path, &token, &config)))
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
            let result = match start(&path.to_string(), &token.to_string(), &config.to_string()) {
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
