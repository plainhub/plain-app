use super::management::reply;
use super::server::with_core;
use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
use plain_rs::features::legacy_upgrade::Status;
use plain_rs::infrastructure::platform::system::log::{self, Level};

fn execute(action: &str) -> Result<Status, String> {
    let (runtime, directory) = with_core(|core| {
        (core.server.runtime().clone(), core.server.data_directory().to_owned())
    })?;
    let Some(storage) = &runtime.host.startup().legacy_storage else {
        return Ok(Status::Unavailable);
    };
    match action {
        "STATUS" => storage.status(&runtime.db, &directory),
        "IMPORT" => storage.import(&runtime.db, &runtime.prefs, &directory),
        "CLEAR" => storage.clear(&runtime.db, &directory),
        _ => return Err("Invalid legacy data upgrade action".into()),
    }
    .map_err(|error| {
        log::write_log(Level::Error, &format!("Legacy data upgrade failed: {error:#}"));
        error.to_string()
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_legacyDataUpgrade(
    mut env: EnvUnowned<'_>,
    _: JObject<'_>,
    action: JString<'_>,
) -> jstring {
    env.with_env(|env| -> jni::errors::Result<_> {
        env.new_string(reply!(execute(&action.to_string())))
    })
    .resolve::<ThrowRuntimeExAndDefault>()
    .into_raw()
}
