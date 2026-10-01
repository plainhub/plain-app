use plain_rs::prefs::{Prefs, set_global, try_get_default};
use serde_json::{Map, Value};
use std::ffi::{CStr, CString, c_char};
use std::path::Path;
use std::sync::Mutex;

static INIT: Mutex<()> = Mutex::new(());

fn prefs() -> Result<std::sync::Arc<Prefs>, String> {
    try_get_default().ok_or_else(|| "Rust preferences are not initialized".to_string())
}

fn open(path: &str) -> Result<(), String> {
    let _guard = INIT.lock().map_err(|e| e.to_string())?;
    if let Some(existing) = try_get_default() {
        if existing.path() == Path::new(path) {
            return Ok(());
        }
        return Err("Rust preferences were initialized with another path".to_string());
    }
    let loaded = std::sync::Arc::new(Prefs::load(Path::new(path)).map_err(|e| e.to_string())?);
    set_global(loaded);
    Ok(())
}

fn snapshot() -> Result<String, String> {
    let entries: Map<String, Value> = prefs()?.entries().into_iter().collect();
    serde_json::to_string(&entries).map_err(|e| e.to_string())
}

fn set(key: &str, value_json: &str) -> Result<(), String> {
    let value: Value = serde_json::from_str(value_json).map_err(|e| e.to_string())?;
    prefs()?.set(key, value).map_err(|e| e.to_string())?;
    Ok(())
}

fn remove(key: &str) -> Result<(), String> {
    prefs()?.remove(key).map_err(|e| e.to_string())?;
    Ok(())
}

fn c_string(value: String) -> *mut c_char {
    CString::new(value).expect("string contains NUL").into_raw()
}

fn input(pointer: *const c_char) -> Result<String, String> {
    if pointer.is_null() {
        return Err("null string passed to Rust preferences".to_string());
    }
    // SAFETY: callers pass a null-terminated string valid for this call.
    Ok(unsafe { CStr::from_ptr(pointer) }
        .to_str()
        .map_err(|e| e.to_string())?
        .to_owned())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_open(path: *const c_char) -> *mut c_char {
    match input(path).and_then(|path| open(&path)) {
        Ok(()) => std::ptr::null_mut(),
        Err(error) => c_string(error),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_snapshot() -> *mut c_char {
    match snapshot() {
        Ok(json) => c_string(json),
        Err(error) => c_string(format!("ERROR:{error}")),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_set(key: *const c_char, value_json: *const c_char) -> *mut c_char {
    match input(key).and_then(|key| input(value_json).and_then(|value| set(&key, &value))) {
        Ok(()) => std::ptr::null_mut(),
        Err(error) => c_string(error),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_remove(key: *const c_char) -> *mut c_char {
    match input(key).and_then(|key| remove(&key)) {
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
    use jni::sys::jstring;

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
        path: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            open(&path.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_snapshotNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> BridgeResult<_> {
            let json = snapshot().map_err(BridgeError::Prefs)?;
            Ok(env.new_string(json)?)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_setNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
        value: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            set(&key.to_string(), &value.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_removeNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            remove(&key.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rust_prefs_bridge_persists_typed_values() {
        let path =
            std::env::temp_dir().join(format!("plain-prefs-mobile-{}.json", std::process::id()));
        let path_c = CString::new(path.to_str().unwrap()).unwrap();
        assert!(plain_prefs_open(path_c.as_ptr()).is_null());
        for (key, json) in [
            ("enabled", "true"),
            ("count", "7"),
            ("speed", "1.25"),
            ("name", "\"plain\""),
            ("set", "[\"a\",\"b\"]"),
        ] {
            let key = CString::new(key).unwrap();
            let json = CString::new(json).unwrap();
            assert!(plain_prefs_set(key.as_ptr(), json.as_ptr()).is_null());
        }
        let text = std::fs::read_to_string(&path).unwrap();
        let stored: Value = serde_json::from_str(&text).unwrap();
        assert_eq!(stored["enabled"], true);
        assert_eq!(stored["count"], 7);
        assert_eq!(stored["speed"], 1.25);
        assert_eq!(stored["name"], "plain");
        assert_eq!(stored["set"], serde_json::json!(["a", "b"]));
        let key = CString::new("count").unwrap();
        assert!(plain_prefs_remove(key.as_ptr()).is_null());
        assert!(
            Prefs::load(&path)
                .unwrap()
                .get::<i32>("count")
                .unwrap()
                .is_none()
        );
        std::fs::remove_file(path).unwrap();
    }
}
