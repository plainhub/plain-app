use plain_rs::prefs::Prefs;
use serde_json::{Map, Value};
use std::ffi::{CStr, CString, c_char};
use std::path::Path;
use std::sync::{Arc, Mutex};

struct PrefsPair {
    system: Prefs,
    user: Prefs,
}

static PREFS: Mutex<Option<Arc<PrefsPair>>> = Mutex::new(None);

fn prefs() -> Result<Arc<PrefsPair>, String> {
    PREFS
        .lock()
        .map_err(|e| e.to_string())?
        .clone()
        .ok_or_else(|| "Rust preferences are not initialized".to_string())
}

fn open(system_path: &str, user_path: &str) -> Result<(), String> {
    let mut guard = PREFS.lock().map_err(|e| e.to_string())?;
    if let Some(existing) = guard.as_ref() {
        if existing.system.path() == Path::new(system_path)
            && existing.user.path() == Path::new(user_path)
        {
            return Ok(());
        }
        return Err("Rust preferences were initialized with another path".to_string());
    }
    let loaded = PrefsPair {
        system: Prefs::load(Path::new(system_path)).map_err(|e| e.to_string())?,
        user: Prefs::load(Path::new(user_path)).map_err(|e| e.to_string())?,
    };
    *guard = Some(Arc::new(loaded));
    Ok(())
}

fn snapshot(is_user_pref: bool) -> Result<String, String> {
    let prefs = prefs()?;
    let entries: Map<String, Value> = if is_user_pref {
        prefs.user.entries()
    } else {
        prefs.system.entries()
    }
    .into_iter()
    .collect();
    serde_json::to_string(&entries).map_err(|e| e.to_string())
}

fn set(is_user_pref: bool, key: &str, value_json: &str) -> Result<(), String> {
    let value: Value = serde_json::from_str(value_json).map_err(|e| e.to_string())?;
    let prefs = prefs()?;
    if is_user_pref {
        prefs.user.set(key, value).map_err(|e| e.to_string())?;
    } else {
        prefs.system.set(key, value).map_err(|e| e.to_string())?;
    }
    Ok(())
}

fn remove(is_user_pref: bool, key: &str) -> Result<(), String> {
    let prefs = prefs()?;
    if is_user_pref {
        prefs.user.remove(key).map_err(|e| e.to_string())?;
    } else {
        prefs.system.remove(key).map_err(|e| e.to_string())?;
    }
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
pub extern "C" fn plain_prefs_system_snapshot() -> *mut c_char {
    match snapshot(false) {
        Ok(json) => c_string(json),
        Err(error) => c_string(format!("ERROR:{error}")),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_user_snapshot() -> *mut c_char {
    match snapshot(true) {
        Ok(json) => c_string(json),
        Err(error) => c_string(format!("ERROR:{error}")),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_set_system(
    key: *const c_char,
    value_json: *const c_char,
) -> *mut c_char {
    match input(key).and_then(|key| input(value_json).and_then(|value| set(false, &key, &value))) {
        Ok(()) => std::ptr::null_mut(),
        Err(error) => c_string(error),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_set_user(
    key: *const c_char,
    value_json: *const c_char,
) -> *mut c_char {
    match input(key).and_then(|key| input(value_json).and_then(|value| set(true, &key, &value))) {
        Ok(()) => std::ptr::null_mut(),
        Err(error) => c_string(error),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_remove_system(key: *const c_char) -> *mut c_char {
    match input(key).and_then(|key| remove(false, &key)) {
        Ok(()) => std::ptr::null_mut(),
        Err(error) => c_string(error),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_prefs_remove_user(key: *const c_char) -> *mut c_char {
    match input(key).and_then(|key| remove(true, &key)) {
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
        system_path: JString<'_>,
        user_path: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            open(&system_path.to_string(), &user_path.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_systemSnapshotNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> BridgeResult<_> {
            let json = snapshot(false).map_err(BridgeError::Prefs)?;
            Ok(env.new_string(json)?)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_userSnapshotNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> BridgeResult<_> {
            let json = snapshot(true).map_err(BridgeError::Prefs)?;
            Ok(env.new_string(json)?)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_setSystemNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
        value: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            set(false, &key.to_string(), &value.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_setUserNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
        value: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            set(true, &key.to_string(), &value.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_removeSystemNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            remove(false, &key.to_string()).map_err(BridgeError::Prefs)?;
            Ok(())
        })
        .resolve::<ThrowRuntimeExAndDefault>();
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_preferences_RustPrefsBridge_removeUserNative(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
    ) {
        env.with_env(|_| -> BridgeResult<()> {
            remove(true, &key.to_string()).map_err(BridgeError::Prefs)?;
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
        let dir = std::env::temp_dir().join(format!("plain-rust-{}", std::process::id()));
        let system_path = dir.join("system_prefs.json");
        let user_path = dir.join("user_prefs.json");
        let system_path_c = CString::new(system_path.to_str().unwrap()).unwrap();
        let user_path_c = CString::new(user_path.to_str().unwrap()).unwrap();
        assert!(plain_prefs_open(system_path_c.as_ptr(), user_path_c.as_ptr()).is_null());
        for (key, json) in [
            ("enabled", "true"),
            ("count", "7"),
            ("speed", "1.25"),
            ("name", "\"plain\""),
            ("set", "[\"a\",\"b\"]"),
        ] {
            let key = CString::new(key).unwrap();
            let json = CString::new(json).unwrap();
            assert!(plain_prefs_set_system(key.as_ptr(), json.as_ptr()).is_null());
        }
        let key = CString::new("theme").unwrap();
        let json = CString::new(r#"{"dark":true}"#).unwrap();
        assert!(plain_prefs_set_user(key.as_ptr(), json.as_ptr()).is_null());
        let text = std::fs::read_to_string(&system_path).unwrap();
        let stored: Value = serde_json::from_str(&text).unwrap();
        assert_eq!(stored["enabled"], true);
        assert_eq!(stored["count"], 7);
        assert_eq!(stored["speed"], 1.25);
        assert_eq!(stored["name"], "plain");
        assert_eq!(stored["set"], serde_json::json!(["a", "b"]));
        let user_text = std::fs::read_to_string(&user_path).unwrap();
        let user_stored: Value = serde_json::from_str(&user_text).unwrap();
        assert_eq!(user_stored["theme"], serde_json::json!({"dark": true}));
        let key = CString::new("count").unwrap();
        assert!(plain_prefs_remove_system(key.as_ptr()).is_null());
        assert!(
            Prefs::load(&system_path)
                .unwrap()
                .get::<i32>("count")
                .unwrap()
                .is_none()
        );
        std::fs::remove_dir_all(dir).unwrap();
    }
}
