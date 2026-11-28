#[path = "accessory_remote.rs"]
mod remote;
#[path = "accessory_routes.rs"]
mod routes;
use jni::{EnvUnowned, objects::JObject, sys::jint};
use plain_server::http::web_bundle::{EmbeddedBundle, WebBundle};
use std::{
    fs::File,
    io::{self, Read, Write},
    os::fd::BorrowedFd,
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_serveWebAccessory(
    mut env: EnvUnowned<'_>,
    _: JObject<'_>,
    fd: jint,
) {
    // The Android caller keeps the ParcelFileDescriptor open for this call.
    let descriptor = unsafe { BorrowedFd::borrow_raw(fd) };
    let result = descriptor
        .try_clone_to_owned()
        .and_then(|fd| serve(File::from(fd)));
    if let Err(error) = result {
        env.with_env(|env| {
            env.throw_new(
                jni::jni_str!("java/io/IOException"),
                jni::strings::JNIString::from(error.to_string()),
            )
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>();
    }
}

fn serve(mut accessory: File) -> io::Result<()> {
    let mut request = [0; 4096];
    let policy = remote::Policy {
        route: routes::resolve,
        endpoint: routes::endpoint,
    };
    let mut session = remote::Session::default();
    loop {
        let count = accessory.read(&mut request)?;
        if count == 0 {
            return Ok(());
        }
        if request[0] < 32 {
            let reply = if request[0] == 29 {
                let length = request[1..count]
                    .try_into()
                    .ok()
                    .map(u32::from_le_bytes)
                    .filter(|n| *n > 0 && *n <= 12 * 1024 * 1024)
                    .ok_or(io::ErrorKind::InvalidData)?;
                let mut packet = vec![0; length as usize];
                accessory.read_exact(&mut packet)?;
                session.exchange(&packet, &policy)
            } else {
                session.exchange(&request[..count], &policy)
            };
            write_packet(&mut accessory, Some(&reply))?;
            continue;
        }
        let allowed = super::server::with_core(|core| {
            core.server.runtime().prefs.get_or("remote_access", true)
        })
        .map_err(io::Error::other)?;
        if !allowed {
            return Err(io::Error::new(
                io::ErrorKind::PermissionDenied,
                "Remote access disabled",
            ));
        }
        let resource = std::str::from_utf8(&request[..count])
            .ok()
            .and_then(|path| path.strip_prefix('/'))
            .and_then(|path| EmbeddedBundle.read(path));
        write_packet(&mut accessory, resource)?;
    }
}

fn write_packet(accessory: &mut File, payload: Option<&[u8]>) -> io::Result<()> {
    let length = payload.map_or(u32::MAX, |bytes| bytes.len() as u32);
    accessory.write_all(&length.to_le_bytes())?;
    if let Some(bytes) = payload {
        for chunk in bytes.chunks(16 * 1024) {
            accessory.write_all(chunk)?;
        }
    }
    Ok(())
}
