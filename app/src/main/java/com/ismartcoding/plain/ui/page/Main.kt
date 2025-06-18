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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.events.AudioActionEvent
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.LinkPreviewHelper
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.base.PToast
import com.ismartcoding.plain.ui.base.ToastEvent
import com.ismartcoding.plain.ui.base.coil.newImageLoader
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.apps.AppPage
import com.ismartcoding.plain.ui.page.apps.AppsPage
import com.ismartcoding.plain.ui.page.chat.ChatEditTextPage
import com.ismartcoding.plain.ui.page.chat.ChatPage
import com.ismartcoding.plain.ui.page.chat.ChatTextPage
import com.ismartcoding.plain.ui.page.docs.DocsPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.page.feeds.FeedSettingsPage
import com.ismartcoding.plain.ui.page.feeds.FeedsPage
import com.ismartcoding.plain.ui.page.files.FilesPage
import com.ismartcoding.plain.ui.page.notes.NotePage
import com.ismartcoding.plain.ui.page.notes.NotesPage
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroPage
import com.ismartcoding.plain.ui.page.root.RootPage
import com.ismartcoding.plain.ui.page.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.page.scan.ScanPage
import com.ismartcoding.plain.ui.page.settings.AboutPage
import com.ismartcoding.plain.ui.page.settings.BackupRestorePage
import com.ismartcoding.plain.ui.page.settings.DarkThemePage
import com.ismartcoding.plain.ui.page.settings.LanguagePage
import com.ismartcoding.plain.ui.page.settings.SettingsPage
import com.ismartcoding.plain.ui.page.tools.SoundMeterPage
import com.ismartcoding.plain.ui.page.web.NotificationSettingsPage
import com.ismartcoding.plain.ui.page.web.SessionsPage
import com.ismartcoding.plain.ui.page.web.WebDevPage
import com.ismartcoding.plain.ui.page.web.WebLearnMorePage
import com.ismartcoding.plain.ui.page.web.WebSecurityPage
import com.ismartcoding.plain.ui.page.web.WebSettingsPage
import com.ismartcoding.plain.ui.theme.AppTheme
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalCoilApi::class)
@Composable
fun Main(
    navControllerState: MutableState<NavHostController?>,
    onLaunched: () -> Unit,
    mainVM: MainViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    pomodoroVM: PomodoroViewModel,
    notesVM: NotesViewModel = viewModel(key = "notesVM"),
    feedTagsVM: TagsViewModel = viewModel(key = "feedTagsVM"),
    noteTagsVM: TagsViewModel = viewModel(key = "noteTagsVM"),
    chatVM: ChatViewModel = viewModel(key = "chatVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    navControllerState.value = navController
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val view = LocalView.current
    var confirmDialogEvent by remember {
        mutableStateOf<ConfirmDialogEvent?>(null)
    }
    var loadingDialogEvent by remember {
        mutableStateOf<LoadingDialogEvent?>(null)
    }
    var toastState by remember {
        mutableStateOf<ToastEvent?>(null)
    }
    var dismissToastJob: Job? = null

    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(Unit) {
        onLaunched()
        scope.launch(Dispatchers.IO) {
            pomodoroVM.loadAsync(context)
        }
    }
    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is AudioActionEvent -> {
                    if (event.action == AudioAction.MEDIA_ITEM_TRANSITION) {
                        scope.launch(Dispatchers.IO) {
                            audioPlaylistVM.loadAsync(context)
                        }
                    }
                }
                is ConfirmDialogEvent -> {
                    confirmDialogEvent = event
                }

                is LoadingDialogEvent -> {
                    loadingDialogEvent = if (event.show) event else null
                }

                is ToastEvent -> {
                    toastState = event
                    dismissToastJob?.cancel()
                    dismissToastJob = coIO {
                        delay(event.duration)
                        toastState = null
                    }
                }

                is FetchLinkPreviewsEvent -> {
                    scope.launch(Dispatchers.IO) {
                        val data = event.chat.content.value as DMessageText
                        val urls = LinkPreviewHelper.extractUrls(data.text)
                        if (urls.isNotEmpty()) {
                            val links = ChatHelper.fetchLinkPreviewsAsync(context, urls).filter { !it.hasError }
                            if (links.isNotEmpty()) {
                                val updatedMessageText = DMessageText(data.text, links)
                                event.chat.content = DMessageContent(DMessageType.TEXT.value, updatedMessageText)
                                AppDatabase.instance.chatDao().updateData(
                                    ChatItemDataUpdate(
                                        event.chat.id,
                                        event.chat.content
                                    )
                                )
                                chatVM.update(event.chat)
                                val m = event.chat.toModel()
                                m.data = m.getContentData()
                                sendEvent(
                                    WebSocketEvent(
                                        EventType.MESSAGE_UPDATED,
                                        JsonHelper.jsonEncode(
                                            listOf(m)
                                        ),
                                    ),
                                )
                            }
                        }
                    }
                }

                is HttpApiEvents.PomodoroStartEvent -> {
                    pomodoroVM.timeLeft.intValue = event.timeLeft
                    pomodoroVM.startSession()
                }

                is HttpApiEvents.PomodoroPauseEvent -> {
                    pomodoroVM.pauseSession()
                }

                is HttpApiEvents.PomodoroStopEvent -> {
                    pomodoroVM.resetTimer()
                }

                else -> {
                    // Handle other events if necessary
                }
            }
        }
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        setSingletonImageLoaderFactory(::newImageLoader)

        DisposableEffect(useDarkTheme) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }

            if (isQPlus() && context.isGestureInteractionMode()) {
                window.isNavigationBarContrastEnforced = false
            }
            onDispose { }
        }

        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            NavHost(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                navController = navController,
                startDestination = Routing.Root,
            ) {
                composable<Routing.Root> { RootPage(navController, mainVM, audioPlaylistVM = audioPlaylistVM) }
                composable<Routing.Settings> { SettingsPage(navController) }
                composable<Routing.DarkTheme> { DarkThemePage(navController) }
                composable<Routing.Language> { LanguagePage(navController) }
                composable<Routing.BackupRestore> { BackupRestorePage(navController) }
                composable<Routing.About> { AboutPage(navController) }
                composable<Routing.WebSettings> { WebSettingsPage(navController, mainVM) }
                composable<Routing.NotificationSettings> { NotificationSettingsPage(navController) }
                composable<Routing.Sessions> { SessionsPage(navController) }
                composable<Routing.WebDev> { WebDevPage(navController) }
                composable<Routing.WebSecurity> { WebSecurityPage(navController) }
                composable<Routing.SoundMeter> { SoundMeterPage(navController) }
                composable<Routing.PomodoroTimer> {
                    PomodoroPage(navController, pomodoroVM)
                }
                composable<Routing.Chat> { ChatPage(navController, audioPlaylistVM = audioPlaylistVM, chatVM = chatVM) }
                composable<Routing.ScanHistory> { ScanHistoryPage(navController) }
                composable<Routing.Scan> { ScanPage(navController) }
                composable<Routing.Apps> { AppsPage(navController) }
                composable<Routing.Docs> { DocsPage(navController) }
                composable<Routing.Feeds> { FeedsPage(navController) }
                composable<Routing.FeedSettings> { FeedSettingsPage(navController) }
                composable<Routing.WebLearnMore> { WebLearnMorePage(navController) }
                composable<Routing.Notes> {
                    NotesPage(navController, notesVM = notesVM, tagsVM = noteTagsVM)
                }
                composable<Routing.AppDetails> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.AppDetails>()
                    AppPage(navController, r.id)
                }

                composable<Routing.FeedEntries> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.FeedEntries>()
                    FeedEntriesPage(navController, r.feedId, tagsVM = feedTagsVM)
                }

                composable<Routing.FeedEntry> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.FeedEntry>()
                    FeedEntryPage(navController, r.id, tagsVM = feedTagsVM)
                }

                composable<Routing.NotesCreate> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.NotesCreate>()
                    NotePage(navController, "", r.tagId, notesVM = notesVM, tagsVM = noteTagsVM)
                }

                composable<Routing.NoteDetail> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.NoteDetail>()
                    NotePage(navController, r.id, "", notesVM = notesVM, tagsVM = noteTagsVM)
                }

                composable<Routing.Text> {
                    val title = navController.previousBackStackEntry?.savedStateHandle?.get("title") ?: ""
                    val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                    val language = navController.previousBackStackEntry?.savedStateHandle?.get("language") ?: ""
                    TextPage(navController, title, content, language)
                }

                composable<Routing.TextFile> {
                    val path = navController.previousBackStackEntry?.savedStateHandle?.get("path") ?: ""
                    val title = navController.previousBackStackEntry?.savedStateHandle?.get("title") ?: ""
                    val type = navController.previousBackStackEntry?.savedStateHandle?.get("type") ?: ""
                    val mediaId = navController.previousBackStackEntry?.savedStateHandle?.get("mediaId") ?: ""
                    TextFilePage(navController, path, title, mediaId, type)
                }

                composable<Routing.ChatText> {
                    val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                    ChatTextPage(navController, content)
                }

                composable<Routing.ChatEditText> { backStackEntry ->
                    val content = navController.previousBackStackEntry?.savedStateHandle?.get("content") ?: ""
                    val r = backStackEntry.toRoute<Routing.ChatEditText>()
                    ChatEditTextPage(navController, r.id, content, chatVM)
                }

                composable<Routing.OtherFile> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.OtherFile>()
                    OtherFilePage(navController, r.path)
                }

                composable<Routing.PdfViewer> {
                    val uri = navController.previousBackStackEntry?.savedStateHandle?.get<Uri>("uri")
                    if (uri != null) {
                        PdfPage(navController, uri)
                    }
                }

                composable<Routing.Files> { backStackEntry ->
                    val r = backStackEntry.toRoute<Routing.Files>()
                    FilesPage(navController, FilesType.entries[r.fileType], audioPlaylistVM)
                }
            }

            loadingDialogEvent?.let {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            confirmDialogEvent?.let {
                AlertDialog(
                    onDismissRequest = { confirmDialogEvent = null },
                    title = { if (it.title.isNotEmpty()) Text(it.title) },
                    text = { Text(it.message) },
                    confirmButton = {
                        Button(
                            onClick = {
                                it.confirmButton.second()
                                confirmDialogEvent = null
                            }
                        ) {
                            Text(it.confirmButton.first)
                        }
                    },
                    dismissButton = {
                        if (it.dismissButton != null) {
                            TextButton(onClick = {
                                it.dismissButton.second()
                                confirmDialogEvent = null
                            }) {
                                Text(it.dismissButton.first)
                            }
                        }
                    }
                )
            }

            toastState?.let { event ->
                PToast(
                    message = event.message,
                    type = event.type,
                    onDismiss = {
                        toastState = null
                        dismissToastJob?.cancel()
                    }
                )
            }
        }
    }
}

