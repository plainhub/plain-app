use super::management::parts;
use super::values::{c_string, input};
use std::ffi::c_char;

fn ble_incoming(
    remote_host: String,
    characteristic_uuid: String,
    bytes: Vec<u8>,
) -> Result<Vec<u8>, String> {
    let (runtime, management) = parts()?;
    runtime.block_on(management.ble_incoming(remote_host, characteristic_uuid, bytes))
}

#[repr(C)]
pub struct BleIncomingBuffer {
    data: *mut u8,
    len: usize,
    status: i32,
}

/// # Safety
/// Strings must be valid NUL-terminated UTF-8. Non-null `bytes` must reference
/// `len` readable bytes for the duration of this call.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_ble_incoming(
    remote_host: *const c_char,
    characteristic_uuid: *const c_char,
    bytes: *const u8,
    len: usize,
) -> BleIncomingBuffer {
    let result = (|| {
        if len > plain_rs::libs::protocols::ble::MAX_MESSAGE {
            return Err("BLE message exceeds limit".into());
        }
        let bytes = if len == 0 {
            Vec::new()
        } else if bytes.is_null() {
            return Err("Null BLE bytes".into());
        } else {
            unsafe { std::slice::from_raw_parts(bytes, len) }.to_vec()
        };
        ble_incoming(input(remote_host)?, input(characteristic_uuid)?, bytes)
    })();
    let (bytes, status) = match result {
        Ok(bytes) => (bytes, 1),
        Err(error) => (error.into_bytes(), -1),
    };
    let mut bytes = bytes.into_boxed_slice();
    let buffer = BleIncomingBuffer {
        data: bytes.as_mut_ptr(),
        len: bytes.len(),
        status,
    };
    std::mem::forget(bytes);
    buffer
}

/// # Safety
/// `buffer` must be an unmodified, unfreed result of `plain_ble_incoming`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_ble_incoming_buffer_free(buffer: BleIncomingBuffer) {
    if !buffer.data.is_null() {
        drop(unsafe { Box::from_raw(std::ptr::slice_from_raw_parts_mut(buffer.data, buffer.len)) });
    }
}

fn check_http() -> String {
    let result = (|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.check_http())
    })();
    match result {
        Ok(value) => serde_json::json!({"value": value}).to_string(),
        Err(error) => serde_json::json!({"error": error}).to_string(),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_check_http() -> *mut c_char {
    c_string(check_http())
}

fn http_status() -> Result<String, String> {
    let status = super::server::with_core(|core| core.server.http_status())?;
    serde_json::to_string(&status).map_err(|error| error.to_string())
}

/// # Safety
/// Non-null `error` must point to a writable string pointer. The returned string
/// and error string must be released with `plain_prefs_string_free`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_http_status(error: *mut *mut c_char) -> *mut c_char {
    if !error.is_null() {
        unsafe { *error = std::ptr::null_mut() };
    }
    match http_status() {
        Ok(value) => c_string(value),
        Err(message) => {
            if !error.is_null() {
                unsafe { *error = c_string(message) };
            }
            std::ptr::null_mut()
        }
    }
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JByteArray, JObject, JString}, sys::{jbyteArray, jstring}};

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_bleIncoming(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        remote_host: JString<'_>,
        characteristic_uuid: JString<'_>,
        bytes: JByteArray<'_>,
    ) -> jbyteArray {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = if bytes.len(env)? > plain_rs::libs::protocols::ble::MAX_MESSAGE {
                Err("BLE message exceeds limit".into())
            } else {
                ble_incoming(
                    remote_host.to_string(),
                    characteristic_uuid.to_string(),
                    env.convert_byte_array(&bytes)?,
                )
            };
            match result {
                Ok(bytes) => env.byte_array_from_slice(&bytes),
                Err(error) => {
                    env.throw_new(
                        jni::jni_str!("java/lang/IllegalStateException"),
                        jni::strings::JNIString::from(error),
                    )?;
                    Ok(JByteArray::null())
                }
            }
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_checkHttp(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(check_http()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_httpStatus(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            match http_status() {
                Ok(value) => env.new_string(value),
                Err(error) => {
                    env.throw_new(
                        jni::jni_str!("java/lang/IllegalStateException"),
                        jni::strings::JNIString::from(error),
                    )?;
                    Ok(JString::null())
                }
            }
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
