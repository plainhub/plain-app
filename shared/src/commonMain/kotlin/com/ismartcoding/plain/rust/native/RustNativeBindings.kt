package com.ismartcoding.plain.rust.native

internal expect object RustNativeBindings {
    fun fileIcon(extension: String): String
    fun startCore(dataDir: String, token: String, configJson: String): String
    fun graphql(request: String): String
    fun encryptHttpRequest(token: String, payload: String): String
    fun encryptResourceReference(token: String, payload: String): String
    fun decryptHttpResponse(token: String, endpoint: String, request: String, response: String): String
    fun graphqlSubscriptionOpen(): Long
    fun graphqlSubscriptionSend(handle: Long, message: String)
    fun graphqlSubscriptionRead(handle: Long): String
    fun graphqlSubscriptionClose(handle: Long)
    fun reportPairingResponse(payload: String): String
    fun reportNotification(payload: String): String
    fun reportImageCatalogChanged(payload: String): String
    fun reportNetworkInterfacesChanged(payload: String): String
    fun reportBleAdvertisement(payload: String): String
    fun reportBleScanState(payload: String): String
    fun reportBleAdvertisingFacts(payload: String): String
    fun reportScreenMirrorState(payload: String): String
    fun reportScreenMirrorCodec(payload: String): String
    fun reportScreenMirrorAudioGranted(payload: String): String
    fun reportSmsProviderChanged(payload: String): String
    fun reportSmsPartSent(payload: String): String
    fun reportClipboard(payload: String): String
    fun reportPermissions(payload: String): String
    fun reportDlnaPosition(payload: String): String
    fun reportDlnaPlayback(payload: String): String
    fun reportDlnaSeekConsumed(payload: String): String
    fun reportDlnaStopped(payload: String): String
    fun reportAudioStarted(payload: String): String
    fun reportAudioCompleted(payload: String): String
    fun reportAudioProgress(payload: String): String
    fun reportScreenMirrorVideo(payload: ByteArray): String
    fun reportScreenMirrorAudio(payload: ByteArray): String
    fun hostNext(): String
    fun hostReply(message: String): String
    fun submitThumbnail(token: String, bytes: ByteArray): String
    fun grantBackgroundExecution(id: String, deadlineMs: Long): String
    fun revokeBackgroundExecution(id: String): String
    fun executionStateChanged(): String
    fun uploadChunk(id: String, index: Long, bytes: ByteArray): String
    fun resourceOpen(id: String): String
    fun resourceWrite(id: String, bytes: ByteArray): String
    fun resourceFile(id: String, fd: Int, offset: Long, length: Long): String
    fun resourceClose(id: String, failed: Boolean): String
    fun restartHttpServer(): String
    fun httpStatus(): String
    fun lanIpv4Addresses(): String
    fun bleIncoming(remoteHost: String, characteristicUuid: String, bytes: ByteArray): ByteArray
    fun checkHttp(): String
    fun tlsCertificateSignature(): String
    fun regenerateTlsCertificate(): String
    fun importTlsCertificatePem(certificatePem: String, privateKeyPem: String): String
    fun importTlsCertificatePkcs12(data: ByteArray, password: String): String
    fun createSession(name: String): String
    fun confirmWebLogin(requestId: String, allowed: Boolean): String
    fun stageAppRestore(path: String): String
    fun exportAppBackup(): String
    fun createCrashReport(report: String): String
    fun completeOnboarding(): String
    fun setPref(key: String, value: String): String
    fun loadPreferencesResponse(): String
    fun toggleNotificationApp(packageName: String): String
    fun verifyAdbToken(token: String): String
    fun startAppUpdateDownload(): String
    fun cancelAppUpdateDownload(): String
    fun checkAppUpdate(automatic: Boolean): String
    fun resetPassword(): String
    fun resetUrlToken(): String
    fun resetAdbToken(): String
    fun setNotificationFilterMode(mode: String): String
    fun clearNotificationFilterApps(): String
    fun isPairingRequestCurrent(id: String, signature: String): String
    fun reconnectPeers(): String
    fun filePaths(paths: String): String
    fun importNoteImage(source: String, fileName: String): String
    fun importPomodoroSound(source: String): String
    fun dlnaReceiver(): String
    fun retryDlnaReceiver(): String
    fun removeDlnaRule(ip: String, allowed: Boolean): String
    fun acceptDlnaCast(remember: Boolean): String
    fun rejectDlnaCast(remember: Boolean): String
    fun scanHistory(): String
    fun addScanHistory(text: String): String
    fun removeScanHistory(text: String): String
    fun acquireMdnsDebugSession(token: String): String
    fun releaseMdnsDebugSession(token: String): String
}
