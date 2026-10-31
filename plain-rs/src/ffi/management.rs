use super::values::{c_string, input};
use plain_server::native::management::NativeManagement;
use std::ffi::c_char;
use tokio::runtime::Handle;

pub(super) fn parts() -> Result<(Handle, NativeManagement), String> {
    super::server::with_core(|core| {
        core.server
            .native_management()
            .map(|management| (core.runtime.handle().clone(), management))
    })?
}

macro_rules! reply {
    ($result:expr) => {
        match $result {
            Ok(value) => serde_json::json!({"value":value}).to_string(),
            Err(error) => serde_json::json!({"error":error}).to_string(),
        }
    };
}

pub(super) use reply;

fn restart_http_server() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let state = runtime.block_on(management.restart_http_server(None, None))?;
        if !state.error.is_empty() {
            return Err(state.error);
        }
        Ok(true)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_restart_http_server() -> *mut c_char {
    c_string(restart_http_server())
}

fn tls_certificate_signature() -> String {
    reply!((|| -> Result<serde_json::Value, String> {
        let response = super::server::with_core(|core| {
            core.server.native_management()?;
            let path = plain_rs::data_paths::DataPaths::new(core.server.data_directory()).tls_identity();
            plain_rs::services::tls_identity::action(&path, r#"{"action":"get"}"#)
        })??;
        let value: serde_json::Value =
            serde_json::from_str(&response).map_err(|e| e.to_string())?;
        value
            .get("signature")
            .cloned()
            .ok_or_else(|| "TLS signature missing".to_owned())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_tls_certificate_signature() -> *mut c_char {
    c_string(tls_certificate_signature())
}

fn regenerate_tls_certificate() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.regenerate_tls_certificate())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_regenerate_tls_certificate() -> *mut c_char {
    c_string(regenerate_tls_certificate())
}

fn import_tls_certificate_pem(certificate_pem: String, private_key_pem: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.import_tls_certificate_pem(certificate_pem, private_key_pem))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_import_tls_certificate_pem(
    certificate_pem: *const c_char,
    private_key_pem: *const c_char,
) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let certificate_pem = input(certificate_pem)?;
        let private_key_pem = input(private_key_pem)?;
        Ok(import_tls_certificate_pem(certificate_pem, private_key_pem))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn import_tls_certificate_pkcs12(data: Vec<u8>, password: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.import_tls_certificate_pkcs12(data, password))
    })())
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_import_tls_certificate_pkcs12(
    data: *const u8,
    data_len: usize,
    password: *const c_char,
) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let data = if data_len == 0 {
            Vec::new()
        } else {
            if data.is_null() {
                return Err("Null certificate bytes".to_owned());
            }
            unsafe { std::slice::from_raw_parts(data, data_len) }.to_vec()
        };
        let password = input(password)?;
        Ok(import_tls_certificate_pkcs12(data, password))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn create_session(name: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.create_session(name)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_create_session(name: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let name = input(name)?;
        Ok(create_session(name))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn confirm_web_login(request_id: String, allowed: bool) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.confirm_web_login(request_id, allowed))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_confirm_web_login(request_id: *const c_char, allowed: bool) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let request_id = input(request_id)?;
        Ok(confirm_web_login(request_id, allowed))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn stage_app_restore(path: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.stage_app_restore(path))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_stage_app_restore(path: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let path = input(path)?;
        Ok(stage_app_restore(path))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn export_app_backup() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.export_app_backup())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_export_app_backup() -> *mut c_char {
    c_string(export_app_backup())
}

fn create_crash_report(report: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.create_crash_report(report))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_create_crash_report(report: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let report = input(report)?;
        Ok(create_crash_report(report))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn verify_adb_token(token: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let _ = runtime;
        management.verify_adb_token(token)
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_verify_adb_token(token: *const c_char) -> *mut c_char {
    let result = (|| -> Result<String, String> {
        let token = input(token)?;
        Ok(verify_adb_token(token))
    })();
    c_string(result.unwrap_or_else(|error| reply!(Err::<(), String>(error))))
}

fn start_app_update_download() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.start_app_update_download())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_start_app_update_download() -> *mut c_char {
    c_string(start_app_update_download())
}

fn cancel_app_update_download() -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.cancel_app_update_download())
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_cancel_app_update_download() -> *mut c_char {
    c_string(cancel_app_update_download())
}

fn check_app_update(automatic: bool) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.check_app_update(automatic))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_check_app_update(automatic: bool) -> *mut c_char {
    c_string(check_app_update(automatic))
}

fn load_preferences() -> String {
    reply!((|| {
        let (_, management) = parts()?;
        management.preferences()
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_load_preferences() -> *mut c_char {
    c_string(load_preferences())
}

fn set_pref(key: String, value: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        let value = serde_json::from_str(&value).map_err(|e| e.to_string())?;
        runtime.block_on(management.set_pref(&key, value))
    })())
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_set_pref(key: *const c_char, value: *const c_char) -> *mut c_char {
    c_string(
        match input(key).and_then(|key| input(value).map(|value| set_pref(key, value))) {
            Ok(result) => result,
            Err(error) => reply!(Err::<(), String>(error)),
        },
    )
}

fn complete_onboarding() -> String {
    reply!((|| {
        let (_, management) = parts()?;
        management.complete_onboarding()
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_complete_onboarding() -> *mut c_char {
    c_string(complete_onboarding())
}

fn toggle_notification_app(package_name: String) -> String {
    reply!((|| {
        let (runtime, management) = parts()?;
        runtime.block_on(management.toggle_notification_app(package_name))
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_toggle_notification_app(package_name: *const c_char) -> *mut c_char {
    c_string(match input(package_name) {
        Ok(package_name) => toggle_notification_app(package_name),
        Err(error) => reply!(Err::<(), String>(error)),
    })
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{
        EnvUnowned,
        errors::ThrowRuntimeExAndDefault,
        objects::{JByteArray, JObject, JString},
        sys::{jboolean, jstring},
    };

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_restartHttpServer(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = restart_http_server();
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_tlsCertificateSignature(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(tls_certificate_signature())
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_regenerateTlsCertificate(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = regenerate_tls_certificate();
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_importTlsCertificatePem(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        certificate_pem: JString<'_>,
        private_key_pem: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = import_tls_certificate_pem(
                certificate_pem.to_string(),
                private_key_pem.to_string(),
            );
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_importTlsCertificatePkcs12(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        data: JByteArray<'_>,
        password: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result =
                import_tls_certificate_pkcs12(env.convert_byte_array(&data)?, password.to_string());
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_createSession(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        name: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = create_session(name.to_string());
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_confirmWebLogin(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        request_id: JString<'_>,
        allowed: jboolean,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = confirm_web_login(request_id.to_string(), allowed);
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_stageAppRestore(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        path: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = stage_app_restore(path.to_string());
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_exportAppBackup(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = export_app_backup();
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_createCrashReport(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        report: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = create_crash_report(report.to_string());
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_verifyAdbToken(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = verify_adb_token(token.to_string());
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_startAppUpdateDownload(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = start_app_update_download();
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_cancelAppUpdateDownload(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = cancel_app_update_download();
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_checkAppUpdate(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        automatic: jboolean,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let result = check_app_update(automatic);
            env.new_string(result)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_loadPreferencesResponse(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(load_preferences()) })
            .resolve::<ThrowRuntimeExAndDefault>()
            .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_setPref(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        key: JString<'_>,
        value: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(set_pref(key.to_string(), value.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_completeOnboarding(
        mut env: EnvUnowned<'_>, _: JObject<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> { env.new_string(complete_onboarding()) })
            .resolve::<ThrowRuntimeExAndDefault>().into_raw()
    }
    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_toggleNotificationApp(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        package_name: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(toggle_notification_app(package_name.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
