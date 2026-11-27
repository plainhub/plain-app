use plain_rs::infrastructure::platform::system::log;
use plain_rs::infrastructure::platform::system::log::Level;
use plain_server::server::ContentServer;
use std::sync::Arc;

pub(crate) fn start(data_dir: &str, token: &str, config: &str) -> Result<String, String> {
    let mut guard = crate::ffi::server::CORE.lock().map_err(|e| e.to_string())?;
    let config: plain_rs::infrastructure::host::startup::StartupConfig =
        plain_libs::serde_json::from_str(config).map_err(|error| error.to_string())?;
    let cache_directory = config
        .cache_dir
        .clone()
        .ok_or("Startup cache directory required")?;
    let directory = std::path::Path::new(data_dir);
    if !directory.is_absolute() {
        return Err("Absolute data directory required".into());
    }
    let paths = plain_rs::infrastructure::data_paths::DataPaths::new(directory);
    #[cfg(target_os = "android")]
    let mut offer_legacy_upgrade = false;
    #[cfg(target_os = "android")]
    let mut legacy_upgrade_error = None;
    #[cfg(not(target_os = "android"))]
    let (offer_legacy_upgrade, legacy_upgrade_error) = (false, None::<String>);
    if let Some(core) = guard.as_ref() {
        if core.server.data_directory() != directory
            || core.token != token
            || core.server.cache_paths().root() != std::path::Path::new(&cache_directory)
        {
            return Err("Rust core already initialized with another session".into());
        }
    } else {
        // Apply a pending restore and acquire the directory lock before loading preferences.
        let _data_directory = plain_rs::infrastructure::data_paths::directory::DataDirectory::open(
            directory,
            std::path::Path::new(&cache_directory),
        )?;
        plain_rs::infrastructure::platform::system::log::init_file(&paths.rust_log_file(), "info")?;
        let prefs = Arc::new(
            plain_rs::features::prefs::Prefs::load(&paths.prefs()).map_err(|e| e.to_string())?,
        );
        #[cfg(target_os = "android")]
        if let Some(storage) = &config.legacy_storage {
            storage.validate().map_err(|error| error.to_string())?;
            if let Err(error) = storage.prepare_identity(&prefs) {
                legacy_upgrade_error = Some(format!("Legacy identity import failed: {error:#}"));
            }
        }
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .worker_threads(2)
            .enable_all()
            .build()
            .map_err(|e| e.to_string())?;
        let mut server = runtime.block_on(async {
            ContentServer::start_core_with_config(directory, token, prefs, config)
        })?;
        #[cfg(target_os = "android")]
        if legacy_upgrade_error.is_none() && let Some(storage) = &server.runtime().host.startup().legacy_storage {
            use plain_rs::features::legacy_upgrade::Status;
            match storage.status(&server.runtime().db, directory) {
                Ok(status) => offer_legacy_upgrade = status == Status::Pending,
                Err(error) => legacy_upgrade_error = Some(format!("Legacy upgrade status failed: {error:#}")),
            }
        }
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
    if let Some(error) = &legacy_upgrade_error {
        log::write_log(Level::Error, error);
    }
    let core = guard.as_ref().unwrap();
    Ok(plain_libs::serde_json::json!({
        "logFile":core.server.log_file()?,
        "sharedPreviewDir":core.server.cache_paths().shared_previews(),
        "offerLegacyUpgrade":offer_legacy_upgrade,"legacyUpgradeError":legacy_upgrade_error})
    .to_string())
}
