use super::values::{c_string, input};
use plain_server::host::events::HostEvents;
use std::{
    ffi::c_char,
    sync::{Arc, Mutex},
};
use tokio::runtime::Handle;

static BRIDGE: Mutex<Option<(Handle, Arc<HostEvents>)>> = Mutex::new(None);
pub(crate) fn install(
    runtime: &tokio::runtime::Runtime,
    server: &plain_server::assembly::server::ContentServer,
) {
    let _entered = runtime.enter();
    let host = Arc::new(server.host_events());
    let changes = Arc::new(tokio::sync::Notify::new());
    let receiver = changes.clone();
    let weak = Arc::downgrade(&host);
    runtime.spawn(async move {
        let Some(host) = weak.upgrade() else { return };
        loop {
            tokio::select! {
                biased;
                _ = host.wait_disconnected() => return,
                _ = receiver.notified() => host.prefs_changed().await,
            }
        }
    });
    server.preferences().expect("Core preferences initialized").set_change_hook(Some(Arc::new(move || changes.notify_one())));
    *BRIDGE.lock().unwrap() = Some((runtime.handle().clone(), host));
}
pub(crate) fn clear() {
    BRIDGE.lock().unwrap().take();
}
pub(crate) fn bridge() -> Result<(Handle, Arc<HostEvents>), String> {
    BRIDGE
        .lock()
        .map_err(|e| e.to_string())?
        .clone()
        .ok_or("Rust event bridge not initialized".into())
}
fn next(stream: &str) -> Result<String, String> {
    let (runtime, bridge) = bridge()?;
    runtime.block_on(async {
        let result = tokio::time::timeout(std::time::Duration::from_secs(1), async {
            match stream {
                "host" => Ok(bridge.next_request().await),
                "events" => Ok(bridge.next_event().await),
                _ => Err("Unknown Rust event stream".to_string()),
            }
        })
        .await;
        match result {
            Err(_) => Ok(String::new()),
            Ok(Ok(Some(value))) => Ok(value.to_string()),
            Ok(Ok(None)) => Err("Rust event stream closed".into()),
            Ok(Err(e)) => Err(e),
        }
    })
}
fn send(action: &str, message: &str, bytes: Vec<u8>) -> Result<(), String> {
    let (runtime, bridge) = bridge()?;
    match action {
        "reply" => bridge.reply(serde_json::from_str(message).map_err(|e| e.to_string())?),
        "text" => {
            let value: serde_json::Value =
                serde_json::from_str(message).map_err(|e| e.to_string())?;
            runtime.block_on(
                bridge.publish_text(
                    value["type"].as_str().ok_or("Missing event type")?,
                    value["payload"]
                        .as_str()
                        .ok_or("Missing event payload")?
                        .into(),
                ),
            )
        }
        "binary" => bridge.publish_binary(message, bytes),
        "thumbnail" => bridge.submit_thumbnail(message, bytes),
        _ => Err("Unknown Rust event action".into()),
    }
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_events_next(stream: *const c_char) -> *mut c_char {
    c_string(
        match input(stream).and_then(|s| next(&s)) {
            Ok(value) => serde_json::json!({"value":value}),
            Err(error) => serde_json::json!({"error":error}),
        }
        .to_string(),
    )
}
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_events_send(
    action: *const c_char,
    message: *const c_char,
    bytes: *const u8,
    len: usize,
) -> *mut c_char {
    let result = input(action).and_then(|action| {
        input(message).and_then(|message| {
            let bytes = if len == 0 {
                Vec::new()
            } else if bytes.is_null() {
                return Err("Null event bytes".into());
            } else {
                unsafe { std::slice::from_raw_parts(bytes, len) }.to_vec()
            };
            send(&action, &message, bytes)
        })
    });
    c_string(result.err().unwrap_or_default())
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
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeApi_nextNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        stream: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let value = match next(&stream.to_string()) {
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
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeApi_sendNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        action: JString<'_>,
        message: JString<'_>,
        bytes: JByteArray<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = send(
                &action.to_string(),
                &message.to_string(),
                env.convert_byte_array(&bytes)?,
            );
            env.new_string(result.err().unwrap_or_default())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
