package com.ismartcoding.plain.rust.native

import com.ismartcoding.plain.rustprefs.plain_report_pairing_response
import com.ismartcoding.plain.rustprefs.plain_report_notification
import com.ismartcoding.plain.rustprefs.plain_report_image_catalog_changed
import com.ismartcoding.plain.rustprefs.plain_report_network_interfaces_changed
import com.ismartcoding.plain.rustprefs.plain_report_ble_advertisement
import com.ismartcoding.plain.rustprefs.plain_report_ble_scan_state
import com.ismartcoding.plain.rustprefs.plain_report_ble_advertising_facts
import com.ismartcoding.plain.rustprefs.plain_report_screen_mirror_state
import com.ismartcoding.plain.rustprefs.plain_report_screen_mirror_codec
import com.ismartcoding.plain.rustprefs.plain_report_screen_mirror_audio_granted
import com.ismartcoding.plain.rustprefs.plain_report_sms_provider_changed
import com.ismartcoding.plain.rustprefs.plain_report_sms_part_sent
import com.ismartcoding.plain.rustprefs.plain_report_clipboard
import com.ismartcoding.plain.rustprefs.plain_report_permissions
import com.ismartcoding.plain.rustprefs.plain_report_dlna_position
import com.ismartcoding.plain.rustprefs.plain_report_dlna_playback
import com.ismartcoding.plain.rustprefs.plain_report_dlna_seek_consumed
import com.ismartcoding.plain.rustprefs.plain_report_dlna_stopped
import com.ismartcoding.plain.rustprefs.plain_report_audio_started
import com.ismartcoding.plain.rustprefs.plain_report_audio_completed
import com.ismartcoding.plain.rustprefs.plain_report_audio_progress
import com.ismartcoding.plain.rustprefs.plain_report_screen_mirror_video
import com.ismartcoding.plain.rustprefs.plain_report_screen_mirror_audio
import com.ismartcoding.plain.rustprefs.plain_accept_dlna_cast
import com.ismartcoding.plain.rustprefs.plain_acquire_mdns_debug_session
import com.ismartcoding.plain.rustprefs.plain_add_scan_history
import com.ismartcoding.plain.rustprefs.plain_ble_incoming
import com.ismartcoding.plain.rustprefs.plain_ble_incoming_buffer_free
import com.ismartcoding.plain.rustprefs.plain_cancel_app_update_download
import com.ismartcoding.plain.rustprefs.plain_check_app_update
import com.ismartcoding.plain.rustprefs.plain_check_http
import com.ismartcoding.plain.rustprefs.plain_clear_notification_filter_apps
import com.ismartcoding.plain.rustprefs.plain_complete_onboarding
import com.ismartcoding.plain.rustprefs.plain_confirm_web_login
import com.ismartcoding.plain.rustprefs.plain_core_start
import com.ismartcoding.plain.rustprefs.plain_create_crash_report
import com.ismartcoding.plain.rustprefs.plain_create_session
import com.ismartcoding.plain.rustprefs.plain_dlna_receiver
import com.ismartcoding.plain.rustprefs.plain_host_next
import com.ismartcoding.plain.rustprefs.plain_host_reply
import com.ismartcoding.plain.rustprefs.plain_submit_thumbnail
import com.ismartcoding.plain.rustprefs.plain_export_app_backup
import com.ismartcoding.plain.rustprefs.plain_file_icon
import com.ismartcoding.plain.rustprefs.plain_file_paths
import com.ismartcoding.plain.rustprefs.plain_graphql
import com.ismartcoding.plain.rustprefs.plain_encrypt_http_request
import com.ismartcoding.plain.rustprefs.plain_encrypt_resource_reference
import com.ismartcoding.plain.rustprefs.plain_decrypt_http_response
import com.ismartcoding.plain.rustprefs.plain_grant_background_execution
import com.ismartcoding.plain.rustprefs.plain_revoke_background_execution
import com.ismartcoding.plain.rustprefs.plain_execution_state_changed
import com.ismartcoding.plain.rustprefs.plain_http_status
import com.ismartcoding.plain.rustprefs.plain_import_note_image
import com.ismartcoding.plain.rustprefs.plain_import_pomodoro_sound
import com.ismartcoding.plain.rustprefs.plain_import_tls_certificate_pem
import com.ismartcoding.plain.rustprefs.plain_import_tls_certificate_pkcs12
import com.ismartcoding.plain.rustprefs.plain_is_pairing_request_current
import com.ismartcoding.plain.rustprefs.plain_lan_ipv4_addresses
import com.ismartcoding.plain.rustprefs.plain_load_preferences
import com.ismartcoding.plain.rustprefs.plain_prefs_string_free
import com.ismartcoding.plain.rustprefs.plain_reconnect_peers
import com.ismartcoding.plain.rustprefs.plain_regenerate_tls_certificate
import com.ismartcoding.plain.rustprefs.plain_reject_dlna_cast
import com.ismartcoding.plain.rustprefs.plain_release_mdns_debug_session
import com.ismartcoding.plain.rustprefs.plain_remove_dlna_rule
import com.ismartcoding.plain.rustprefs.plain_remove_scan_history
import com.ismartcoding.plain.rustprefs.plain_reset_adb_token
import com.ismartcoding.plain.rustprefs.plain_reset_password
import com.ismartcoding.plain.rustprefs.plain_reset_url_token
import com.ismartcoding.plain.rustprefs.plain_resource_close
import com.ismartcoding.plain.rustprefs.plain_resource_file
import com.ismartcoding.plain.rustprefs.plain_resource_open
import com.ismartcoding.plain.rustprefs.plain_resource_write
import com.ismartcoding.plain.rustprefs.plain_restart_http_server
import com.ismartcoding.plain.rustprefs.plain_retry_dlna_receiver
import com.ismartcoding.plain.rustprefs.plain_scan_history
import com.ismartcoding.plain.rustprefs.plain_set_notification_filter_mode
import com.ismartcoding.plain.rustprefs.plain_set_pref
import com.ismartcoding.plain.rustprefs.plain_stage_app_restore
import com.ismartcoding.plain.rustprefs.plain_start_app_update_download
import com.ismartcoding.plain.rustprefs.plain_tls_certificate_signature
import com.ismartcoding.plain.rustprefs.plain_toggle_notification_app
import com.ismartcoding.plain.rustprefs.plain_upload_chunk
import com.ismartcoding.plain.rustprefs.plain_verify_adb_token
import com.ismartcoding.plain.rustprefs.plain_graphql_subscription_open
import com.ismartcoding.plain.rustprefs.plain_graphql_subscription_send
import com.ismartcoding.plain.rustprefs.plain_graphql_subscription_read
import com.ismartcoding.plain.rustprefs.plain_graphql_subscription_close
import com.ismartcoding.plain.rust.graphql.NativeSubscriptionReply
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
internal actual object RustNativeBindings {
    actual fun fileIcon(extension: String): String = memScoped {
        readString(plain_file_icon(extension.cstr.ptr))
    }

    actual fun startCore(dataDir: String, token: String, configJson: String): String = memScoped {
        readString(plain_core_start(dataDir.cstr.ptr, token.cstr.ptr, configJson.cstr.ptr))
    }

    actual fun graphql(request: String): String = memScoped {
        readString(plain_graphql(request.cstr.ptr))
    }
    actual fun encryptHttpRequest(token: String, payload: String): String = memScoped {
        readString(plain_encrypt_http_request(token.cstr.ptr, payload.cstr.ptr))
    }
    actual fun encryptResourceReference(token: String, payload: String): String = memScoped {
        readString(plain_encrypt_resource_reference(token.cstr.ptr, payload.cstr.ptr))
    }
    actual fun decryptHttpResponse(token: String, endpoint: String, request: String, response: String): String = memScoped {
        readString(plain_decrypt_http_response(token.cstr.ptr, endpoint.cstr.ptr, request.cstr.ptr, response.cstr.ptr))
    }
    actual fun graphqlSubscriptionOpen(target: String?): Long = memScoped {
        val reply = JsonHelper.jsonDecode<NativeSubscriptionReply>(readString(plain_graphql_subscription_open(target?.cstr?.ptr)))
        reply.error?.let { error(it) }
        requireNotNull(reply.handle) { "Subscription handle missing" }
    }
    actual fun graphqlSubscriptionSend(handle: Long, message: String) = memScoped {
        val error = readString(plain_graphql_subscription_send(handle.toULong(), message.cstr.ptr))
        require(error.isEmpty()) { error }
    }
    actual fun graphqlSubscriptionRead(handle: Long): String {
        val reply = JsonHelper.jsonDecode<NativeSubscriptionReply>(readString(plain_graphql_subscription_read(handle.toULong())))
        reply.error?.let { error(it) }
        return requireNotNull(reply.value) { "Subscription message missing" }
    }
    actual fun graphqlSubscriptionClose(handle: Long) {
        val error = readString(plain_graphql_subscription_close(handle.toULong()))
        require(error.isEmpty()) { error }
    }


    actual fun reportPairingResponse(payload: String): String = memScoped { readString(plain_report_pairing_response(payload.cstr.ptr)) }
    actual fun reportNotification(payload: String): String = memScoped { readString(plain_report_notification(payload.cstr.ptr)) }
    actual fun reportImageCatalogChanged(payload: String): String = memScoped { readString(plain_report_image_catalog_changed(payload.cstr.ptr)) }
    actual fun reportNetworkInterfacesChanged(payload: String): String = memScoped { readString(plain_report_network_interfaces_changed(payload.cstr.ptr)) }
    actual fun reportBleAdvertisement(payload: String): String = memScoped { readString(plain_report_ble_advertisement(payload.cstr.ptr)) }
    actual fun reportBleScanState(payload: String): String = memScoped { readString(plain_report_ble_scan_state(payload.cstr.ptr)) }
    actual fun reportBleAdvertisingFacts(payload: String): String = memScoped { readString(plain_report_ble_advertising_facts(payload.cstr.ptr)) }
    actual fun reportScreenMirrorState(payload: String): String = memScoped { readString(plain_report_screen_mirror_state(payload.cstr.ptr)) }
    actual fun reportScreenMirrorCodec(payload: String): String = memScoped { readString(plain_report_screen_mirror_codec(payload.cstr.ptr)) }
    actual fun reportScreenMirrorAudioGranted(payload: String): String = memScoped { readString(plain_report_screen_mirror_audio_granted(payload.cstr.ptr)) }
    actual fun reportSmsProviderChanged(payload: String): String = memScoped { readString(plain_report_sms_provider_changed(payload.cstr.ptr)) }
    actual fun reportSmsPartSent(payload: String): String = memScoped { readString(plain_report_sms_part_sent(payload.cstr.ptr)) }
    actual fun reportClipboard(payload: String): String = memScoped { readString(plain_report_clipboard(payload.cstr.ptr)) }
    actual fun reportPermissions(payload: String): String = memScoped { readString(plain_report_permissions(payload.cstr.ptr)) }
    actual fun reportDlnaPosition(payload: String): String = memScoped { readString(plain_report_dlna_position(payload.cstr.ptr)) }
    actual fun reportDlnaPlayback(payload: String): String = memScoped { readString(plain_report_dlna_playback(payload.cstr.ptr)) }
    actual fun reportDlnaSeekConsumed(payload: String): String = memScoped { readString(plain_report_dlna_seek_consumed(payload.cstr.ptr)) }
    actual fun reportDlnaStopped(payload: String): String = memScoped { readString(plain_report_dlna_stopped(payload.cstr.ptr)) }
    actual fun reportAudioStarted(payload: String): String = memScoped { readString(plain_report_audio_started(payload.cstr.ptr)) }
    actual fun reportAudioCompleted(payload: String): String = memScoped { readString(plain_report_audio_completed(payload.cstr.ptr)) }
    actual fun reportAudioProgress(payload: String): String = memScoped { readString(plain_report_audio_progress(payload.cstr.ptr)) }
    actual fun reportScreenMirrorVideo(payload: ByteArray): String = payload.usePinned { pinned -> readString(plain_report_screen_mirror_video(if (payload.isEmpty()) null else pinned.addressOf(0).reinterpret(), payload.size.toULong())) }
    actual fun reportScreenMirrorAudio(payload: ByteArray): String = payload.usePinned { pinned -> readString(plain_report_screen_mirror_audio(if (payload.isEmpty()) null else pinned.addressOf(0).reinterpret(), payload.size.toULong())) }

    actual fun hostNext(): String = RustNativeReply.value(readString(plain_host_next()))
    actual fun hostReply(message: String): String = memScoped { readString(plain_host_reply(message.cstr.ptr)) }
    actual fun submitThumbnail(token: String, bytes: ByteArray): String = memScoped {
        bytes.usePinned { pinned ->
            readString(plain_submit_thumbnail(token.cstr.ptr, if (bytes.isEmpty()) null else pinned.addressOf(0).reinterpret(), bytes.size.toULong()))
        }
    }

    actual fun grantBackgroundExecution(id: String, deadlineMs: Long): String = memScoped {
        readString(plain_grant_background_execution(id.cstr.ptr, deadlineMs))
    }

    actual fun revokeBackgroundExecution(id: String): String = memScoped {
        readString(plain_revoke_background_execution(id.cstr.ptr))
    }

    actual fun executionStateChanged(): String = readString(plain_execution_state_changed())

    actual fun uploadChunk(id: String, index: Long, bytes: ByteArray): String = memScoped {
        bytes.usePinned { pinned ->
            readString(plain_upload_chunk(id.cstr.ptr, index,
                if (bytes.isEmpty()) null else pinned.addressOf(0).reinterpret(), bytes.size.toULong()))
        }
    }

    actual fun resourceOpen(id: String): String = memScoped {
        readString(plain_resource_open(id.cstr.ptr))
    }

    actual fun resourceWrite(id: String, bytes: ByteArray): String = memScoped {
        bytes.usePinned { pinned ->
            readString(plain_resource_write(id.cstr.ptr,
                if (bytes.isEmpty()) null else pinned.addressOf(0).reinterpret(), bytes.size.toULong()))
        }
    }

    actual fun resourceFile(id: String, fd: Int, offset: Long, length: Long): String = memScoped {
        readString(plain_resource_file(id.cstr.ptr, fd, offset, length))
    }

    actual fun resourceClose(id: String, failed: Boolean): String = memScoped {
        readString(plain_resource_close(id.cstr.ptr, failed))
    }

    actual fun restartHttpServer(): String = readString(plain_restart_http_server())

    actual fun httpStatus(): String = memScoped {
        val failure = alloc<CPointerVar<ByteVar>>()
        failure.value = null
        val pointer = plain_http_status(failure.ptr)
        try {
            failure.value?.let { error(it.toKString()) }
            checkNotNull(pointer) { "Rust core returned no HTTP status" }.toKString()
        } finally {
            plain_prefs_string_free(pointer)
            plain_prefs_string_free(failure.value)
        }
    }

    actual fun lanIpv4Addresses(): String = readString(plain_lan_ipv4_addresses())

    actual fun bleIncoming(remoteHost: String, characteristicUuid: String, bytes: ByteArray): ByteArray = memScoped {
        bytes.usePinned { pinned ->
            val buffer = plain_ble_incoming(remoteHost.cstr.ptr, characteristicUuid.cstr.ptr,
                if (bytes.isEmpty()) null else pinned.addressOf(0).reinterpret(), bytes.size.toULong())
            try {
                buffer.useContents {
                    val value = data?.reinterpret<ByteVar>()?.readBytes(len.toInt()) ?: ByteArray(0)
                    check(status > 0) { value.decodeToString() }
                    value
                }
            } finally { plain_ble_incoming_buffer_free(buffer) }
        }
    }

    actual fun checkHttp(): String = readString(plain_check_http())

    actual fun tlsCertificateSignature(): String = readString(plain_tls_certificate_signature())

    actual fun regenerateTlsCertificate(): String = readString(plain_regenerate_tls_certificate())

    actual fun importTlsCertificatePem(certificatePem: String, privateKeyPem: String): String = memScoped {
        readString(plain_import_tls_certificate_pem(certificatePem.cstr.ptr, privateKeyPem.cstr.ptr))
    }

    actual fun importTlsCertificatePkcs12(data: ByteArray, password: String): String = memScoped {
        data.usePinned { pinned ->
            readString(plain_import_tls_certificate_pkcs12(
                if (data.isEmpty()) null else pinned.addressOf(0).reinterpret(), data.size.toULong(), password.cstr.ptr))
        }
    }

    actual fun createSession(name: String): String = memScoped {
        readString(plain_create_session(name.cstr.ptr))
    }

    actual fun confirmWebLogin(requestId: String, allowed: Boolean): String = memScoped {
        readString(plain_confirm_web_login(requestId.cstr.ptr, allowed))
    }

    actual fun stageAppRestore(path: String): String = memScoped {
        readString(plain_stage_app_restore(path.cstr.ptr))
    }

    actual fun exportAppBackup(): String = readString(plain_export_app_backup())

    actual fun createCrashReport(report: String): String = memScoped {
        readString(plain_create_crash_report(report.cstr.ptr))
    }

    actual fun completeOnboarding(): String = readString(plain_complete_onboarding())

    actual fun setPref(key: String, value: String): String = memScoped {
        readString(plain_set_pref(key.cstr.ptr, value.cstr.ptr))
    }

    actual fun loadPreferencesResponse(): String = readString(plain_load_preferences())

    actual fun toggleNotificationApp(packageName: String): String = memScoped {
        readString(plain_toggle_notification_app(packageName.cstr.ptr))
    }

    actual fun verifyAdbToken(token: String): String = memScoped {
        readString(plain_verify_adb_token(token.cstr.ptr))
    }

    actual fun startAppUpdateDownload(): String = readString(plain_start_app_update_download())

    actual fun cancelAppUpdateDownload(): String = readString(plain_cancel_app_update_download())

    actual fun checkAppUpdate(automatic: Boolean): String = readString(plain_check_app_update(automatic))

    actual fun resetPassword(): String = readString(plain_reset_password())

    actual fun resetUrlToken(): String = readString(plain_reset_url_token())

    actual fun resetAdbToken(): String = readString(plain_reset_adb_token())

    actual fun setNotificationFilterMode(mode: String): String = memScoped {
        readString(plain_set_notification_filter_mode(mode.cstr.ptr))
    }

    actual fun clearNotificationFilterApps(): String = readString(plain_clear_notification_filter_apps())

    actual fun isPairingRequestCurrent(id: String, signature: String): String = memScoped {
        readString(plain_is_pairing_request_current(id.cstr.ptr, signature.cstr.ptr))
    }

    actual fun reconnectPeers(): String = readString(plain_reconnect_peers())

    actual fun filePaths(paths: String): String = memScoped {
        readString(plain_file_paths(paths.cstr.ptr))
    }

    actual fun importNoteImage(source: String, fileName: String): String = memScoped {
        readString(plain_import_note_image(source.cstr.ptr, fileName.cstr.ptr))
    }

    actual fun importPomodoroSound(source: String): String = memScoped {
        readString(plain_import_pomodoro_sound(source.cstr.ptr))
    }

    actual fun dlnaReceiver(): String = readString(plain_dlna_receiver())

    actual fun retryDlnaReceiver(): String = readString(plain_retry_dlna_receiver())

    actual fun removeDlnaRule(ip: String, allowed: Boolean): String = memScoped {
        readString(plain_remove_dlna_rule(ip.cstr.ptr, allowed))
    }

    actual fun acceptDlnaCast(remember: Boolean): String = readString(plain_accept_dlna_cast(remember))

    actual fun rejectDlnaCast(remember: Boolean): String = readString(plain_reject_dlna_cast(remember))

    actual fun scanHistory(): String = readString(plain_scan_history())

    actual fun addScanHistory(text: String): String = memScoped {
        readString(plain_add_scan_history(text.cstr.ptr))
    }

    actual fun removeScanHistory(text: String): String = memScoped {
        readString(plain_remove_scan_history(text.cstr.ptr))
    }

    actual fun acquireMdnsDebugSession(token: String): String = memScoped {
        readString(plain_acquire_mdns_debug_session(token.cstr.ptr))
    }

    actual fun releaseMdnsDebugSession(token: String): String = memScoped {
        readString(plain_release_mdns_debug_session(token.cstr.ptr))
    }

    private fun readString(pointer: CPointer<ByteVar>?): String = try {
        checkNotNull(pointer).toKString()
    } finally {
        plain_prefs_string_free(pointer)
    }
}
