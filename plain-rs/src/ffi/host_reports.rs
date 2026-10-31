use super::{host::bridge, management::reply, values::{c_string, input}};

use std::ffi::c_char;

fn pairing_response(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_pairing_response(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_pairing_response(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => pairing_response(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn notification(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_notification(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_notification(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => notification(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn image_catalog_changed(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_image_catalog_changed(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_image_catalog_changed(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => image_catalog_changed(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn network_interfaces_changed(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_network_interfaces_changed(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_network_interfaces_changed(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => network_interfaces_changed(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn ble_advertisement(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_ble_advertisement(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_ble_advertisement(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => ble_advertisement(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn ble_scan_state(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_ble_scan_state(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_ble_scan_state(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => ble_scan_state(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn ble_advertising_facts(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_ble_advertising_facts(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_ble_advertising_facts(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => ble_advertising_facts(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn screen_mirror_state(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_screen_mirror_state(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_screen_mirror_state(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => screen_mirror_state(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn screen_mirror_codec(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_screen_mirror_codec(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_screen_mirror_codec(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => screen_mirror_codec(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn screen_mirror_audio_granted(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_screen_mirror_audio_granted(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_screen_mirror_audio_granted(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => screen_mirror_audio_granted(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn sms_provider_changed(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_sms_provider_changed(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_sms_provider_changed(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => sms_provider_changed(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn sms_part_sent(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_sms_part_sent(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_sms_part_sent(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => sms_part_sent(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn clipboard(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_clipboard(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_clipboard(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => clipboard(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn permissions(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_permissions(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_permissions(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => permissions(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn dlna_position(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_dlna_position(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_dlna_position(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => dlna_position(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn dlna_playback(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_dlna_playback(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_dlna_playback(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => dlna_playback(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn dlna_seek_consumed(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_dlna_seek_consumed(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_dlna_seek_consumed(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => dlna_seek_consumed(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn dlna_stopped(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_dlna_stopped(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_dlna_stopped(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => dlna_stopped(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn audio_started(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_audio_started(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_audio_started(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => audio_started(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn audio_completed(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_audio_completed(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_audio_completed(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => audio_completed(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn audio_progress(payload: String) -> String {
    reply!((|| -> Result<bool, String> {
        let (runtime, host) = bridge()?;
        runtime.block_on(host.report_audio_progress(payload))?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub extern "C" fn plain_report_audio_progress(payload: *const c_char) -> *mut c_char {
    c_string(match input(payload) {
        Ok(payload) => audio_progress(payload),
        Err(error) => reply!(Err::<bool, String>(error)),
    })
}

fn screen_mirror_video(payload: Vec<u8>) -> String {
    reply!((|| -> Result<bool, String> {
        let (_, host) = bridge()?;
        host.report_screen_mirror_video(payload)?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_report_screen_mirror_video(
    data: *const u8,
    len: usize,
) -> *mut c_char {
    let result = super::values::input_bytes(data, len).map(screen_mirror_video);
    c_string(result.unwrap_or_else(|error| reply!(Err::<bool, String>(error))))
}

fn screen_mirror_audio(payload: Vec<u8>) -> String {
    reply!((|| -> Result<bool, String> {
        let (_, host) = bridge()?;
        host.report_screen_mirror_audio(payload)?;
        Ok(true)
    })())
}
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plain_report_screen_mirror_audio(
    data: *const u8,
    len: usize,
) -> *mut c_char {
    let result = super::values::input_bytes(data, len).map(screen_mirror_audio);
    c_string(result.unwrap_or_else(|error| reply!(Err::<bool, String>(error))))
}

#[cfg(target_os = "android")]
mod android {
    use super::*;
    use jni::{EnvUnowned, errors::ThrowRuntimeExAndDefault, objects::{JByteArray, JObject, JString}, sys::jstring};

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportPairingResponse(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(pairing_response(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportNotification(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(notification(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportImageCatalogChanged(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(image_catalog_changed(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportNetworkInterfacesChanged(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(network_interfaces_changed(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportBleAdvertisement(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(ble_advertisement(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportBleScanState(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(ble_scan_state(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportBleAdvertisingFacts(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(ble_advertising_facts(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportScreenMirrorState(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(screen_mirror_state(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportScreenMirrorCodec(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(screen_mirror_codec(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportScreenMirrorAudioGranted(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(screen_mirror_audio_granted(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportSmsProviderChanged(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(sms_provider_changed(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportSmsPartSent(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(sms_part_sent(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportClipboard(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(clipboard(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportPermissions(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(permissions(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportDlnaPosition(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(dlna_position(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportDlnaPlayback(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(dlna_playback(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportDlnaSeekConsumed(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(dlna_seek_consumed(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportDlnaStopped(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(dlna_stopped(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportAudioStarted(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(audio_started(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportAudioCompleted(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(audio_completed(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportAudioProgress(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JString<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            env.new_string(audio_progress(payload.to_string()))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportScreenMirrorVideo(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JByteArray<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let bytes = env.convert_byte_array(&payload)?;
            env.new_string(screen_mirror_video(bytes))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_ismartcoding_plain_rust_native_RustNativeBindings_reportScreenMirrorAudio(
        mut env: EnvUnowned<'_>,
        _: JObject<'_>,
        payload: JByteArray<'_>,
    ) -> jstring {
        env.with_env(|env| -> jni::errors::Result<_> {
            let bytes = env.convert_byte_array(&payload)?;
            env.new_string(screen_mirror_audio(bytes))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
        .into_raw()
    }
}
