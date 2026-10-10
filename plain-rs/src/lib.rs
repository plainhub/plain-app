// `PublicSchema` merges ~25 roots into one `QueryRoot`, and async-graphql's
// field-set resolver builds a nested future per field. Layout computation for
// that type recurses deeper than rustc's default 128. The limit has to live
// here rather than in `plain-rs`: the generic is monomorphized in whichever
// crate instantiates the schema, which for the mobile build is this one.
#![recursion_limit = "512"]

mod bootstrap;
mod ffi;
use crate::ffi::values::c_string;
use crate::ffi::values::input;
pub use plain_rs::ffi::ble;
use plain_rs::prefs::Prefs;
use std::ffi::{CString, c_char};
use std::path::Path;
use std::sync::{Arc, Mutex};

static PREFS: Mutex<Option<Arc<Prefs>>> = Mutex::new(None);

fn prefs() -> Result<Arc<Prefs>, String> {
    PREFS
        .lock()
        .map_err(|e| e.to_string())?
        .clone()
        .ok_or_else(|| "Rust preferences are not initialized".to_string())
}

fn open(system_path: &str, user_path: &str) -> Result<(), String> {
    let mut guard = PREFS.lock().map_err(|e| e.to_string())?;
    if let Some(existing) = guard.as_ref() {
        if existing.path() == Path::new(system_path) && existing.user_path() == Path::new(user_path)
        {
            return Ok(());
        }
        return Err("Rust preferences were initialized with another path".to_string());
    }
    let loaded = Prefs::load_pair(Path::new(system_path), Path::new(user_path))
        .map_err(|e| e.to_string())?;
    *guard = Some(Arc::new(loaded));
    Ok(())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_open(
    system_path: *const c_char,
    user_path: *const c_char,
) -> *mut c_char {
    match input(system_path).and_then(|system_path| {
        input(user_path).and_then(|user_path| open(&system_path, &user_path))
    }) {
        Ok(()) => std::ptr::null_mut(),
        Err(error) => c_string(error),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_string_free(pointer: *mut c_char) {
    if !pointer.is_null() {
        // SAFETY: pointer was returned by c_string and is freed exactly once.
        drop(unsafe { CString::from_raw(pointer) });
    }
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::EnvUnowned;
    use jni::errors::ThrowRuntimeExAndDefault;
    use jni::objects::{JObject, JString};

    #[derive(Debug)]
    enum BridgeError {
        Jni(jni::errors::Error),
        Prefs(String),
    }

    impl std::fmt::Display for BridgeError {
        fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            match self {
                Self::Jni(error) => write!(f, "{error}"),
                Self::Prefs(error) => write!(f, "{error}"),
            }
        }
    }

    impl std::error::Error for BridgeError {}

    impl From<jni::errors::Error> for BridgeError {
        fn from(error: jni::errors::Error) -> Self {
            Self::Jni(error)
        }
    }

    type BridgeResult<T> = std::result::Result<T, BridgeError>;

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_openNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        system_path: JString<'_>,
        user_path: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            open(&system_path.to_string(), &user_path.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

}
