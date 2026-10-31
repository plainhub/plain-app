use super::values::{c_string, input};
use plain_server::host::events::HostEvents;
use plain_rs::platform::system::log;
use plain_rs::platform::system::log::Level;
use std::{
    ffi::c_char,
    sync::{Arc, Mutex},
};
use tokio::runtime::Handle;

static BRIDGE: Mutex<Option<(Handle, Arc<HostEvents>)>> = Mutex::new(None);
pub(crate) fn install(
    runtime: &tokio::runtime::Runtime,
    server: &plain_server::server::ContentServer,
) -> Result<(), String> {
    let _entered = runtime.enter();
    let host = Arc::new(server.host_events()?);
    let changes = Arc::new(tokio::sync::Notify::new());
    let receiver = changes.clone();
    let weak = Arc::downgrade(&host);
    runtime.spawn(async move {
        let Some(host) = weak.upgrade() else { return };
        loop {
            tokio::select! {
                biased;
                _ = host.wait_disconnected() => return,
                _ = receiver.notified() => {
                    if let Err(error) = host.prefs_changed().await {
                        log::write_log(Level::Error, &format!("Host preferences update failed: {error}"));
                    }
                },
            }
        }
    });
    server.preferences()?.set_change_hook(Some(Arc::new({
        let changes = changes.clone();
        move || changes.notify_one()
    })));
    changes.notify_one();
    *BRIDGE.lock().map_err(|error| error.to_string())? = Some((runtime.handle().clone(), host));
    Ok(())
}
pub(crate) fn clear() {
    BRIDGE.lock().unwrap().take();
}
pub(super) fn bridge() -> Result<(Handle, Arc<HostEvents>), String> {
    BRIDGE
        .lock()
        .map_err(|e| e.to_string())?
        .clone()
        .ok_or("Rust host bridge not initialized".into())
}
fn next() -> Result<String, String> {
    let (runtime, host) = bridge()?;
    runtime.block_on(async {
        match tokio::time::timeout(std::time::Duration::from_secs(1), host.next_request()).await {
            Err(_) => Ok(String::new()),
            Ok(Some(value)) => Ok(value.to_string()),
            Ok(None) => Err("Rust host stream closed".into()),
        }
    })
}
fn reply(message: &str) -> Result<(), String> {
    let (_, host) = bridge()?;
    host.reply(serde_json::from_str(message).map_err(|error| error.to_string())?)
}
fn thumbnail(token: &str, bytes: Vec<u8>) -> Result<(), String> {
    let (_, host) = bridge()?;
    host.submit_thumbnail(token, bytes)
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_host_next() -> *mut c_char {
    c_string(
        match next() {
            Ok(value) => serde_json::json!({"value":value}),
            Err(error) => serde_json::json!({"error":error}),
        }
        .to_string(),
    )
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_host_reply(message: *const c_char) -> *mut c_char {
    c_string(
        input(message)
            .and_then(|message| reply(&message))
            .err()
            .unwrap_or_default(),
    )
}
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_submit_thumbnail(
    token: *const c_char,
    bytes: *const u8,
    len: usize,
) -> *mut c_char {
    c_string(
        input(token)
            .and_then(|token| {
                super::values::input_bytes(bytes, len).and_then(|bytes| thumbnail(&token, bytes))
            })
            .err()
            .unwrap_or_default(),
    )
}
#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{
        EnvUnowned,
        errors::ThrowRuntimeExAndDefault,
        objects::{JByteArray, JObject, JString},
        sys::jstring,
    };
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_hostNext(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let value = match next() {
                Ok(value) => value,
                Err(error) => {
                    env.throw_new(
                        jni::jni_str!("java/lang/IllegalStateException"),
                        jni::strings::JNIString::from(error),
                    )?;
                    String::new()
                }
            };
            env.new_string(value)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_hostReply(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        message: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(reply(&message.to_string()).err().unwrap_or_default())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_submitThumbnail(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
        bytes: JByteArray<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let bytes = env.convert_byte_array(&bytes)?;
            env.new_string(
                thumbnail(&token.to_string(), bytes)
                    .err()
                    .unwrap_or_default(),
            )
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
