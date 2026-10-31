use super::management::{parts, reply};
use super::values::{c_string, input};
use std::ffi::c_char;

fn file_paths(paths: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        let paths: Vec<String> = serde_json::from_str(&paths).map_err(|error| error.to_string())?;
        management.file_paths(paths)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_file_paths(paths: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let paths = input(paths)?;
        Ok(file_paths(paths))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn import_note_image(source: String, file_name: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.import_note_image(source, file_name))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_import_note_image(
    source: *const c_char,
    file_name: *const c_char,
) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let source = input(source)?;
        let file_name = input(file_name)?;
        Ok(import_note_image(source, file_name))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn import_pomodoro_sound(source: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.import_pomodoro_sound(source))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_import_pomodoro_sound(source: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let source = input(source)?;
        Ok(import_pomodoro_sound(source))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JObject, JString}, sys::jstring};
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_filePaths(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        paths: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(file_paths(paths.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_importNoteImage(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        source: JString<'_>,
        file_name: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(import_note_image(source.to_string(), file_name.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_importPomodoroSound(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        source: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(import_pomodoro_sound(source.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
