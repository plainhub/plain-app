package com.ismartcoding.plain.ui.page

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.models.WebConsoleViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.appfiles.AppFilesPage
import com.ismartcoding.plain.ui.page.apps.AppPage
import com.ismartcoding.plain.ui.page.apps.AppsPage
import com.ismartcoding.plain.ui.page.audio.AudioPage
import com.ismartcoding.plain.ui.page.chat.ChannelInfoPage
import com.ismartcoding.plain.ui.page.chat.ChatEditTextPage
import com.ismartcoding.plain.ui.page.chat.ChatListPage
import com.ismartcoding.plain.ui.page.chat.ChatPage
import com.ismartcoding.plain.ui.page.chat.ChatTextPage
import com.ismartcoding.plain.ui.page.nearby.NearbyPage
import com.ismartcoding.plain.ui.page.settings.WifiAwareDebugPage
import com.ismartcoding.plain.ui.page.chat.PeerInfoPage
import com.ismartcoding.plain.ui.page.connections.ApiTokenTipsPage
import com.ismartcoding.plain.ui.page.connections.ConnectionsPage
import com.ismartcoding.plain.ui.page.devoptions.WebDevPage
import com.ismartcoding.plain.ui.page.dlna.DlnaCastHistoryPage
import com.ismartcoding.plain.ui.page.dlna.DlnaReceiverPage
import com.ismartcoding.plain.ui.page.docs.DocsPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.page.feeds.FeedSettingsPage
import com.ismartcoding.plain.ui.page.feeds.FeedsPage
import com.ismartcoding.plain.ui.page.files.FilesPage
import com.ismartcoding.plain.ui.page.home.HomeFeaturesSelectionPage
import com.ismartcoding.plain.ui.page.home.HomePage
import com.ismartcoding.plain.ui.page.images.ImagesPage
import com.ismartcoding.plain.ui.page.media.PlayMediaPage
import com.ismartcoding.plain.ui.page.notes.NotePage
import com.ismartcoding.plain.ui.page.notes.NotesPage
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroPage
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroSettingsDialog
import com.ismartcoding.plain.platform.checkNotificationPermission
import com.ismartcoding.plain.platform.getOwnPackageName
import com.ismartcoding.plain.platform.printText
import com.ismartcoding.plain.platform.updateChatMessageTextAsync
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.discover.PairingResponder
import com.ismartcoding.plain.preferences.AdbTokenPreference
import com.ismartcoding.plain.preferences.resetAsync
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.i18n.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.page.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.page.scan.ScanPage
import com.ismartcoding.plain.ui.page.settings.BackupRestorePage
import com.ismartcoding.plain.ui.page.settings.ComponentShowcasePage
import com.ismartcoding.plain.ui.page.settings.DarkThemePage
import com.ismartcoding.plain.ui.page.settings.LanguagePage
import com.ismartcoding.plain.ui.page.settings.SettingsPage
import com.ismartcoding.plain.ui.page.tools.SoundMeterPage
import com.ismartcoding.plain.ui.page.videos.VideosPage
import com.ismartcoding.plain.ui.page.web.HowToUsePage
import com.ismartcoding.plain.ui.page.web.NotificationSettingsPage
import com.ismartcoding.plain.ui.page.web.WebSecurityPage
import com.ismartcoding.plain.ui.page.web.WebSettingsPage

@Composable
fun MainNavGraph(
    navController: NavHostController,
    mainVM: MainViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    notesVM: NotesViewModel,
    feedTagsVM: TagsViewModel,
    noteTagsVM: TagsViewModel,
    pomodoroVM: PomodoroViewModel,
) {
    val updateVM: UpdateViewModel = viewModel()
    val scope = rememberCoroutineScope()
    NavHost(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        navController = navController,
        startDestination = Routing.Home,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = LinearOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(150, 50, easing = LinearOutSlowInEasing))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300, easing = FastOutLinearInEasing),
            ) + fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300, easing = LinearOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(150, 50, easing = LinearOutSlowInEasing))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutLinearInEasing),
            ) + fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing))
        },
    ) {
        composable<Routing.Home> { HomePage(navController, mainVM, updateVM, peerVM, channelVM) }
        composable<Routing.Images> { ImagesPage(navController) }
        composable<Routing.Videos> { VideosPage(navController) }
        composable<Routing.Audio> { AudioPage(navController, audioPlaylistVM) }
        composable<Routing.ChatList> { ChatListPage(navController, mainVM, peerVM = peerVM, channelVM = channelVM) }
        composable<Routing.Apps> { AppsPage(navController) }
        composable<Routing.Docs> { DocsPage(navController) }
        composable<Routing.Notes> { NotesPage(navController, notesVM = notesVM, tagsVM = noteTagsVM) }
        composable<Routing.SoundMeter> { SoundMeterPage(navController) }
        composable<Routing.PomodoroTimer> {
            PomodoroPage(
                navController = navController,
                pomodoroVM = pomodoroVM,
                settingsDialog = { settings, onSettingsChange, onDismiss ->
                    PomodoroSettingsDialog(settings = settings, onSettingsChange = onSettingsChange, onDismiss = onDismiss)
                },
                onCheckNotificationPermission = { onGranted ->
                    checkNotificationPermission(Res.string.pomodoro_notification_prompt) { onGranted() }
                },
            )
        }
        composable<Routing.Settings> { SettingsPage(navController, updateVM, peerVM) }
        composable<Routing.DarkTheme> { DarkThemePage(navController) }
        composable<Routing.Language> { LanguagePage(navController) }
        composable<Routing.BackupRestore> { BackupRestorePage(navController) }
        composable<Routing.WebSettings> { WebSettingsPage(navController) }
        composable<Routing.CustomFeatures> { HomeFeaturesSelectionPage(navController) }
        composable<Routing.NotificationSettings> { NotificationSettingsPage(navController) }
        composable<Routing.Connections> { ConnectionsPage(navController) }
        composable<Routing.ApiTokenTips> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.ApiTokenTips>()
            ApiTokenTipsPage(navController, r.clientId, r.token)
        }
        composable<Routing.WebDev> {
            WebDevPage(navController, packageId = getOwnPackageName(), onResetToken = {
                coIO { AdbTokenPreference.resetAsync() }
            })
        }
        composable<Routing.WebSecurity> { WebSecurityPage(navController) }
        composable<Routing.Chat> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Chat>()
            ChatPage(navController, audioPlaylistVM = audioPlaylistVM, chatVM = chatVM, peerVM = peerVM, channelVM = channelVM, r.id)
        }
        composable<Routing.ChatInfo> {
            val chatTarget = chatVM.target.collectAsState()
            if (chatTarget.value.type == ChatTargetType.PEER) {
                PeerInfoPage(navController, chatVM.target, { chatVM.clearAllMessages() }, peerVM)
            } else {
                ChannelInfoPage(navController, chatVM, peerVM, channelVM)
            }
        }
        composable<Routing.ScanHistory> { ScanHistoryPage(navController) }
        composable<Routing.Scan> { ScanPage(navController) }
        composable<Routing.Feeds> { FeedsPage(navController) }
        composable<Routing.FeedSettings> { FeedSettingsPage(navController) }
        composable<Routing.HowToUse> {
            val webVM: WebConsoleViewModel = viewModel()
            HowToUsePage(navController, onRunDiagnostics = { webVM.dig() })
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
        composable<Routing.Text> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Text>()
            TextPage(navController, r.title, r.content, r.language)
        }
        composable<Routing.TextFile> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.TextFile>()
            TextFilePage(navController, r.path, r.title, r.mediaId, r.type)
        }
        composable<Routing.ChatText> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.ChatText>()
            ChatTextPage(navController, r.content, onPrint = { tm, jobName, content ->
                printText(tm, jobName, content)
            })
        }
        composable<Routing.ChatEditText> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.ChatEditText>()
            ChatEditTextPage(navController, r.content, onSave = { text ->
                coIO {
                    val originalChat = ChatDbHelper.getChatItem(r.id) ?: return@coIO
                    updateChatMessageTextAsync(originalChat, text)
                    chatVM.update(originalChat)
                }
            })
        }
        composable<Routing.OtherFile> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.OtherFile>()
            OtherFilePage(navController, r.path, r.title)
        }
        composable<Routing.PdfViewer> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.PdfViewer>()
            PdfPage(navController, r.uri, r.fileName)
        }
        composable<Routing.Files> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Files>()
            FilesPage(navController, audioPlaylistVM, r.folderPath)
        }
        composable<Routing.AppFiles> {
            AppFilesPage(navController)
        }
        composable<Routing.Nearby> {
            NearbyPage(navController, peerVM = peerVM)
        }
        composable<Routing.WifiAwareDebug> {
            WifiAwareDebugPage(navController)
        }
        composable<Routing.ComponentShowcase> { ComponentShowcasePage(navController) }
        composable<Routing.DlnaReceiver> { DlnaReceiverPage(navController) }
        composable<Routing.DlnaCastHistory> { DlnaCastHistoryPage(navController) }
        composable<Routing.PlayMedia> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.PlayMedia>()
            PlayMediaPage(navController, r.path, audioPlaylistVM)
        }
        composableNoAnim<Routing.PairingRequest> {
            val request = mainVM.pendingPairingRequest.value
            if (request != null) {
                PairingRequestPage(
                    request = request,
                    navController = navController,
                    onAccept = { coIO { PairingResponder.respond(request, true) } },
                    onDeny = { coIO { PairingResponder.respond(request, false) } },
                )
            } else {
                navController.popBackStack<Routing.PairingRequest>(inclusive = true)
            }
        }
        composableNoAnim<Routing.LoginRequest> {
            val event = mainVM.pendingLoginEvent.value
            if (event != null) {
                val clientIp = HttpServerManager.clientIpCache[event.clientId] ?: ""
                LoginRequestPage(
                    request = event.request,
                    clientIp = clientIp,
                    navController = navController,
                    onAccept = {
                        scope.launch(Dispatchers.Default) {
                            HttpServerManager.respondTokenAsync(event, clientIp)
                        }
                    },
                    onDeny = {
                        scope.launch(Dispatchers.Default) {
                            event.session.close(1013, "rejected")
                        }
                    },
                )
            } else {
                navController.popBackStack<Routing.LoginRequest>(inclusive = true)
            }
        }
        composableNoAnim<Routing.ChannelInviteRequest> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.ChannelInviteRequest>()

            DisposableEffect(r.channelId) {
                mainVM.pendingChannelInvite.value = ChannelInviteReceivedEvent(
                    channelId = r.channelId,
                    channelName = r.channelName,
                    ownerPeerId = r.ownerPeerId,
                    ownerPeerName = r.ownerPeerName,
                )
                onDispose {
                    if (mainVM.pendingChannelInvite.value?.channelId == r.channelId) {
                        mainVM.pendingChannelInvite.value = null
                    }
                }
            }

            ChannelInvitePage(
                channelName = r.channelName,
                ownerPeerName = r.ownerPeerName,
                onAccept = { channelVM.acceptInvite(r.channelId) { navController.popBackStack() } },
                onDecline = { channelVM.declineInvite(r.channelId); navController.popBackStack() },
            )
        }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.composableNoAnim(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        content = content,
    )
}
