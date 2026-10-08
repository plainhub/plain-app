package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.concurrent.Volatile

/**
 * Swift-implemented network info provider. Returns device IPv4 addresses
 * via the Darwin `getifaddrs` API (not directly accessible from Kotlin/Native
 * without a custom .def file).
 */
interface IosNetworkInfoProvider {
    fun getDeviceIP4s(): List<String>
}

/**
 * Swift-implemented permission checker. iOS permission APIs (Photos, Bluetooth,
 * Location, Camera, etc.) are only accessible from Swift/ObjC, so the real
 * checks live in Swift and are bridged through this interface.
 *
 * The [permission] string is the `Permission` enum name from commonMain
 * (e.g. `"READ_MEDIA_IMAGES"`, `"ACCESS_FINE_LOCATION"`).
 */
interface IosPermissionChecker {
    fun isGranted(permission: String): Boolean
    fun requestPermission(permission: String)
    fun openAppSettings()
}

/**
 * Swift-implemented file picker. iOS document/photo pickers
 * (`UIDocumentPickerViewController`, `PHPickerViewController`) must be presented
 * from a `UIViewController` and use ObjC delegate protocols, so the real UI
 * lives in Swift and is bridged through this interface.
 *
 * Kotlin calls [pickFile] when commonMain emits `PickFileEvent`; Swift presents
 * the appropriate picker and, on completion, calls
 * [IosFilePickerCallback.onPickResult]. Likewise [exportFile] is called for
 * `ExportFileEvent` and Swift calls [IosFilePickerCallback.onExportResult].
 *
 * @param tag      `PickFileTag` name (e.g. `"SEND_MESSAGE"`, `"FEED"`)
 * @param type     `PickFileType` name (e.g. `"IMAGE_VIDEO"`, `"FILE"`, `"FOLDER"`)
 * @param multiple whether multiple selection is allowed
 */
interface IosFilePicker {
    fun pickFile(tag: String, type: String, multiple: Boolean)
    fun exportFile(type: String, fileName: String)
}

/**
 * Swift-implemented system UI controller. iOS Status Bar / Home Indicator
 * visibility is controlled by the key window's root `UIViewController`
 * (`prefersStatusBarHidden`, `prefersHomeIndicatorAutoHidden`,
 * `preferredScreenEdgesDeferringSystemGestures`). Because the Compose `Dialog`
 * creates its own window on top, the Swift implementation creates a
 * `PassThroughWindow` above all other windows whose root VC hides the system
 * bars, while its `hitTest` returns nil so touches reach the Compose UI below.
 * Kotlin calls [setImmersive] from [setImmersiveFullscreen] /
 * [exitImmersiveFullscreen] when the Stay-Online overlay opens/closes.
 */
interface IosSystemUiController {
    /**
     * Toggle immersive fullscreen. When enabled, Swift creates a
     * `PassThroughWindow` (above `.alert` level) whose root VC hides the
     * Status Bar and Home Indicator and defers all edge system gestures.
     * When disabled, the window is dismissed and the previous key window
     * is restored.
     */
    fun setImmersive(enabled: Boolean)
}

/**
 * Swift-implemented sound meter. iOS audio recording APIs (`AVAudioRecorder`)
 * are only accessible from Swift/ObjC, so the real recorder lives in Swift and
 * is bridged through this interface.
 *
 * Kotlin calls [start] when the sound meter screen opens, then polls
 * [peakPower] every ~180 ms to get the latest dBFS reading. When the screen
 * closes, [stop] releases the audio session.
 */
interface IosSoundMeter {
    fun start(): Boolean
    fun stop()
    fun peakPower(): Float
}

/**
 * Singleton registry that lets Swift register platform implementations at app
 * startup. Kotlin code in iosMain reads these providers from commonMain.
 */
object IosPlatformRegistry {
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

    fun getDeviceIP4s(): List<String> {
        val provider = _networkInfoProvider
        if (provider == null) {
            LogCat.w("IosPlatformRegistry: getDeviceIP4s() called before NetworkInfoProvider registered")
            return emptyList()
        }
        val result = provider.getDeviceIP4s().filter { !it.startsWith("169.254.") }
        if (result.isEmpty()) {
            LogCat.w("IosPlatformRegistry: NetworkInfoProvider.getDeviceIP4s() returned empty list")
        } else {
            LogCat.d("IosPlatformRegistry: getDeviceIP4s() -> ${result.joinToString()}")
        }
        return result
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

    /** Request immersive fullscreen (hide status bar + home indicator). */
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
