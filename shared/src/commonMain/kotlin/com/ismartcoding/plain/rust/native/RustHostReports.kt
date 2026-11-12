package com.ismartcoding.plain.rust.native

object RustHostReports {
    suspend fun pairingReply(payload: String): Boolean = nativeCall { RustNativeBindings.reportPairingReply(payload) }
    suspend fun notification(payload: String): Boolean = nativeCall { RustNativeBindings.reportNotification(payload) }
    suspend fun imageCatalogChanged(payload: String): Boolean = nativeCall { RustNativeBindings.reportImageCatalogChanged(payload) }
    suspend fun networkInterfacesChanged(payload: String): Boolean = nativeCall { RustNativeBindings.reportNetworkInterfacesChanged(payload) }
    suspend fun bleAdvertisement(payload: String): Boolean = nativeCall { RustNativeBindings.reportBleAdvertisement(payload) }
    suspend fun bleScanState(payload: String): Boolean = nativeCall { RustNativeBindings.reportBleScanState(payload) }
    suspend fun bleAdvertisingFacts(payload: String): Boolean = nativeCall { RustNativeBindings.reportBleAdvertisingFacts(payload) }
    suspend fun screenMirrorState(payload: String): Boolean = nativeCall { RustNativeBindings.reportScreenMirrorState(payload) }
    suspend fun screenMirrorCodec(payload: String): Boolean = nativeCall { RustNativeBindings.reportScreenMirrorCodec(payload) }
    suspend fun screenMirrorAudioGranted(payload: String): Boolean = nativeCall { RustNativeBindings.reportScreenMirrorAudioGranted(payload) }
    suspend fun smsProviderChanged(payload: String): Boolean = nativeCall { RustNativeBindings.reportSmsProviderChanged(payload) }
    suspend fun smsPartSent(payload: String): Boolean = nativeCall { RustNativeBindings.reportSmsPartSent(payload) }
    suspend fun clipboard(payload: String): Boolean = nativeCall { RustNativeBindings.reportClipboard(payload) }
    suspend fun permissions(payload: String): Boolean = nativeCall { RustNativeBindings.reportPermissions(payload) }
    suspend fun dlnaPosition(payload: String): Boolean = nativeCall { RustNativeBindings.reportDlnaPosition(payload) }
    suspend fun dlnaPlayback(payload: String): Boolean = nativeCall { RustNativeBindings.reportDlnaPlayback(payload) }
    suspend fun dlnaSeekConsumed(payload: String): Boolean = nativeCall { RustNativeBindings.reportDlnaSeekConsumed(payload) }
    suspend fun dlnaStopped(payload: String): Boolean = nativeCall { RustNativeBindings.reportDlnaStopped(payload) }
    suspend fun audioStarted(payload: String): Boolean = nativeCall { RustNativeBindings.reportAudioStarted(payload) }
    suspend fun audioCompleted(payload: String): Boolean = nativeCall { RustNativeBindings.reportAudioCompleted(payload) }
    suspend fun audioProgress(payload: String): Boolean = nativeCall { RustNativeBindings.reportAudioProgress(payload) }
    suspend fun screenMirrorVideo(payload: ByteArray): Boolean = nativeCall { RustNativeBindings.reportScreenMirrorVideo(payload) }
    suspend fun screenMirrorAudio(payload: ByteArray): Boolean = nativeCall { RustNativeBindings.reportScreenMirrorAudio(payload) }
}
