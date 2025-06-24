package com.ismartcoding.plain.ui.page.chat

import android.annotation.SuppressLint
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFilenameWithoutExtension
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.extensions.queryOpenableFile
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.extensions.getDuration
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.PeerChatHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.preferences.ChatInputTextPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.page.chat.components.ChatInput
import com.ismartcoding.plain.ui.page.chat.components.ChatListItem
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.File

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    chatVM: ChatViewModel,
    id: String = "",
) {
    val context = LocalContext.current
    val itemsState = chatVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var toName by remember { mutableStateOf("") }
    var peer by remember { mutableStateOf<DPeer?>(null) }

    // Parse chat type and real ID from the id parameter
    val (chatType, toId) = remember(id) {
        when {
            id.startsWith("peer:") -> "peer" to id.removePrefix("peer:")
            id.startsWith("group:") -> "group" to id.removePrefix("group:")
            else -> "local" to "local"
        }
    }

    val imageWidthDp = remember {
        (configuration.screenWidthDp.dp - 44.dp) / 3
    }
    val imageWidthPx = remember(imageWidthDp) {
        derivedStateOf {
            density.run { imageWidthDp.toPx().toInt() }
        }
    }
    val refreshState =
        rememberRefreshLayoutState {
            chatVM.fetch(context, toId)
            setRefreshState(RefreshContentState.Finished)
        }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val sharedFlow = Channel.sharedFlow
    val previewerState = rememberPreviewerState()

    val once = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!once.value) {
            once.value = true
            inputValue = ChatInputTextPreference.getAsync(context)
            chatVM.fetch(context, toId)
        }
    }

    // Load chat name based on chat type and id
    LaunchedEffect(chatType, toId) {
        withIO {
            when (chatType) {
                "peer" -> {
                    peer = AppDatabase.instance.peerDao().getById(toId)
                    toName = peer?.name ?: ""
                }

                "group" -> {
                    val group = AppDatabase.instance.chatGroupDao().getById(toId)
                    toName = group?.name ?: ""
                }
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is DeleteChatItemViewEvent -> {
                    chatVM.remove(event.id)
                }

                is HttpApiEvents.MessageCreatedEvent -> {
                    chatVM.addAll(event.items)
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                }

                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SEND_MESSAGE) {
                        return@collect
                    }
                    scope.launch {
                        DialogHelper.showLoading()
                        val items = mutableListOf<DMessageFile>()
                        withIO {
                            event.uris.forEach { uri ->
                                try {
                                    val file = context.contentResolver.queryOpenableFile(uri)
                                    if (file != null) {
                                        var fileName = file.displayName
                                        if (event.type == PickFileType.IMAGE_VIDEO) {
                                            val mimeType = context.contentResolver.getType(uri)
                                            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
                                            if (extension.isNotEmpty()) {
                                                fileName = fileName.getFilenameWithoutExtension() + "." + extension
                                            }
                                        }
                                        val size = file.size
                                        val dir =
                                            when {
                                                fileName.isVideoFast() -> {
                                                    Environment.DIRECTORY_MOVIES
                                                }

                                                fileName.isImageFast() -> {
                                                    Environment.DIRECTORY_PICTURES
                                                }

                                                fileName.isAudioFast() -> {
                                                    Environment.DIRECTORY_MUSIC
                                                }

                                                else -> {
                                                    Environment.DIRECTORY_DOCUMENTS
                                                }
                                            }
                                        var dst = context.getExternalFilesDir(dir)!!.path + "/$fileName"
                                        var dstFile = File(dst)
                                        if (dstFile.exists()) {
                                            dst = dstFile.newPath()
                                            dstFile = File(dst)
                                            FileHelper.copyFile(context, uri, dst)
                                        } else {
                                            FileHelper.copyFile(context, uri, dst)
                                        }
                                        val intrinsicSize = if (dst.isImageFast()) ImageHelper.getIntrinsicSize(
                                            dst,
                                            ImageHelper.getRotation(dst)
                                        ) else if (dst.isVideoFast()) VideoHelper.getIntrinsicSize(dst) else IntSize.Zero
                                        items.add(
                                            DMessageFile(
                                                StringHelper.shortUUID(),
                                                "app://$dir/${dst.getFilenameFromPath()}",
                                                size,
                                                dstFile.getDuration(context),
                                                intrinsicSize.width,
                                                intrinsicSize.height,
                                            )
                                        )
                                    }
                                } catch (ex: Exception) {
                                    // the picked file could be deleted
                                    DialogHelper.showMessage(ex)
                                    ex.printStackTrace()
                                }
                            }
                        }
                        val content =
                            if (event.type == PickFileType.IMAGE_VIDEO) {
                                DMessageContent(DMessageType.IMAGES.value, DMessageImages(items))
                            } else {
                                DMessageContent(
                                    DMessageType.FILES.value,
                                    DMessageFiles(items),
                                )
                            }
                        val item = withIO { ChatHelper.sendAsync(content, toId) }
                        DialogHelper.hideLoading()
                        chatVM.addAll(arrayListOf(item))
                        val m = item.toModel()
                        m.data = m.getContentData()
                        sendEvent(
                            WebSocketEvent(
                                EventType.MESSAGE_CREATED,
                                JsonHelper.jsonEncode(
                                    arrayListOf(
                                        m,
                                    ),
                                ),
                            ),
                        )
                        scrollState.scrollToItem(0)
                        delay(200)
                        focusManager.clearFocus()
                    }
                }
            }
        }
    }

    BackHandler(enabled = chatVM.selectMode.value || previewerState.visible) {
        if (previewerState.visible) {
            scope.launch {
                previewerState.closeTransform()
            }
        } else {
            chatVM.exitSelectMode()
        }
    }

    val pageTitle = if (chatVM.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", chatVM.selectedIds.size)
    } else {
        when (chatType) {
            "local" -> stringResource(id = R.string.local_chat)
            "group", "peer" -> {
                toName
            }

            else -> stringResource(id = R.string.chat)
        }
    }
    PScaffold(
        modifier = Modifier
            .imePadding(),
        topBar = {
            PTopAppBar(
                modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = {
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                }),
                navController = navController,
                navigationIcon = {
                    if (chatVM.selectMode.value) {
                        NavigationCloseIcon {
                            chatVM.exitSelectMode()
                        }
                    } else {
                        NavigationBackIcon {
                            navController.popBackStack()
                        }
                    }
                },
                title = pageTitle,
                actions = {
                    if (chatVM.selectMode.value) {
                        PTopRightButton(
                            label = stringResource(if (chatVM.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            click = {
                                chatVM.toggleSelectAll()
                            },
                        )
                        HorizontalSpace(dp = 8.dp)
                    }
                },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = chatVM.showBottomActions()) {
                ChatSelectModeBottomActions(chatVM)
            }
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            PullToRefresh(
                modifier = Modifier.weight(1f),
                refreshLayoutState = refreshState,
            ) {
                LazyColumnScrollbar(
                    state = scrollState,
                ) {
                    LazyColumn(
                        state = scrollState,
                        reverseLayout = true,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        item(key = "bottomSpace") {
                            VerticalSpace(dp = paddingValues.calculateBottomPadding())
                        }
                        itemsIndexed(itemsState.value, key = { _, a -> a.id }) { index, m ->
                            ChatListItem(
                                navController = navController,
                                chatVM = chatVM,
                                audioPlaylistVM,
                                itemsState.value,
                                m = m,
                                peer = peer,
                                index = index,
                                imageWidthDp = imageWidthDp,
                                imageWidthPx = imageWidthPx.value,
                                focusManager = focusManager,
                                previewerState = previewerState,
                            )
                        }

                    }
                }
            }
            if (!chatVM.showBottomActions()) {
                ChatInput(
                    value = inputValue,
                    hint = stringResource(id = R.string.chat_input_hint),
                    onValueChange = {
                        inputValue = it
                        scope.launch(Dispatchers.IO) {
                            ChatInputTextPreference.putAsync(context, inputValue)
                        }
                    },
                    onSend = {
                        if (inputValue.isEmpty()) {
                            return@ChatInput
                        }
                        scope.launch {
                            // Check if the message exceeds 1024 characters
                            if (inputValue.length > Constants.MAX_MESSAGE_LENGTH) {
                                // Create a temporary text file with the message content
                                val timestamp = Clock.System.now().toEpochMilliseconds()
                                val fileName = "message-$timestamp.txt"
                                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                                if (!dir!!.exists()) {
                                    dir.mkdirs()
                                }
                                val file = File(dir, fileName)
                                file.writeText(inputValue)

                                val summary = inputValue.cut(Constants.TEXT_FILE_SUMMARY_LENGTH)

                                // Create a message file item
                                val messageFile = DMessageFile(
                                    uri = file.absolutePath,
                                    size = file.length(),
                                    summary = summary
                                )

                                // Send the file as a message
                                val content = DMessageContent(
                                    DMessageType.FILES.value,
                                    DMessageFiles(listOf(messageFile))
                                )

                                val item = withIO { ChatHelper.sendAsync(content, fromId = "me", toId = toId) }
                                chatVM.addAll(arrayListOf(item))
                                val m = item.toModel()
                                m.data = m.getContentData()
                                sendEvent(
                                    WebSocketEvent(
                                        EventType.MESSAGE_CREATED,
                                        JsonHelper.jsonEncode(
                                            arrayListOf(
                                                m,
                                            ),
                                        ),
                                    ),
                                )

                                // Send message to peer if it's a peer chat
                                when (chatType) {
                                    "peer" -> {
                                        withIO {
                                            val success = PeerChatHelper.sendMessageToPeerAsync(toId, content)
                                            if (!success) {
                                                // Show error message on main thread when sending fails
                                                scope.launch {
                                                    DialogHelper.showMessage(
                                                        LocaleHelper.getString(R.string.failed_to_send_message_to_peer)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    "group" -> {
                                        // TODO: Implement group message sending
                                    }
                                }
                            } else {
                                // Send as normal text message
                                val messageContent = DMessageContent(DMessageType.TEXT.value, DMessageText(inputValue))
                                val item = withIO { ChatHelper.sendAsync(messageContent, fromId = "me", toId = toId) }
                                chatVM.addAll(arrayListOf(item))
                                val m = item.toModel()
                                m.data = m.getContentData()
                                sendEvent(
                                    WebSocketEvent(
                                        EventType.MESSAGE_CREATED,
                                        JsonHelper.jsonEncode(
                                            arrayListOf(
                                                m,
                                            ),
                                        ),
                                    ),
                                )
                                sendEvent(FetchLinkPreviewsEvent(item))
                                when (chatType) {
                                    "peer" -> {
                                        // Send message to peer
                                        withIO {
                                            val success = PeerChatHelper.sendMessageToPeerAsync(toId, messageContent)
                                            if (!success) {
                                                // Show error message on main thread when sending fails
                                                scope.launch {
                                                    DialogHelper.showMessage(
                                                        LocaleHelper.getString(R.string.failed_to_send_message_to_peer)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    "group" -> {
                                        // TODO: Implement group message sending
                                    }
                                }
                            }

                            // Reset input and scroll regardless of message type
                            inputValue = ""
                            withIO { ChatInputTextPreference.putAsync(context, inputValue) }
                            scrollState.scrollToItem(0)
                        }
                    },
                )
            }
        }
    }
    MediaPreviewer(state = previewerState)
}