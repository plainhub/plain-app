use plain_server::server::ContentServer;
use plain_rs::libs::platform::system::log;
use plain_rs::libs::platform::system::log::Level;
use std::sync::Arc;

pub(crate) fn start(data_dir: &str, token: &str, config: &str) -> Result<String, String> {
    let mut guard = crate::ffi::server::CORE.lock().map_err(|e| e.to_string())?;
    let config: plain_rs::libs::host::startup::StartupConfig =
        serde_json::from_str(config).map_err(|error| error.to_string())?;
    let cache_directory = config
        .cache_dir
        .clone()
        .ok_or("Startup cache directory required")?;
    let directory = std::path::Path::new(data_dir);
    if !directory.is_absolute() {
        return Err("Absolute data directory required".into());
    }
    let paths = plain_rs::libs::data_paths::DataPaths::new(directory);
    if let Some(core) = guard.as_ref() {
        if core.server.data_directory() != directory
            || core.token != token
            || core.server.cache_paths().root() != std::path::Path::new(&cache_directory)
        {
            return Err("Rust core already initialized with another session".into());
        }
    } else {
        // Apply a pending restore and acquire the directory lock before loading preferences.
        let _data_directory = plain_rs::libs::data_paths::directory::DataDirectory::open(
            directory,
            std::path::Path::new(&cache_directory),
        )?;
        plain_rs::libs::platform::system::log::init_file(&paths.rust_log_file(), "info")?;
        let prefs =
            Arc::new(plain_rs::features::prefs::Prefs::load(&paths.prefs()).map_err(|e| e.to_string())?);
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .worker_threads(2)
            .enable_all()
            .build()
            .map_err(|e| e.to_string())?;
        let mut server = runtime.block_on(async {
            ContentServer::start_core_with_config(directory, token, prefs, config)
        })?;
        match runtime.block_on(server.start_configured_public()) {
            Ok((http, _)) => server.port = http,
            Err(error) => log::write_log(
                Level::Error,
                &format!("HTTP server startup failed: {error}"),
            ),
        }
        crate::ffi::host::install(&runtime, &server)?;
        *guard = Some(crate::ffi::server::Core {
            token: token.into(),
            server,
            runtime,
        });
    }
    let core = guard.as_ref().unwrap();
    Ok(serde_json::json!({
        "logFile":core.server.log_file()?,
        "sharedPreviewDir":core.server.cache_paths().shared_previews()})
    .to_string())
}
