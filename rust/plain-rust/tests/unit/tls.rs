use super::*;
#[test]
fn persists_rotates_and_rejects_invalid_identity() {
    let dir = std::env::temp_dir().join(format!("plain-mobile-tls-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let path = dir.join("identity.json");
    let first = load(&path).unwrap();
    assert_eq!(first, load(&path).unwrap());
    assert!(!signature(&first.0).unwrap().is_empty());
    let second = generate(&path).unwrap();
    assert_ne!(signature(&first.0).unwrap(), signature(&second.0).unwrap());
    assert!(save(&path, &first.0, &second.1).is_err());
    assert_eq!(second, load(&path).unwrap());
    assert!(signature(b"invalid PEM").is_err());
    std::fs::remove_dir_all(dir).unwrap();
}
#[test]
fn rejects_truncated_and_overflowing_der() {
    for bytes in [
        &[0x30, 0x80][..],
        &[0x30, 0x82, 0xff][..],
        &[0x30, 0x01][..],
        &[0x30, 0x89, 0, 0, 0, 0, 0, 0, 0, 0, 0][..],
    ] {
        let mut input = bytes;
        assert!(tlv(&mut input, 0x30).is_err());
    }
}
