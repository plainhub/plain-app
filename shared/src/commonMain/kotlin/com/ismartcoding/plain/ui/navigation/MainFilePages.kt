package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import com.ismartcoding.plain.features.files.TextFileType
import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.features.chat.ChatQueries
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChatEditTextPage
import com.ismartcoding.plain.ui.chat.ChatTextPage
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.ui.chat.updateChatMessageTextAsync
import com.ismartcoding.plain.ui.files.FilesPage
import com.ismartcoding.plain.ui.files.ZipFilePage
import com.ismartcoding.plain.ui.files.documents.DocsPage
import com.ismartcoding.plain.ui.files.stored.AppFilesPage
import com.ismartcoding.plain.ui.media.OtherFilePage
import com.ismartcoding.plain.ui.media.PdfPage
import com.ismartcoding.plain.ui.media.PlayMediaPage
import com.ismartcoding.plain.ui.media.TextFilePage
import com.ismartcoding.plain.ui.media.TextPage
import com.ismartcoding.plain.ui.platform.files.printText
import com.ismartcoding.plain.ui.shares.EditSharePage
import com.ismartcoding.plain.ui.shares.browser.SharedFolderPage
import com.ismartcoding.plain.ui.shares.send.ShareImagePage

@androidx.compose.runtime.Composable
private fun contentAudioQueue(local: AudioQueueViewModel): AudioQueueViewModel {
    val source = LocalContentSource.current
    return if (source is ContentSource.Local) local
        else androidx.lifecycle.viewmodel.compose.viewModel(key = "file-audio-queue") { AudioQueueViewModel(source) }
}

internal fun NavGraphBuilder.addMainFilePages(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    source: @androidx.compose.runtime.Composable () -> ContentSource? = { ContentSource.Local },
) {
    contentDestination<Routing.Docs>(source = source) { DocsPage(navController, initialLoadGate = rememberNavLoadGate()) }
    contentDestination<Routing.Text>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.Text>()
        TextPage(navController, r.title, r.content, r.language)
    }
    composable<Routing.TextFile> { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.TextFile>()
        val fileSource = when (r.type) {
            TextFileType.APP_LOG.name, TextFileType.CRASH_REPORT.name -> ContentSource.Local
            else -> source()
        }
        fileSource?.let {
            CompositionLocalProvider(LocalContentSource provides it) {
                TextFilePage(navController, r.path, r.title, r.mediaId, r.type)
            }
        }
    }
    contentDestination<Routing.ChatText>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.ChatText>()
        ChatTextPage(navController, r.content, onPrint = { tm, jobName, content ->
            printText(tm, jobName, content)
        })
    }
    contentDestination<Routing.ChatEditText>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.ChatEditText>()
        val content = LocalContentSource.current
        ChatEditTextPage(navController, r.content, onSave = { text ->
            coIO {
                val originalChat = content.chats.getChatItem(r.id) ?: return@coIO
                com.ismartcoding.plain.features.chat.ChatMessageEditor.updateTextAsync(originalChat, text, content.api)
                if (content is ContentSource.Local) ChatViewModel.local.update(originalChat)
            }
        })
    }
    contentDestination<Routing.OtherFile>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.OtherFile>()
        OtherFilePage(navController, r.path, r.title)
    }
    contentDestination<Routing.ZipFile>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.ZipFile>()
        ZipFilePage(navController, contentAudioQueue(audioQueueVM), r.path, r.title)
    }
    contentDestination<Routing.PdfViewer>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.PdfViewer>()
        PdfPage(navController, r.uri, r.fileName)
    }
    contentDestination<Routing.Files>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.Files>()
        FilesPage(navController, contentAudioQueue(audioQueueVM), r.folderPath)
    }
    contentDestination<Routing.AppFiles>(source = source) {
        AppFilesPage(navController, contentAudioQueue(audioQueueVM))
    }
    contentDestination<Routing.EditShare>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.EditShare>()
        EditSharePage(navController, r.id)
    }
    contentDestination<Routing.SharedFolder>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.SharedFolder>()
        SharedFolderPage(navController, r.messageId)
    }
    contentDestination<Routing.PlayMedia>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.PlayMedia>()
        PlayMediaPage(navController, r.path, contentAudioQueue(audioQueueVM))
    }
    contentDestination<Routing.ShareImage>(source = source) { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.ShareImage>()
        ShareImagePage(navController, r.path, r.name)
    }
}
