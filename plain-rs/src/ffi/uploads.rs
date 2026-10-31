use super::values::{c_string, input};
use std::ffi::c_char;

fn write(id: &str, index: i64, bytes: Vec<u8>) -> Result<(), String> {
    if bytes.len() > plain_server::native::uploads::MAX_NATIVE_UPLOAD_CHUNK_BYTES {
        return Err("Invalid upload chunk length".into());
    }
    let (runtime, uploads) = super::server::with_core(|core| {
        core.server
            .native_uploads()
            .map(|uploads| (core.runtime.handle().clone(), uploads))
    })??;
    runtime.block_on(uploads.write_chunk(id, index, &bytes))
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_upload_chunk(
    id: *const c_char,
    index: i64,
    bytes: *const u8,
    len: usize,
) -> *mut c_char {
    let result = input(id).and_then(|id| {
        if len > plain_server::native::uploads::MAX_NATIVE_UPLOAD_CHUNK_BYTES {
            return Err("Invalid upload chunk length".into());
        }
        let bytes = if len == 0 {
            Vec::new()
        } else if bytes.is_null() {
            return Err("Null upload bytes".into());
        } else {
            unsafe { std::slice::from_raw_parts(bytes, len) }.to_vec()
        };
        write(&id, index, bytes)
    });
    c_string(result.err().unwrap_or_default())
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JByteArray, JObject, JString}, sys::{jlong, jstring}};

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_uploadChunk(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        id: JString<'_>,
        index: jlong,
        bytes: JByteArray<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = if bytes.len(env)?
                > plain_server::native::uploads::MAX_NATIVE_UPLOAD_CHUNK_BYTES
            {
                Err("Invalid upload chunk length".into())
            } else {
                write(&id.to_string(), index, env.convert_byte_array(&bytes)?)
            };
            env.new_string(result.err().unwrap_or_default())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
