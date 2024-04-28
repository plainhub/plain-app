package com.ismartcoding.plain.ui.page

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.extensions.parcelableArrayList
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.ConfirmDialogEvent
import com.ismartcoding.plain.features.LoadingDialogEvent
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.TextEditorDialog
import com.ismartcoding.plain.ui.audio.AudioPlayerDialog
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.extensions.navigatePdf
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.page.apps.AppPage
import com.ismartcoding.plain.ui.page.apps.AppsPage
import com.ismartcoding.plain.ui.page.apps.AppsSearchPage
import com.ismartcoding.plain.ui.page.audio.AudioPage
import com.ismartcoding.plain.ui.page.chat.ChatEditTextPage
import com.ismartcoding.plain.ui.page.chat.ChatPage
import com.ismartcoding.plain.ui.page.chat.ChatTextPage
import com.ismartcoding.plain.ui.page.docs.DocsPage
import com.ismartcoding.plain.ui.page.docs.DocsSearchPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesSearchPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.page.feeds.FeedSettingsPage
import com.ismartcoding.plain.ui.page.feeds.FeedsPage
import com.ismartcoding.plain.ui.page.notes.NotePage
import com.ismartcoding.plain.ui.page.notes.NotesPage
import com.ismartcoding.plain.ui.page.notes.NotesSearchPage
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
import com.ismartcoding.plain.ui.page.web.SessionsPage
import com.ismartcoding.plain.ui.page.web.WebDevPage
import com.ismartcoding.plain.ui.page.web.WebLearnMorePage
import com.ismartcoding.plain.ui.page.web.WebSecurityPage
import com.ismartcoding.plain.ui.page.web.WebSettingsPage
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.theme.AppTheme
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Main(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val sharedViewModel: SharedViewModel = viewModel()
    val scope = rememberCoroutineScope()
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
        val intent = MainActivity.instance.get()?.intent
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType != null) {
                    if (mimeType.startsWith("audio/") ||
                        setOf("application/ogg", "application/x-ogg", "application/itunes").contains(mimeType)
                    ) {
                        AudioPlayerDialog().show()
                        Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                            AudioPlayer.play(context, DPlaylistAudio.fromPath(context, uri.toString()))
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
                        navController.navigatePdf(uri)
                    }
                }
            }
        } else if (intent?.action == Intent.ACTION_SEND) {
            if (intent.type?.startsWith("text/") == true) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return@LaunchedEffect
                scope.launch {
                    val item = withIO {
                        ChatHelper.sendAsync(DMessageContent(DMessageType.TEXT.value, DMessageText(sharedText)))
                    }
                    sendEvent(
                        WebSocketEvent(
                            EventType.MESSAGE_CREATED,
                            JsonHelper.jsonEncode(
                                arrayListOf(
                                    item.toModel().apply {
                                        data = this.getContentData()
                                    },
                                ),
                            ),
                        ),
                    )
                    navController.navigate(RouteName.CHAT)
                }
                return@LaunchedEffect
            }

            val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri ?: return@LaunchedEffect
            DialogHelper.showConfirmDialog(uri.toString(), getString(R.string.confirm_to_send_file_to_file_assistant),
                confirmButton = getString(R.string.ok) to {
                    navController.navigate(RouteName.CHAT)
                    scope.launch(Dispatchers.IO) {
                        delay(1000)
                        sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, setOf(uri)))
                    }
                },
                dismissButton = getString(R.string.cancel) to {})
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            DialogHelper.showConfirmDialog("", getString(R.string.confirm_to_send_files_to_file_assistant),
                confirmButton = getString(R.string.ok) to {
                    val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
                    if (uris != null) {
                        navController.navigate(RouteName.CHAT)
                        scope.launch(Dispatchers.IO) {
                            delay(1000)
                            sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, uris.toSet()))
                        }
                    }
                },
                dismissButton = getString(R.string.cancel) to {})
        }
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = MaterialTheme.colorScheme.background.toArgb()
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
                RouteName.WEB_SETTINGS to { WebSettingsPage(navController, viewModel) },
                RouteName.SESSIONS to { SessionsPage(navController) },
                RouteName.WEB_DEV to { WebDevPage(navController) },
                RouteName.WEB_SECURITY to { WebSecurityPage(navController) },
                RouteName.EXCHANGE_RATE to { ExchangeRatePage(navController) },
                RouteName.SOUND_METER to { SoundMeterPage(navController) },
                RouteName.CHAT to { ChatPage(navController, sharedViewModel) },
                RouteName.SCAN_HISTORY to { ScanHistoryPage(navController) },
                RouteName.SCAN to { ScanPage(navController) },
                RouteName.MEDIA_PREVIEW to { MediaPreviewPage(navController, sharedViewModel) },
                RouteName.APPS to { AppsPage(navController) },
                RouteName.DOCS to { DocsPage(navController) },
                RouteName.NOTES to { NotesPage(navController) },
                RouteName.FEEDS to { FeedsPage(navController) },
                RouteName.FEED_SETTINGS to { FeedSettingsPage(navController) },
                RouteName.WEB_LEARN_MORE to { WebLearnMorePage(navController) },
                RouteName.AUDIO to { AudioPage(navController) },
            ).forEach { (routeName, content) ->
                slideHorizontallyComposable(routeName.name) {
                    content()
                }
            }

            routeSearch(RouteName.APPS) {
                AppsSearchPage(navController, it)
            }

            routeDetail(RouteName.APPS) {
                AppPage(navController, it)
            }

            slideHorizontallyComposable(
                "${RouteName.FEED_ENTRIES.name}?feedId={feedId}",
                arguments = listOf(navArgument("feedId") {
                    nullable = true
                    defaultValue = ""
                    type = NavType.StringType
                }),
            ) {
                val feedId = it.arguments?.getString("feedId") ?: ""
                FeedEntriesPage(navController, feedId)
            }

            slideHorizontallyComposable(
                "${RouteName.FEED_ENTRIES.name}/search?q={q}&feedId={feedId}",
                arguments = listOf(navArgument("q") {
                    nullable = true
                    defaultValue = ""
                    type = NavType.StringType
                }, navArgument("feedId") {
                    nullable = true
                    defaultValue = ""
                    type = NavType.StringType
                }),
            ) {
                val q = it.arguments?.getString("q") ?: ""
                val feedId = it.arguments?.getString("feedId") ?: ""
                FeedEntriesSearchPage(navController, q, feedId)
            }

            routeDetail(RouteName.FEED_ENTRIES) {
                FeedEntryPage(navController, it)
            }

            routeSearch(RouteName.DOCS) {
                DocsSearchPage(navController, it)
            }

            routeSearch(RouteName.NOTES) {
                NotesSearchPage(navController, it)
            }

            slideHorizontallyComposable(
                "${RouteName.NOTES.name}/create?tagId={tagId}",
                arguments = listOf(navArgument("tagId") {
                    nullable = true
                    defaultValue = ""
                    type = NavType.StringType
                }),
            ) {
                val tagId = it.arguments?.getString("tagId") ?: ""
                NotePage(navController, "", tagId)
            }

            slideHorizontallyComposable(
                "${RouteName.NOTES.name}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                val id = it.arguments?.getString("id") ?: ""
                NotePage(navController, id, "")
            }

            slideHorizontallyComposable(RouteName.TEXT.name) {
                val title = navController.previousBackStackEntry?.savedStateHandle?.get("title") ?: ""
                val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                val language = navController.previousBackStackEntry?.savedStateHandle?.get("language") ?: ""
                TextPage(navController, title, content, language)
            }

            slideHorizontallyComposable(RouteName.TEXT_FILE.name) {
                val path = navController.previousBackStackEntry?.savedStateHandle?.get("path") ?: ""
                val title = navController.previousBackStackEntry?.savedStateHandle?.get("title") ?: ""
                val type = navController.previousBackStackEntry?.savedStateHandle?.get("type") ?: ""
                val mediaStoreId = navController.previousBackStackEntry?.savedStateHandle?.get("mediaStoreId") ?: ""
                TextFilePage(navController, path, title, mediaStoreId, type)
            }

            slideHorizontallyComposable(
                RouteName.CHAT_TEXT.name
            ) {
                val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                ChatTextPage(navController, content)
            }

            slideHorizontallyComposable(
                "${RouteName.TAGS.name}?dataType={dataType}",
                arguments = listOf(navArgument("dataType") { type = NavType.IntType }),
            ) {
                val dataType = it.arguments?.getInt("dataType") ?: -1
                TagsPage(navController, DataType.fromInt(dataType))
            }

            slideHorizontallyComposable(
                "${RouteName.CHAT_EDIT_TEXT.name}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                val id = it.arguments?.getString("id") ?: ""
                val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                ChatEditTextPage(navController, id, content)
            }

            slideHorizontallyComposable(
                RouteName.OTHER_FILE.name,
            ) {
                val path = navController.previousBackStackEntry?.savedStateHandle?.get("path") ?: ""
                OtherFilePage(navController, path)
            }

            slideHorizontallyComposable(
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

fun NavGraphBuilder.routeSearch(routeName: RouteName, action: @Composable (String) -> Unit) {
    slideHorizontallyComposable(
        "${routeName.name}/search?q={q}",
        arguments = listOf(navArgument("q") {
            nullable = true
            defaultValue = ""
            type = NavType.StringType
        }),
    ) {
        val q = it.arguments?.getString("q") ?: ""
        action(q)
    }
}

fun NavGraphBuilder.routeDetail(routeName: RouteName, action: @Composable (String) -> Unit) {
    slideHorizontallyComposable(
        "${routeName.name}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        val id = it.arguments?.getString("id") ?: ""
        action(id)
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
//        enterTransition = {
//            slideIntoContainer(
//                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
//                animationSpec = tween(PlainTheme.ANIMATION_DURATION),
//            )
//        },
//        exitTransition = {
//            slideOutOfContainer(
//                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
//                animationSpec = tween(PlainTheme.ANIMATION_DURATION),
//            )
//        },
//        popEnterTransition = {
//            slideIntoContainer(
//                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
//                animationSpec = tween(PlainTheme.ANIMATION_DURATION),
//            )
//        },
//        popExitTransition = {
//            slideOutOfContainer(
//                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
//                animationSpec = tween(PlainTheme.ANIMATION_DURATION),
//            )
//        },
    ) {
        content(it)
    }
}
