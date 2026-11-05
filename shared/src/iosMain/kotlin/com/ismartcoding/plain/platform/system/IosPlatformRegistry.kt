package com.ismartcoding.plain.platform.system



import com.ismartcoding.plain.ui.platform.media.IosVideoFrameObserver
import com.ismartcoding.plain.ui.platform.media.IosVideoFrameObservation
import platform.AVFoundation.AVPlayerLayer
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.concurrent.Volatile

interface IosNetworkInfoProvider {
    fun getNetworkType(): String
    fun isVpnActive(): Boolean
}

interface IosPermissionChecker {
    fun isGranted(permission: String): Boolean
    fun requestPermission(permission: String)
    fun openAppSettings()
}

interface IosFilePicker {
    fun grantedDirectories(): List<String>
    fun hasUnavailableDirectories(): Boolean
    fun pickFile(tag: String, type: String, multiple: Boolean)
    fun exportFile(type: String, fileName: String)
}

interface IosSystemUiController {
    fun setImmersive(enabled: Boolean)
}

interface IosSoundMeter {
    fun start(): Boolean
    fun stop()
    fun peakPower(): Float
}

object IosPlatformRegistry {
    private lateinit var videoFrameObserver: IosVideoFrameObserver
    fun setVideoFrameObserver(observer: IosVideoFrameObserver) { videoFrameObserver = observer }
    fun observeVideoFrame(layer: AVPlayerLayer, onReady: () -> Unit): IosVideoFrameObservation =
        videoFrameObserver.observe(layer, onReady)
    private var backgroundScheduler: IosBackgroundScheduler? = null
    fun setBackgroundScheduler(scheduler: IosBackgroundScheduler) { backgroundScheduler = scheduler }
    fun scheduleBackgroundWake(atMillis: Double): String = checkNotNull(backgroundScheduler).schedule(atMillis)
    fun networkType(): String = _networkInfoProvider?.getNetworkType() ?: "NONE"
    fun vpnActive(): Boolean = _networkInfoProvider?.isVpnActive() ?: false

    @Volatile
    private var _networkInfoProvider: IosNetworkInfoProvider? = null

    @Volatile
    private var _permissionChecker: IosPermissionChecker? = null

    @Volatile
    private var _filePicker: IosFilePicker? = null

    @Volatile
    private var _shareController: IosShareController? = null

    @Volatile
    private var _systemUiController: IosSystemUiController? = null

    @Volatile
    private var _soundMeter: IosSoundMeter? = null

    fun setNetworkInfoProvider(provider: IosNetworkInfoProvider) {
        _networkInfoProvider = provider
        LogCat.d("IosPlatformRegistry: network info provider registered")
    }

    fun setPermissionChecker(checker: IosPermissionChecker) {
        _permissionChecker = checker
        LogCat.d("IosPlatformRegistry: permission checker registered")
    }

    fun isPermissionGranted(permission: String): Boolean =
        _permissionChecker?.isGranted(permission) ?: true

    fun requestPermission(permission: String) {
        _permissionChecker?.requestPermission(permission)
    }

    fun openAppSettings() {
        _permissionChecker?.openAppSettings()
    }

    fun setFilePicker(picker: IosFilePicker) {
        _filePicker = picker
        LogCat.d("IosPlatformRegistry: file picker registered")
    }

    fun filePicker(): IosFilePicker? = _filePicker

    fun setShareController(controller: IosShareController) {
        _shareController = controller
        LogCat.d("IosPlatformRegistry: share controller registered")
    }

    fun shareController(): IosShareController? = _shareController

    fun setSystemUiController(controller: IosSystemUiController) {
        _systemUiController = controller
        LogCat.d("IosPlatformRegistry: system UI controller registered")
    }

    fun setImmersive(enabled: Boolean) {
        _systemUiController?.setImmersive(enabled)
            ?: LogCat.w("IosPlatformRegistry: setImmersive($enabled) called before SystemUiController registered")
    }

    fun setSoundMeter(meter: IosSoundMeter) {
        _soundMeter = meter
        LogCat.d("IosPlatformRegistry: sound meter registered")
    }

    fun soundMeter(): IosSoundMeter? = _soundMeter
}
