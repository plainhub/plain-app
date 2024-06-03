package com.ismartcoding.plain.ui.page

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.ConfirmDialogEvent
import com.ismartcoding.plain.features.LoadingDialogEvent
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.base.coil.newImageLoader
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.page.apps.AppPage
import com.ismartcoding.plain.ui.page.apps.AppsPage
import com.ismartcoding.plain.ui.page.audio.AudioPage
import com.ismartcoding.plain.ui.page.chat.ChatEditTextPage
import com.ismartcoding.plain.ui.page.chat.ChatPage
import com.ismartcoding.plain.ui.page.chat.ChatTextPage
import com.ismartcoding.plain.ui.page.docs.DocsPage
import com.ismartcoding.plain.ui.page.feeds.FeedSettingsPage
import com.ismartcoding.plain.ui.page.feeds.FeedsPage
import com.ismartcoding.plain.ui.page.images.ImagesPage
import com.ismartcoding.plain.ui.page.images.MediaFoldersPage
import com.ismartcoding.plain.ui.page.notes.NotePage
import com.ismartcoding.plain.ui.page.notes.NotesPage
import com.ismartcoding.plain.ui.page.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.page.scan.ScanPage
import com.ismartcoding.plain.ui.page.settings.AboutPage
import com.ismartcoding.plain.ui.page.settings.BackupRestorePage
import com.ismartcoding.plain.ui.page.settings.ColorAndStylePage
import com.ismartcoding.plain.ui.page.settings.DarkThemePage
import com.ismartcoding.plain.ui.page.settings.LanguagePage
import com.ismartcoding.plain.ui.page.settings.SettingsPage
import com.ismartcoding.plain.ui.page.tags.TagsPage
import com.ismartcoding.plain.ui.page.tools.ExchangeRatePage
import com.ismartcoding.plain.ui.page.tools.SoundMeterPage
import com.ismartcoding.plain.ui.page.videos.VideosPage
import com.ismartcoding.plain.ui.page.web.SessionsPage
import com.ismartcoding.plain.ui.page.web.WebDevPage
import com.ismartcoding.plain.ui.page.web.WebLearnMorePage
import com.ismartcoding.plain.ui.page.web.WebSecurityPage
import com.ismartcoding.plain.ui.page.web.WebSettingsPage
import com.ismartcoding.plain.ui.nav.RouteName
import com.ismartcoding.plain.ui.nav.feedEntriesGraph
import com.ismartcoding.plain.ui.nav.notesGraph
import com.ismartcoding.plain.ui.nav.routeDetail
import com.ismartcoding.plain.ui.theme.AppTheme
import kotlinx.coroutines.Job

@OptIn(ExperimentalAnimationApi::class, ExperimentalCoilApi::class)
@Composable
fun Main(navControllerState: MutableState<NavHostController?>, onLaunched: () -> Unit, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    navControllerState.value = navController
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val view = LocalView.current
    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)
    var confirmDialogEvent by remember {
        mutableStateOf<ConfirmDialogEvent?>(null)
    }
    var loadingDialogEvent by remember {
        mutableStateOf<LoadingDialogEvent?>(null)
    }
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    LaunchedEffect(Unit) {
        events.add(
            receiveEventHandler<ConfirmDialogEvent> { event ->
                confirmDialogEvent = event
            }
        )
        events.add(
            receiveEventHandler<LoadingDialogEvent> { event ->
                loadingDialogEvent = if (event.show) event else null
            }
        )
        onLaunched()
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        setSingletonImageLoaderFactory(::newImageLoader)

        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = if (context.isGestureInteractionMode()) Color.Transparent.toArgb() else MaterialTheme.colorScheme.background.toArgb()
        insetsController.isAppearanceLightStatusBars = !useDarkTheme
        insetsController.isAppearanceLightNavigationBars = !useDarkTheme

        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            navController = navController,
            startDestination = RouteName.HOME.name,
        ) {
            mapOf<RouteName, @Composable () -> Unit>(
                RouteName.HOME to { HomePage(navController, mainViewModel) },
                RouteName.SETTINGS to { SettingsPage(navController) },
                RouteName.COLOR_AND_STYLE to { ColorAndStylePage(navController) },
                RouteName.DARK_THEME to { DarkThemePage(navController) },
                RouteName.LANGUAGE to { LanguagePage(navController) },
                RouteName.BACKUP_RESTORE to { BackupRestorePage(navController) },
                RouteName.ABOUT to { AboutPage(navController) },
                RouteName.WEB_SETTINGS to { WebSettingsPage(navController, mainViewModel) },
                RouteName.SESSIONS to { SessionsPage(navController) },
                RouteName.WEB_DEV to { WebDevPage(navController) },
                RouteName.WEB_SECURITY to { WebSecurityPage(navController) },
                RouteName.EXCHANGE_RATE to { ExchangeRatePage(navController) },
                RouteName.SOUND_METER to { SoundMeterPage(navController) },
                RouteName.CHAT to { ChatPage(navController) },
                RouteName.SCAN_HISTORY to { ScanHistoryPage(navController) },
                RouteName.SCAN to { ScanPage(navController) },
                RouteName.APPS to { AppsPage(navController) },
                RouteName.DOCS to { DocsPage(navController) },
                RouteName.FEEDS to { FeedsPage(navController) },
                RouteName.FEED_SETTINGS to { FeedSettingsPage(navController) },
                RouteName.WEB_LEARN_MORE to { WebLearnMorePage(navController) },
                RouteName.AUDIO to { AudioPage(navController) },
            ).forEach { (routeName, content) ->
                composable(routeName.name) {
                    content()
                }
            }

            routeDetail(RouteName.APPS) { _, id ->
                AppPage(navController, id)
            }

            feedEntriesGraph(navController)
            notesGraph(navController)

            composable(
                "${RouteName.IMAGES.name}?bucketId={bucketId}",
                arguments = listOf(navArgument("bucketId") {
                    nullable = true
                    defaultValue = ""
                    type = NavType.StringType
                }),
            ) {
                val bucketId = it.arguments?.getString("bucketId") ?: ""
                ImagesPage(navController, bucketId)
            }

            composable(
                "${RouteName.VIDEOS.name}?bucketId={bucketId}",
                arguments = listOf(navArgument("bucketId") {
                    nullable = true
                    defaultValue = ""
                    type = NavType.StringType
                }),
            ) {
                val bucketId = it.arguments?.getString("bucketId") ?: ""
                VideosPage(navController, bucketId)
            }


            composable(RouteName.TEXT.name) {
                val title = navController.previousBackStackEntry?.savedStateHandle?.get("title") ?: ""
                val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                val language = navController.previousBackStackEntry?.savedStateHandle?.get("language") ?: ""
                TextPage(navController, title, content, language)
            }

            composable(RouteName.TEXT_FILE.name) {
                val path = navController.previousBackStackEntry?.savedStateHandle?.get("path") ?: ""
                val title = navController.previousBackStackEntry?.savedStateHandle?.get("title") ?: ""
                val type = navController.previousBackStackEntry?.savedStateHandle?.get("type") ?: ""
                val mediaId = navController.previousBackStackEntry?.savedStateHandle?.get("mediaId") ?: ""
                TextFilePage(navController, path, title, mediaId, type)
            }

            composable(
                RouteName.CHAT_TEXT.name
            ) {
                val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                ChatTextPage(navController, content)
            }

            composable(
                "${RouteName.TAGS.name}?dataType={dataType}",
                arguments = listOf(navArgument("dataType") { type = NavType.IntType }),
            ) {
                val dataType = it.arguments?.getInt("dataType") ?: -1
                TagsPage(navController, DataType.fromInt(dataType))
            }

            composable(
                "${RouteName.MEDIA_FOLDERS.name}?dataType={dataType}",
                arguments = listOf(navArgument("dataType") { type = NavType.IntType }),
            ) {
                val dataType = it.arguments?.getInt("dataType") ?: -1
                MediaFoldersPage(navController, DataType.fromInt(dataType))
            }


            routeDetail(RouteName.CHAT_EDIT_TEXT) { _, id ->
                val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                ChatEditTextPage(navController, id, content)
            }

            composable(
                RouteName.OTHER_FILE.name,
            ) {
                val path = navController.previousBackStackEntry?.savedStateHandle?.get("path") ?: ""
                OtherFilePage(navController, path)
            }

            composable(
                RouteName.PDF_VIEWER.name,
            ) {
                val uri = navController.previousBackStackEntry?.savedStateHandle?.get("uri") as? Uri
                if (uri != null) {
                    PdfPage(navController, uri)
                }
            }
        }

        if (confirmDialogEvent != null) {
            AlertDialog(onDismissRequest = {
                confirmDialogEvent = null
            }, title = if (confirmDialogEvent!!.title.isNotEmpty()) {
                {
                    Text(
                        confirmDialogEvent!!.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else null, text = {
                Text(confirmDialogEvent!!.message)
            }, confirmButton = {
                Button(onClick = {
                    confirmDialogEvent!!.confirmButton.second()
                    confirmDialogEvent = null
                }) {
                    Text(
                        confirmDialogEvent!!.confirmButton.first,
                    )
                }
            }, dismissButton = {
                confirmDialogEvent?.dismissButton?.let {
                    TextButton(onClick = {
                        it.second()
                        confirmDialogEvent = null
                    }) {
                        Text(it.first)
                    }
                }
            })
        }

        if (loadingDialogEvent != null) {
            Dialog(
                onDismissRequest = { loadingDialogEvent = null },
                DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

