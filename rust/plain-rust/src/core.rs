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
    let runtime = tokio::runtime::Builder::new_multi_thread().worker_threads(2).enable_all().build().map_err(|e| e.to_string())?;
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
}
