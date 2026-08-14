package com.ismartcoding.plain

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.ismartcoding.plain.db.DataInitializer
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.platform.buildAppDatabase
import com.ismartcoding.plain.platform.dataStoreFilePath
import com.ismartcoding.plain.platform.initDatabase
import com.ismartcoding.plain.platform.initDiskLogging
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.preferences.SettingsProvider
import com.ismartcoding.plain.preferences.initDataStore
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.Main
import com.ismartcoding.plain.ui.theme.AppTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

private var initialized = false

/**
 * One-time iOS app initialization: DataStore, Room database, log adapters,
 * and event collectors. Idempotent — safe to call multiple times.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun initIosApp() {
    if (initialized) return
    initialized = true

    // Ensure the datastore directory exists before OkioStorage tries to write
    val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0] as String
    val datastoreDir = "$docsDir/datastore"
    NSFileManager.defaultManager.createDirectoryAtPath(datastoreDir, withIntermediateDirectories = true, attributes = null, error = null)

    // DataStore (multiplatform Preferences DataStore backed by okio)
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFilePath().toPath() },
    )
    initDataStore(dataStore)

    // Room database with the same onCreate seed data as Android
    initDatabase(
        buildAppDatabase(Constants.DATABASE_NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    DataInitializer(connection).apply {
                        insertWelcome()
                        insertTags()
                        insertNotes()
                    }
                }
            })
            .build(),
    )

    // Disk logging + HTTP request logging (debug=VERBOSE, release=WARN)
    initDiskLogging()

    // Global event collectors (MediaDurationFixQueue, sleep timer, etc.)
    AppEvents.register()

    // iOS permission request handler (bridges RequestPermissionsEvent → Swift)
    com.ismartcoding.plain.platform.IosPermissionEvents.register()

    // iOS file picker (bridges PickFileEvent/ExportFileEvent → Swift UIDocumentPicker/PHPicker)
    com.ismartcoding.plain.platform.IosFilePickerEvents.register()

    // iOS window focus events (bridges UIApplication.didBecomeActive/willResignActive → WindowFocusChangedEvent)
    com.ismartcoding.plain.platform.IosWindowFocusEvents.register()

    // Network path monitor (recreates the mDNS responder socket on network changes)
    com.ismartcoding.plain.platform.IosNetworkMonitor.init()

    // Initialize preferences and TempData (shared with Android via initCommonPreferences)
    coIO {
        initCommonPreferences()
    }
}

/**
 * Entry point called from Swift. Returns a UIViewController hosting the
 * Compose Multiplatform UI tree (SettingsProvider → AppTheme → Main).
 */
@Suppress("unused")
fun MainViewController(): UIViewController {
    initIosApp()
    return ComposeUIViewController {
        SettingsProvider {
            AppTheme(useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
                val navControllerState = remember { mutableStateOf<NavHostController?>(null) }
                // iOS has no reflection-based default ViewModel factory. Provide an
                // explicit factory for the top-level ViewModels created here. Page-level
                // ViewModels use `viewModel { XxxViewModel() }` initializers (already in
                // commonMain) which work on all platforms.
                val factory = viewModelFactory {
                    initializer { MainViewModel() }
                    initializer { AudioPlaylistViewModel() }
                    initializer { PomodoroViewModel() }
                    initializer { ChatViewModel() }
                    initializer { PeerViewModel() }
                    initializer { ChannelViewModel() }
                    initializer { NotesViewModel() }
                    initializer { TagsViewModel() }
                }
                val mainVM: MainViewModel = viewModel(factory = factory)
                val audioPlaylistVM: AudioPlaylistViewModel = viewModel(factory = factory)
                val pomodoroVM: PomodoroViewModel = viewModel(factory = factory)
                val chatVM: ChatViewModel = viewModel(factory = factory)
                val peerVM: PeerViewModel = viewModel(factory = factory)
                val channelVM: ChannelViewModel = viewModel(factory = factory)
                Main(
                    navControllerState = navControllerState,
                    onLaunched = {
                        // iOS has no Intent to handle; locale init runs in background.
                        MainScope().launch { com.ismartcoding.plain.enums.Language.initLocaleAsync() }
                    },
                    mainVM = mainVM,
                    audioPlaylistVM = audioPlaylistVM,
                    pomodoroVM = pomodoroVM,
                    chatVM = chatVM,
                    peerVM = peerVM,
                    channelVM = channelVM,
                )
            }
        }
    }
}
