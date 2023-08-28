package com.ismartcoding.plain.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.extensions.parcelableArrayList
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.DarkTheme
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.data.preference.LocalDarkTheme
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.audio.DPlaylistAudio
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.PdfViewerDialog
import com.ismartcoding.plain.ui.TextEditorDialog
import com.ismartcoding.plain.ui.audio.AudioPlayerDialog
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.page.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.page.scan.ScanPage
import com.ismartcoding.plain.ui.page.settings.AboutPage
import com.ismartcoding.plain.ui.page.settings.BackupRestorePage
import com.ismartcoding.plain.ui.page.settings.ColorAndStylePage
import com.ismartcoding.plain.ui.page.settings.DarkThemePage
import com.ismartcoding.plain.ui.page.settings.LanguagePage
import com.ismartcoding.plain.ui.page.settings.LogsPage
import com.ismartcoding.plain.ui.page.settings.SettingsPage
import com.ismartcoding.plain.ui.page.tools.ExchangeRatePage
import com.ismartcoding.plain.ui.page.tools.SoundMeterPage
import com.ismartcoding.plain.ui.page.web.PasswordPage
import com.ismartcoding.plain.ui.page.web.SessionsPage
import com.ismartcoding.plain.ui.page.web.WebConsolePage
import com.ismartcoding.plain.ui.page.web.WebDevPage
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.theme.AppTheme
import com.ismartcoding.plain.ui.theme.backColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Main(
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val sharedViewModel: SharedViewModel = viewModel()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val intent = MainActivity.instance.get()?.intent
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType != null) {
                    if (mimeType.startsWith("audio/")
                        || setOf("application/ogg", "application/x-ogg", "application/itunes").contains(mimeType)
                    ) {
                        AudioPlayerDialog().show()
                        Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                            AudioPlayerService.play(context, DPlaylistAudio.fromPath(context, uri.toString()))
                        }
                    } else if (mimeType.startsWith("text/")) {
                        TextEditorDialog(uri).show()
                    } else if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                        val link = uri.toString()
                        PreviewDialog().show(
                            items = arrayListOf(PreviewItem(link, uri)),
                            initKey = link,
                        )
                    } else if (mimeType == "application/pdf") {
                        PdfViewerDialog(uri).show()
                    }
                }
            }
        } else if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri
            if (uri != null) {
                navController.navigate(RouteName.CHAT)
                scope.launch(Dispatchers.IO) {
                    delay(1000)
                    sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, setOf(uri)))
                }
            }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                navController.navigate(RouteName.CHAT)
                scope.launch(Dispatchers.IO) {
                    delay(1000)
                    sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, uris.toSet()))
                }
            }
        }
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        val systemUiController = rememberSystemUiController()
        systemUiController.run {
            setStatusBarColor(Color.Transparent, !useDarkTheme)
            setSystemBarsColor(Color.Transparent, !useDarkTheme)
            setNavigationBarColor(MaterialTheme.colorScheme.backColor(), !useDarkTheme)
        }

        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            navController = navController,
            startDestination = RouteName.HOME.name,
        ) {
            mapOf<RouteName, @Composable () -> Unit>(
                RouteName.HOME to { HomePage(navController, viewModel) },
                RouteName.SETTINGS to { SettingsPage(navController) },
                RouteName.COLOR_AND_STYLE to { ColorAndStylePage(navController) },
                RouteName.DARK_THEME to { DarkThemePage(navController) },
                RouteName.LANGUAGE to { LanguagePage(navController) },
                RouteName.BACKUP_RESTORE to { BackupRestorePage(navController) },
                RouteName.ABOUT to { AboutPage(navController) },
                RouteName.LOGS to { LogsPage(navController) },
                RouteName.WEB_CONSOLE to { WebConsolePage(navController, sharedViewModel) },
                RouteName.PASSWORD to { PasswordPage(navController) },
                RouteName.SESSIONS to { SessionsPage(navController) },
                RouteName.WEB_DEV to { WebDevPage(navController) },
                RouteName.EXCHANGE_RATE to { ExchangeRatePage(navController) },
                RouteName.SOUND_METER to { SoundMeterPage(navController) },
                RouteName.CHAT to { ChatPage(navController, sharedViewModel) },
                RouteName.CHAT_TEXT to { ChatTextPage(navController, sharedViewModel) },
                RouteName.TEXT to { TextPage(navController, sharedViewModel) },
                RouteName.SCAN to { ScanPage(navController) },
                RouteName.SCAN_HISTORY to { ScanHistoryPage(navController) },
            ).forEach { (routeName, content) ->
                composable(routeName.name) {
                    content()
                }
            }
        }
    }
}

