use std::ffi::{CStr, CString, c_char};

pub(crate) fn c_string(value: String) -> *mut c_char {
    CString::new(value).expect("string contains NUL").into_raw()
}

pub(crate) fn input(pointer: *const c_char) -> Result<String, String> {
    if pointer.is_null() {
        return Err("null string passed to Rust preferences".to_string());
    }
    // SAFETY: callers pass a null-terminated string valid for this call.
    Ok(unsafe { CStr::from_ptr(pointer) }
        .to_str()
        .map_err(|e| e.to_string())?
        .to_owned())
}
