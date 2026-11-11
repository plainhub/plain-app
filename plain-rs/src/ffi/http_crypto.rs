use super::values::{c_string, input};
use std::ffi::c_char;

fn reply(result: Result<Vec<u8>, &str>) -> String {
    match result {
        Ok(value) => serde_json::json!({"value":plain_libs::utils::base64::base64_encode(&value)})
            .to_string(),
        Err(error) => serde_json::json!({"error":error}).to_string(),
    }
}

fn encrypt(token: &str, endpoint: &str, payload: &str) -> String {
    reply(plain_rs::infrastructure::transport::http_request::encrypt(
        &plain_libs::utils::base64::base64_decode(token),
        endpoint,
        payload,
    ))
}

fn reference(token: &str, payload: &str) -> String {
    let result = plain_rs::features::files::resource_link::seal_reference(
        &plain_libs::utils::base64::base64_decode(token),
        payload,
    );
    match result {
        Ok(value) => serde_json::json!({"value":value}).to_string(),
        Err(error) => serde_json::json!({"error":error.to_string()}).to_string(),
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

fn decrypt(token: &str, request: &str, response: &str) -> String {
    reply(
        plain_rs::infrastructure::transport::http_request::decrypt_response(
            &plain_libs::utils::base64::base64_decode(token),
            &plain_libs::utils::base64::base64_decode(request),
            &plain_libs::utils::base64::base64_decode(response),
        ),
    )
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_encrypt_http_request(
    token: *const c_char,
    endpoint: *const c_char,
    payload: *const c_char,
) -> *mut c_char {
    c_string(match (input(token), input(endpoint), input(payload)) {
        (Ok(token), Ok(endpoint), Ok(payload)) => encrypt(&token, &endpoint, &payload),
        _ => reply(Err("Invalid HTTP encryption input")),
    })
}

#[unsafe(no_mangle)]
pub extern "C" fn plain_decrypt_http_response(
    token: *const c_char,
    request: *const c_char,
    response: *const c_char,
) -> *mut c_char {
    c_string(match (input(token), input(request), input(response)) {
        (Ok(token), Ok(request), Ok(response)) => decrypt(&token, &request, &response),
        _ => reply(Err("Invalid HTTP decryption input")),
    })
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
        endpoint: JString<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(encrypt(
                &token.to_string(),
                &endpoint.to_string(),
                &payload.to_string(),
            ))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_decryptHttpResponse(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        token: JString<'_>,
        request: JString<'_>,
        response: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(decrypt(
                &token.to_string(),
                &request.to_string(),
                &response.to_string(),
            ))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}

#[cfg(test)]
#[path = "../../tests/unit/ffi/http_crypto.rs"]
mod tests;
