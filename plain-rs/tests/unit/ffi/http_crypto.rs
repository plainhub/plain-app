use super::*;
use plain_libs::utils::base64::{base64_decode, base64_encode};
use plain_rs::infrastructure::transport::{http_request, http_response::Context};
use std::ffi::CString;

#[test]
fn response_ffi_binds_to_the_complete_request_without_an_endpoint_argument() {
    let key = [7; 32];
    let request =
        http_request::encrypt(&key, "/graphql", "{\"query\":\"{ __typename }\"}").unwrap();
    let plaintext = b"{\"data\":{\"__typename\":\"Query\"}}";
    let response = Context::new(&request).encrypt(&key, plaintext).unwrap();
    let token = CString::new(base64_encode(&key)).unwrap();
    let body = CString::new(base64_encode(&response)).unwrap();
    for changed in [false, true] {
        let mut request = request.clone();
        if changed {
            request[0] ^= 1;
        }
        let request = CString::new(base64_encode(&request)).unwrap();
        let pointer = plain_decrypt_http_response(token.as_ptr(), request.as_ptr(), body.as_ptr());
        let reply: plain_libs::serde_json::Value = plain_libs::serde_json::from_str(&input(pointer).unwrap()).unwrap();
        super::super::values::plain_prefs_string_free(pointer);
        if changed {
            assert_eq!(reply["error"], "Invalid encrypted HTTP response");
        } else {
            assert_eq!(base64_decode(reply["value"].as_str().unwrap()), plaintext);
        }
    }
}
