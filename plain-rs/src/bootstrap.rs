use plain_server::assembly::server::ContentServer;
use std::path::PathBuf;

pub(crate) fn start(path: &str, token: &str, config: &str) -> Result<String, String> {
    let mut guard = crate::ffi::server::CORE.lock().map_err(|e| e.to_string())?;
    let config: serde_json::Value = serde_json::from_str(config).map_err(|e| e.to_string())?;
    let prefs = crate::prefs()?;
    let directory = prefs.path().parent().ok_or("Missing TLS directory")?;
    let (cert, key) =
        plain_server::http::tls_identity::identity(&directory.join("tls-identity.json"))?;
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
                plain_rs::prefs::user::snapshot(&prefs).map_err(|e|e.to_string())?.http_port as u16,
                plain_rs::prefs::user::snapshot(&prefs).map_err(|e|e.to_string())?.https_port as u16,
                cert,
                key,
            ))?;
            core.server.port = http;
        }
    } else {
        plain_rs::prefs::system::bootstrap(&prefs).map_err(|e| e.to_string())?;
        if plain_rs::prefs::user::snapshot(&prefs).map_err(|e|e.to_string())?.device_name.is_empty() {
            let name=config["deviceName"].as_str().ok_or("Missing device name")?;
            plain_rs::prefs::user::patch(&prefs,plain_rs::prefs::user::UserSettingsPatch{device_name:Some(name.into()),..Default::default()}).map_err(|e|e.to_string())?;
        }
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .worker_threads(2)
            .enable_all()
            .build()
            .map_err(|e| e.to_string())?;
        let server = runtime.block_on(ContentServer::start_http(
            std::path::Path::new(path),
            token,
            prefs.clone(),
            plain_rs::prefs::user::snapshot(&prefs).map_err(|e|e.to_string())?.http_port as u16,
            plain_rs::prefs::user::snapshot(&prefs).map_err(|e|e.to_string())?.https_port as u16,
            cert,
            key,
            config["debug"].as_bool().unwrap_or(false),
            config["webRoot"].as_str().unwrap_or_default(),
        ))?;
        *guard = Some(crate::ffi::server::Core {
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
    plain_rs::prefs::user::patch(&prefs, plain_rs::prefs::user::UserSettingsPatch { http_port:Some(http as i32), https_port:Some(https as i32), ..Default::default() }).map_err(|e|e.to_string())?;
    Ok(serde_json::json!({"httpPort":http, "httpsPort":https,
        "generation":core.runtime.block_on(core.server.public_generation()).ok_or("HTTP server stopped during startup")?}).to_string())
}
