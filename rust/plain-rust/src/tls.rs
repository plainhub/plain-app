use serde_json::{Value, json};
use std::{path::Path, sync::Mutex};
static LOCK: Mutex<()> = Mutex::new(());

pub fn identity() -> Result<(Vec<u8>, Vec<u8>), String> {
    let _guard = LOCK.lock().map_err(|e| e.to_string())?;
    load(&path()?)
}
fn path() -> Result<std::path::PathBuf, String> {
    Ok(super::prefs()?
        .path()
        .parent()
        .ok_or("Missing TLS directory")?
        .join("tls-identity.json"))
}
fn load(path: &Path) -> Result<(Vec<u8>, Vec<u8>), String> {
    match std::fs::read(path) {
        Ok(data) => {
            let v: Value = serde_json::from_slice(&data).map_err(|e| e.to_string())?;
            let cert = v["certificatePem"]
                .as_str()
                .ok_or("Invalid TLS certificate")?
                .as_bytes()
                .to_vec();
            let key = v["privateKeyPem"]
                .as_str()
                .ok_or("Invalid TLS key")?
                .as_bytes()
                .to_vec();
            plain_rs::http_transport::tls_config(&cert, &key)?;
            Ok((cert, key))
        }
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => generate(path),
        Err(e) => Err(e.to_string()),
    }
}
fn save(path: &Path, cert: &[u8], key: &[u8]) -> Result<(), String> {
    plain_rs::http_transport::tls_config(cert, key)?;
    let bytes = serde_json::to_vec(&json!({"certificatePem": std::str::from_utf8(cert).map_err(|e| e.to_string())?, "privateKeyPem": std::str::from_utf8(key).map_err(|e| e.to_string())?})).map_err(|e| e.to_string())?;
    let temp = path.with_extension("tmp");
    let mut options = std::fs::OpenOptions::new();
    options.write(true).create(true).truncate(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options.mode(0o600);
    }
    use std::io::Write;
    let mut file = options.open(&temp).map_err(|e| e.to_string())?;
    file.write_all(&bytes)
        .and_then(|_| file.sync_all())
        .map_err(|e| e.to_string())?;
    std::fs::rename(temp, path).map_err(|e| e.to_string())
}
fn generate(path: &Path) -> Result<(Vec<u8>, Vec<u8>), String> {
    let pair = plain_rs::tls::generate_pem(&["localhost".into(), "127.0.0.1".into(), "::1".into()])
        .map_err(|e| e.to_string())?;
    save(path, &pair.0, &pair.1)?;
    Ok(pair)
}
fn tlv<'a>(data: &mut &'a [u8], tag: u8) -> Result<&'a [u8], String> {
    if data.len() < 2 || data[0] != tag {
        return Err("Invalid X509 DER".into());
    }
    let n = data[1];
    *data = &data[2..];
    let length = if n < 128 {
        n as usize
    } else {
        let count = (n & 127) as usize;
        if count == 0 || count > std::mem::size_of::<usize>() || count > data.len() {
            return Err("Invalid DER length".into());
        }
        let mut length = 0usize;
        for byte in &data[..count] {
            length = length
                .checked_mul(256)
                .and_then(|v| v.checked_add(*byte as usize))
                .ok_or("DER length overflow")?;
        }
        *data = &data[count..];
        length
    };
    if length > data.len() {
        return Err("Truncated DER".into());
    }
    let (value, rest) = data.split_at(length);
    *data = rest;
    Ok(value)
}
fn signature(cert: &[u8]) -> Result<Vec<u8>, String> {
    let der = rustls_pemfile::certs(&mut std::io::Cursor::new(cert))
        .next()
        .ok_or("Missing certificate")?
        .map_err(|e| e.to_string())?;
    let mut input = der.as_ref();
    let mut sequence = tlv(&mut input, 0x30)?;
    if !input.is_empty() {
        return Err("Trailing certificate bytes".into());
    }
    tlv(&mut sequence, 0x30)?;
    tlv(&mut sequence, 0x30)?;
    let bits = tlv(&mut sequence, 0x03)?;
    if !sequence.is_empty() || bits.len() < 2 || bits[0] != 0 {
        return Err("Invalid certificate signature".into());
    }
    Ok(bits[1..].to_vec())
}
pub fn action(config: &str) -> Result<String, String> {
    let v: Value = serde_json::from_str(config).map_err(|e| e.to_string())?;
    let _guard = LOCK.lock().map_err(|e| e.to_string())?;
    let path = path()?;
    let pair = match v["action"].as_str() {
        Some("get") => load(&path)?,
        Some("generate") => generate(&path)?,
        Some("import") => {
            let cert = v["certificatePem"]
                .as_str()
                .ok_or("Missing certificate")?
                .as_bytes()
                .to_vec();
            let key = v["privateKeyPem"]
                .as_str()
                .ok_or("Missing key")?
                .as_bytes()
                .to_vec();
            signature(&cert)?;
            save(&path, &cert, &key)?;
            (cert, key)
        }
        _ => return Err("Unknown TLS action".into()),
    };
    Ok(json!({"signature": signature(&pair.0)?}).to_string())
}

#[cfg(test)]
#[path = "../tests/unit/tls.rs"]
mod tests;
