use plain_server::assembly::server::ContentServer;
use std::path::PathBuf;

pub(crate) fn start(path: &str, token: &str, config: &str) -> Result<String, String> {
    let mut guard = crate::ffi::server::CORE.lock().map_err(|e| e.to_string())?;
    let config: serde_json::Value = serde_json::from_str(config).map_err(|e| e.to_string())?;
    let port = |name: &str| {
        config[name]
            .as_u64()
            .and_then(|v| u16::try_from(v).ok())
            .ok_or_else(|| format!("Invalid {name}"))
    };
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
    Ok(serde_json::json!({"httpPort":http, "httpsPort":https,
        "generation":core.runtime.block_on(core.server.public_generation()).ok_or("HTTP server stopped during startup")?}).to_string())
}
