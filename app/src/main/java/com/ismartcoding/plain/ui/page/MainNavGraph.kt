package com.ismartcoding.plain.ui.page

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.appfiles.AppFilesPage
import com.ismartcoding.plain.ui.page.apps.AppPage
import com.ismartcoding.plain.ui.page.chat.ChatEditTextPage
import com.ismartcoding.plain.ui.page.chat.ChatInfoPage
import com.ismartcoding.plain.ui.page.chat.ChatPage
import com.ismartcoding.plain.ui.page.chat.ChatTextPage
import com.ismartcoding.plain.ui.page.chat.NearbyPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.page.feeds.FeedSettingsPage
import com.ismartcoding.plain.ui.page.feeds.FeedsPage
import com.ismartcoding.plain.ui.page.files.FilesPage
import com.ismartcoding.plain.ui.page.notes.NotePage
import com.ismartcoding.plain.ui.page.root.RootPage
import com.ismartcoding.plain.ui.page.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.page.scan.ScanPage
import com.ismartcoding.plain.ui.page.settings.AboutPage
import com.ismartcoding.plain.ui.page.settings.BackupRestorePage
import com.ismartcoding.plain.ui.page.settings.DarkThemePage
import com.ismartcoding.plain.ui.page.settings.LanguagePage
import com.ismartcoding.plain.ui.page.settings.SettingsPage
import com.ismartcoding.plain.ui.page.web.NotificationSettingsPage
import com.ismartcoding.plain.ui.page.web.SessionsPage
import com.ismartcoding.plain.ui.page.web.WebDevPage
import com.ismartcoding.plain.ui.page.web.WebLearnMorePage
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
    onOpenDrawer: () -> Unit,
) {
    NavHost(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        navController = navController,
        startDestination = Routing.Root,
    ) {
        composable<Routing.Root> { RootPage(navController, mainVM, audioPlaylistVM = audioPlaylistVM, notesVM = notesVM, noteTagsVM = noteTagsVM, feedTagsVM = feedTagsVM, pomodoroVM = pomodoroVM) }
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
        composable<Routing.Chat> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Chat>()
            ChatPage(navController, audioPlaylistVM = audioPlaylistVM, chatVM = chatVM, peerVM = peerVM, channelVM = channelVM, r.id)
        }
        composable<Routing.ChatInfo> {
            ChatInfoPage(navController, chatVM = chatVM, peerVM = peerVM, channelVM = channelVM)
        }
        composable<Routing.ScanHistory> { ScanHistoryPage(navController) }
        composable<Routing.Scan> { ScanPage(navController) }
        composable<Routing.Feeds> { FeedsPage(navController) }
        composable<Routing.FeedSettings> { FeedSettingsPage(navController) }
        composable<Routing.WebLearnMore> { WebLearnMorePage(navController) }
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
            ChatTextPage(navController, r.content)
        }
        composable<Routing.ChatEditText> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.ChatEditText>()
            ChatEditTextPage(navController, r.id, r.content, chatVM)
        }
        composable<Routing.OtherFile> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.OtherFile>()
            OtherFilePage(navController, r.path, r.title)
        }
        composable<Routing.PdfViewer> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.PdfViewer>()
            PdfPage(navController, Uri.parse(r.uri))
        }
        composable<Routing.Files> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Files>()
            FilesPage(navController, audioPlaylistVM, r.folderPath, onOpenDrawer = onOpenDrawer)
        }
        composable<Routing.AppFiles> {
            AppFilesPage(navController)
        }
        composable<Routing.Nearby> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Nearby>()
            NearbyPage(navController, pairDeviceJson = r.pairDeviceJson)
        }
    }
}
