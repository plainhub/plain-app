use super::values::{c_string, input};
use plain_rs::libs::host::resource_stream::NativeResourceStream;
use std::{ffi::c_char, sync::Mutex, time::Duration};
use tokio::runtime::Handle;

const STREAM_TIMEOUT: Duration = Duration::from_secs(30);

#[cfg(test)]
#[path = "../../tests/unit/ffi/resources.rs"]
mod tests;

enum Stream {
    Open(NativeResourceStream),
    Failed(String),
}

static STREAMS: Mutex<Vec<(String, Stream)>> = Mutex::new(Vec::new());

pub(crate) fn clear() {
    if let Ok(mut streams) = STREAMS.lock() {
        streams.clear();
    }
}
fn runtime() -> Result<Handle, String> {
    super::server::with_core(|core| core.runtime.handle().clone())
}
fn put(id: &str, stream: Stream) {
    if let Ok(mut streams) = STREAMS.lock() {
        if let Some(index) = streams.iter().position(|(key, _)| key == id) {
            streams.swap_remove(index);
        }
        streams.push((id.to_string(), stream));
    }
}
fn take(id: &str) -> Result<Stream, String> {
    let mut streams = STREAMS.lock().map_err(|e| e.to_string())?;
    let index = streams
        .iter()
        .position(|(key, _)| key == id)
        .ok_or("Resource stream not attached")?;
    Ok(streams.swap_remove(index).1)
}
fn take_open(id: &str) -> Result<(Handle, NativeResourceStream), String> {
    let runtime = runtime()?;
    match take(id)? {
        Stream::Open(stream) => Ok((runtime, stream)),
        Stream::Failed(error) => {
            put(id, Stream::Failed(error.clone()));
            Err(error)
        }
    }
}
fn open(id: &str) -> Result<String, String> {
    if id.is_empty() {
        return Err("Missing resource stream id".to_string());
    }
    let (runtime, events) = super::host::bridge()?;
    let stream = events.attach_resource(id)?;
    let request = runtime
        .block_on(async { tokio::time::timeout(STREAM_TIMEOUT, stream.next_request()).await });
    let metadata = match request {
        Err(_) => return Err("Resource stream request timed out".to_string()),
        Ok(Err(error)) => return Err(error),
        Ok(Ok(None)) => return Err("Resource stream closed".to_string()),
        Ok(Ok(Some(metadata))) => metadata,
    };
    put(id, Stream::Open(stream));
    Ok(metadata)
}
fn write(id: &str, bytes: Vec<u8>) -> Result<(), String> {
    let (runtime, stream) = take_open(id)?;
    let outcome =
        runtime.block_on(async { tokio::time::timeout(STREAM_TIMEOUT, stream.write(bytes)).await });
    let result = match outcome {
        Err(_) => Err("Resource stream write timed out".to_string()),
        Ok(result) => result,
    };
    put(
        id,
        match &result {
            Ok(()) => Stream::Open(stream),
            Err(error) => Stream::Failed(error.clone()),
        },
    );
    result
}
fn close(id: &str, failed: bool) -> Result<(), String> {
    let stream = match take(id)? {
        Stream::Failed(_) => return Ok(()),
        Stream::Open(stream) => stream,
    };
    let runtime = runtime()?;
    let outcome = runtime.block_on(async {
        tokio::time::timeout(STREAM_TIMEOUT, async {
            if failed {
                stream.fail().await
            } else {
                stream.finish().await
            }
        })
        .await
    });
    let result = match outcome {
        Err(_) => Err("Resource stream close timed out".to_string()),
        Ok(result) => result,
    };
    if let Err(error) = &result {
        put(id, Stream::Failed(error.clone()));
    }
    result
}
fn envelope(result: Result<String, String>) -> String {
    match result {
        Ok(value) => serde_json::json!({"value":value}).to_string(),
        Err(error) => serde_json::json!({"error":error}).to_string(),
    }
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_resource_open(id: *const c_char) -> *mut c_char {
    c_string(envelope(input(id).and_then(|id| open(&id))))
}
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_resource_write(
    id: *const c_char,
    bytes: *const u8,
    len: usize,
) -> *mut c_char {
    let result = input(id).and_then(|id| {
        let bytes = if len == 0 {
            Vec::new()
        } else if bytes.is_null() {
            return Err("Null resource bytes".to_string());
        } else {
            unsafe { std::slice::from_raw_parts(bytes, len) }.to_vec()
        };
        write(&id, bytes)
    });
    c_string(result.err().unwrap_or_default())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_resource_close(id: *const c_char, failed: bool) -> *mut c_char {
    c_string(
        input(id)
            .and_then(|id| close(&id, failed))
            .err()
            .unwrap_or_default(),
    )
}
#[cfg(unix)]
fn provide_file(id: &str, fd: i32, offset: i64, length: i64) -> Result<(), String> {
    use std::os::fd::BorrowedFd;
    if fd < 0 {
        return Err("Invalid resource descriptor".into());
    }
    let file = unsafe { BorrowedFd::borrow_raw(fd) }
        .try_clone_to_owned()
        .map_err(|error| error.to_string())?;
    let file = plain_rs::libs::host::resource_stream::NativeResourceFile::new(
        std::fs::File::from(file),
        offset,
        length,
    )?;
    let (runtime, stream) = take_open(id)?;
    runtime.block_on(async {
        let result = tokio::time::timeout(STREAM_TIMEOUT, stream.file(file))
            .await
            .map_err(|_| "Resource file timed out".to_string())
            .and_then(|result| result);
        if result.is_err() {
            let _ = stream.fail().await;
        }
        result
    })
}
#[cfg(unix)]
#[unsafe(no_mangle)]
pub extern "C" fn plain_resource_file(
    id: *const c_char,
    fd: i32,
    offset: i64,
    length: i64,
) -> *mut c_char {
    c_string(
        input(id)
            .and_then(|id| provide_file(&id, fd, offset, length))
            .err()
            .unwrap_or_default(),
    )
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JByteArray, JObject, JString}, sys::{jboolean, jstring}};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resourceFile(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
        fd: jni::sys::jint,
        offset: jni::sys::jlong,
        length: jni::sys::jlong,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = provide_file(&id.to_string(), fd, offset, length);
            env.new_string(result.err().unwrap_or_default())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resourceOpen(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(envelope(open(&id.to_string())))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resourceWrite(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
        bytes: JByteArray<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let bytes = env.convert_byte_array(&bytes)?;
            let result = write(&id.to_string(), bytes);
            env.new_string(result.err().unwrap_or_default())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_resourceClose(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
        failed: jboolean,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = close(&id.to_string(), failed);
            env.new_string(result.err().unwrap_or_default())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
