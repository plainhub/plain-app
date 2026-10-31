use super::values::{c_string, input};
use std::ffi::c_char;

#[unsafe(no_mangle)]
pub extern "C" fn plain_file_icon(extension: *const c_char) -> *mut c_char {
    c_string(
        plain_server::native::assets::file_icon(&input(extension).unwrap_or_default()).to_owned(),
    )
}

#[cfg(target_os = "android")]
mod android {
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_fileIcon(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        extension: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(plain_server::native::assets::file_icon(
                &extension.to_string(),
            ))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
