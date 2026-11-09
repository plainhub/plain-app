package com.ismartcoding.plain.rust.native

import com.ismartcoding.plain.features.nearby.MdnsSnapshot
import com.ismartcoding.plain.features.notifications.NotificationFilterMode
import com.ismartcoding.plain.features.sessions.CreatedSession
import com.ismartcoding.plain.features.settings.AppUpdateDownload
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object RustNativeApi {
    suspend fun restartHttpServer(): Boolean = nativeCall { RustNativeBindings.restartHttpServer() }

    suspend fun lanIpv4Addresses(): List<String> = nativeCall { RustNativeBindings.lanIpv4Addresses() }

    suspend fun bleIncoming(remoteHost: String, characteristicUuid: String, bytes: ByteArray): ByteArray =
        withContext(Dispatchers.IO) { RustNativeBindings.bleIncoming(remoteHost, characteristicUuid, bytes) }

    suspend fun checkHttp(): Boolean = nativeCall { RustNativeBindings.checkHttp() }

    suspend fun tlsCertificateSignature(): ByteArray = nativeCertificate { RustNativeBindings.tlsCertificateSignature() }

    suspend fun regenerateTlsCertificate(): ByteArray = nativeCertificate { RustNativeBindings.regenerateTlsCertificate() }

    suspend fun importTlsCertificatePem(certificatePem: String, privateKeyPem: String): ByteArray =
        nativeCertificate { RustNativeBindings.importTlsCertificatePem(certificatePem, privateKeyPem) }

    suspend fun importTlsCertificatePkcs12(data: ByteArray, password: String): ByteArray =
        nativeCertificate { RustNativeBindings.importTlsCertificatePkcs12(data, password) }

    suspend fun createSession(name: String): CreatedSession = nativeCall { RustNativeBindings.createSession(name) }

    suspend fun confirmWebLogin(requestId: String, allowed: Boolean): Boolean =
        nativeCall { RustNativeBindings.confirmWebLogin(requestId, allowed) }

    suspend fun stageAppRestore(path: String): Boolean = nativeCall { RustNativeBindings.stageAppRestore(path) }

    suspend fun exportAppBackup(): String = nativeCall { RustNativeBindings.exportAppBackup() }

    suspend fun createCrashReport(report: String): String = nativeCall { RustNativeBindings.createCrashReport(report) }

    suspend fun completeOnboarding(): Boolean = nativeCall { RustNativeBindings.completeOnboarding() }

    suspend fun setPref(
        key: String,
        value: JsonElement,
    ): Boolean = nativeCall { RustNativeBindings.setPref(key, value.toString()) }

    suspend fun toggleNotificationApp(packageName: String): Boolean = nativeCall { RustNativeBindings.toggleNotificationApp(packageName) }

    suspend fun verifyAdbToken(token: String): Boolean = nativeCall { RustNativeBindings.verifyAdbToken(token) }

    suspend fun startAppUpdateDownload(): AppUpdateDownload = nativeCall { RustNativeBindings.startAppUpdateDownload() }

    suspend fun cancelAppUpdateDownload(): AppUpdateDownload = nativeCall { RustNativeBindings.cancelAppUpdateDownload() }

    suspend fun checkAppUpdate(automatic: Boolean): String = nativeCall { RustNativeBindings.checkAppUpdate(automatic) }

    suspend fun resetPassword(): String = nativeCall { RustNativeBindings.resetPassword() }


    suspend fun resetAdbToken(): Boolean = nativeCall { RustNativeBindings.resetAdbToken() }

    suspend fun setNotificationFilterMode(mode: NotificationFilterMode): Boolean = nativeCall { RustNativeBindings.setNotificationFilterMode(mode.name.lowercase()) }

    suspend fun clearNotificationFilterApps(): Boolean = nativeCall { RustNativeBindings.clearNotificationFilterApps() }

    suspend fun isPairingRequestCurrent(id: String, signature: String): Boolean =
        nativeCall { RustNativeBindings.isPairingRequestCurrent(id, signature) }

    suspend fun reconnectPeers(): Boolean = nativeCall { RustNativeBindings.reconnectPeers() }

    suspend fun filePaths(paths: List<String>): List<String?> = nativeCall { RustNativeBindings.filePaths(JsonHelper.jsonEncode(paths)) }

    suspend fun importNoteImage(source: String, fileName: String): String =
        nativeCall { RustNativeBindings.importNoteImage(source, fileName) }

    suspend fun importPomodoroSound(source: String): String = nativeCall { RustNativeBindings.importPomodoroSound(source) }

    suspend fun dlnaReceiver(): JsonObject = nativeCall { RustNativeBindings.dlnaReceiver() }

    suspend fun retryDlnaReceiver(): JsonObject = nativeCall { RustNativeBindings.retryDlnaReceiver() }

    suspend fun removeDlnaRule(
        ip: String,
        allowed: Boolean,
    ): JsonObject = nativeCall { RustNativeBindings.removeDlnaRule(ip, allowed) }

    suspend fun acceptDlnaCast(remember: Boolean): JsonObject = nativeCall { RustNativeBindings.acceptDlnaCast(remember) }

    suspend fun rejectDlnaCast(remember: Boolean): JsonObject = nativeCall { RustNativeBindings.rejectDlnaCast(remember) }

    suspend fun scanHistory(): List<String> = nativeCall { RustNativeBindings.scanHistory() }

    suspend fun addScanHistory(text: String): Boolean = nativeCall { RustNativeBindings.addScanHistory(text) }

    suspend fun removeScanHistory(text: String): Boolean = nativeCall { RustNativeBindings.removeScanHistory(text) }

    suspend fun acquireMdnsDebugSession(token: String): MdnsSnapshot = nativeCall { RustNativeBindings.acquireMdnsDebugSession(token) }

    suspend fun releaseMdnsDebugSession(token: String): MdnsSnapshot = nativeCall { RustNativeBindings.releaseMdnsDebugSession(token) }
}
