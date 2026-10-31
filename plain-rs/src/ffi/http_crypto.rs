use super::values::{c_string, input};
use std::ffi::c_char;

fn reply(result: Result<Vec<u8>, &str>) -> String {
    match result {
        Ok(value) => {
            serde_json::json!({"value":plain_rs::crypto::base64_encode(&value)}).to_string()
        }
        Err(error) => serde_json::json!({"error":error}).to_string(),
    }
}

fn encrypt(token: &str, payload: &str) -> String {
    reply(plain_rs::transport::http_request::encrypt(
        &plain_rs::crypto::base64_decode(token),
        payload,
    ))
}

fn reference(token: &str, payload: &str) -> String {
    let result = (|| -> Result<String, String> {
        let input: serde_json::Value = serde_json::from_str(payload).map_err(|e| e.to_string())?;
        let client_id = input["clientId"]
            .as_str()
            .ok_or("Missing resource client")?;
        let path = input["path"].as_str().ok_or("Missing resource path")?;
        plain_rs::files::resource_link::seal(
            &plain_rs::files::resource_link::Scope::Session(client_id.into()),
            &plain_rs::crypto::base64_decode(token),
            &plain_rs::files::resource_link::Resource::reference(path),
        )
        .map_err(|e| e.to_string())
    })();
    match result {
        Ok(value) => serde_json::json!({"value":value}).to_string(),
        Err(error) => serde_json::json!({"error":error}).to_string(),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_encrypt_resource_reference(
    token: *const c_char,
    payload: *const c_char,
) -> *mut c_char {
    c_string(match (input(token), input(payload)) {
        (Ok(token), Ok(payload)) => reference(&token, &payload),
        _ => reply(Err("Invalid resource encryption input")),
    })
}

fn decrypt(token: &str, endpoint: &str, request: &str, response: &str) -> String {
    reply(plain_rs::transport::http_request::decrypt_response(
        &plain_rs::crypto::base64_decode(token),
        endpoint,
        &plain_rs::crypto::base64_decode(request),
        &plain_rs::crypto::base64_decode(response),
    ))
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_encrypt_http_request(
    token: *const c_char,
    payload: *const c_char,
) -> *mut c_char {
    c_string(match (input(token), input(payload)) {
        (Ok(token), Ok(payload)) => encrypt(&token, &payload),
        _ => reply(Err("Invalid HTTP encryption input")),
    })
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_decrypt_http_response(
    token: *const c_char,
    endpoint: *const c_char,
    request: *const c_char,
    response: *const c_char,
) -> *mut c_char {
    c_string(
        match (
            input(token),
            input(endpoint),
            input(request),
            input(response),
        ) {
            (Ok(token), Ok(endpoint), Ok(request), Ok(response)) => {
                decrypt(&token, &endpoint, &request, &response)
            }
            _ => reply(Err("Invalid HTTP decryption input")),
        },
    )
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{
        EnvUnowned,
        errors::ThrowRuntimeExAndDefault,
        objects::{JObject, JString},
        sys::jstring,
    };

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_encryptResourceReference(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(reference(&token.to_string(), &payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_encryptHttpRequest(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(encrypt(&token.to_string(), &payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_decryptHttpResponse(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
        endpoint: JString<'_>,
        request: JString<'_>,
        response: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(decrypt(
                &token.to_string(),
                &endpoint.to_string(),
                &request.to_string(),
                &response.to_string(),
            ))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
