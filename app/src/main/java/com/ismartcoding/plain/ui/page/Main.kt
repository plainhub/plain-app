package com.ismartcoding.plain.ui.page

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.ismartcoding.plain.ui.page.web.WebSecurityPage
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
    val view = LocalView.current
    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)

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
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = MaterialTheme.colorScheme.backColor().toArgb()
        insetsController.isAppearanceLightStatusBars = !useDarkTheme
        insetsController.isAppearanceLightNavigationBars = !useDarkTheme

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
                RouteName.WEB_SECURITY to { WebSecurityPage(navController) },
                RouteName.EXCHANGE_RATE to { ExchangeRatePage(navController) },
                RouteName.SOUND_METER to { SoundMeterPage(navController) },
                RouteName.CHAT to { ChatPage(navController, sharedViewModel) },
                RouteName.CHAT_TEXT to { ChatTextPage(navController, sharedViewModel) },
                RouteName.TEXT to { TextPage(navController, sharedViewModel) },
                RouteName.SCAN_HISTORY to { ScanHistoryPage(navController) },
                RouteName.SCAN to { ScanPage(navController) },
                RouteName.MEDIA_PREVIEW to { MediaPreviewPage(navController) },
            ).forEach { (routeName, content) ->
                slideHorizontallyComposable(routeName.name) {
                    content()
                }
            }

            slideHorizontallyComposable(
                "${RouteName.CHAT_EDIT_TEXT.name}?id={id}",
                arguments = listOf(navArgument("id") { })
            ) { backStackEntry ->
                val arguments = requireNotNull(backStackEntry.arguments)
                val id = arguments.getString("id", "")
                ChatEditTextPage(navController, sharedViewModel, id)
            }
        }
    }
}

fun NavGraphBuilder.slideHorizontallyComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (AnimatedVisibilityScope.(NavBackStackEntry) -> Unit),
) {
    composable(
        route,
        arguments,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                animationSpec = tween(400)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                animationSpec = tween(400)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                animationSpec = tween(400)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                animationSpec = tween(400)
            )
        },
    ) {
        content(it)
    }
}

