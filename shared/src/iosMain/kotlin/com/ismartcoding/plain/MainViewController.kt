package com.ismartcoding.plain

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.ismartcoding.plain.db.DataInitializer
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.lib.logcat.DiskLogAdapter
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.DiskLogFormatStrategy
import com.ismartcoding.plain.platform.buildAppDatabase
import com.ismartcoding.plain.platform.dataStoreFilePath
import com.ismartcoding.plain.platform.initDatabase
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.preferences.SettingsProvider
import com.ismartcoding.plain.preferences.initDataStore
import com.ismartcoding.plain.ui.models.AppFilesViewModel
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedEntryViewModel
import com.ismartcoding.plain.ui.models.FeedSettingsViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.ImageEditorEditViewModel
import com.ismartcoding.plain.ui.models.ImageEditorViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.models.NoteViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.NotificationSettingsViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.ScanHistoryViewModel
import com.ismartcoding.plain.ui.models.SessionsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.TextFileViewModel
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.models.WebConsoleViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.page.dlna.DlnaCastRulesViewModel
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

    // Disk logging (isDebugBuild() returns false on iOS → WARN threshold)
    LogCat.addLogAdapter(
        DiskLogAdapter(
            DiskLogFormatStrategy.getInstance(),
            minPriority = LogCat.WARN,
        ),
    )

    // Global event collectors (MediaDurationFixQueue, sleep timer, etc.)
    AppEvents.register()
}

/**
 * Comprehensive ViewModel factory for iOS.
 *
 * On Android, `viewModel()` without a factory falls back to a reflection-based
 * factory that can instantiate any no-arg ViewModel. On iOS the default
 * `SavedStateViewModelFactory` cannot create arbitrary ViewModels and throws
 * `UnsupportedOperationException`. This factory registers an `initializer` for
 * every ViewModel in commonMain so that any `viewModel()` call in the
 * composition tree resolves correctly.
 */
private val iosViewModelFactory = viewModelFactory {
    initializer { MainViewModel() }
    initializer { AudioPlaylistViewModel() }
    initializer { PomodoroViewModel() }
    initializer { ChatViewModel() }
    initializer { PeerViewModel() }
    initializer { ChannelViewModel() }
    initializer { NotesViewModel() }
    initializer { TagsViewModel() }
    initializer { AudioViewModel() }
    initializer { VideosViewModel() }
    initializer { ImagesViewModel() }
    initializer { DocsViewModel() }
    initializer { CastViewModel() }
    initializer { MediaFoldersViewModel() }
    initializer { FeedsViewModel() }
    initializer { FeedEntriesViewModel() }
    initializer { FeedEntryViewModel() }
    initializer { FeedSettingsViewModel() }
    initializer { AppsViewModel() }
    initializer { FilesViewModel() }
    initializer { AppFilesViewModel() }
    initializer { TextFileViewModel() }
    initializer { NoteViewModel() }
    initializer { MdEditorViewModel() }
    initializer { ScanHistoryViewModel() }
    initializer { ImageEditorViewModel() }
    initializer { ImageEditorEditViewModel() }
    initializer { WebConsoleViewModel() }
    initializer { NotificationSettingsViewModel() }
    initializer { DlnaReceiverViewModel() }
    initializer { DlnaCastRulesViewModel() }
    initializer { SessionsViewModel() }
    initializer { UpdateViewModel() }
}

/**
 * Wraps a [ViewModelStoreOwner] so that every `viewModel()` call without an
 * explicit factory uses [iosViewModelFactory] instead of the platform default
 * (which cannot instantiate arbitrary ViewModels on iOS).
 */
private class IosViewModelStoreOwner(
    private val delegate: ViewModelStoreOwner,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val viewModelStore: ViewModelStore get() = delegate.viewModelStore
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory get() = iosViewModelFactory
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
                val owner = LocalViewModelStoreOwner.current
                    ?: error("ViewModelStoreOwner not provided")
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides IosViewModelStoreOwner(owner),
                ) {
                    // With the global factory installed, `viewModel()` calls both
                    // here and inside Main/page defaults resolve correctly.
                    val mainVM: MainViewModel = viewModel()
                    val audioPlaylistVM: AudioPlaylistViewModel = viewModel()
                    val pomodoroVM: PomodoroViewModel = viewModel()
                    val chatVM: ChatViewModel = viewModel()
                    val peerVM: PeerViewModel = viewModel()
                    val channelVM: ChannelViewModel = viewModel()
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
}
