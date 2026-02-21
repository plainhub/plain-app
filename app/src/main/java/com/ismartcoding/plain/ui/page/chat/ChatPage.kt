package com.ismartcoding.plain.ui.page.chat

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.getFilenameWithoutExtension
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.extensions.queryOpenableFile
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.extensions.getDuration
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ChatFileSaveHelper
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.preferences.ChatInputTextPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PIconButton
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
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.page.chat.components.ChatInput
import com.ismartcoding.plain.ui.page.chat.components.ChatListItem
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import com.ismartcoding.plain.ui.page.chat.components.ForwardTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    chatVM: ChatViewModel,
    chatListVM: ChatListViewModel,
    id: String = "",
) {
    val context = LocalContext.current
    val itemsState = chatVM.itemsFlow.collectAsState()
    val chatState = chatVM.chatState.collectAsState()
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf("") }
    var showForwardDialog by remember { mutableStateOf(false) }
    var showPeerInfoDialog by remember { mutableStateOf(false) }
    var messageToForward by remember { mutableStateOf<VChat?>(null) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

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
            scope.launch(Dispatchers.IO) {
                chatVM.fetchAsync(chatState.value.toId)
                setRefreshState(RefreshContentState.Finished)
            }
        }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val sharedFlow = Channel.sharedFlow
    val previewerState = rememberPreviewerState()

    LaunchedEffect(Unit) {
        inputValue = ChatInputTextPreference.getAsync(context)
        scope.launch(Dispatchers.IO) {
            chatVM.initializeChatStateAsync(id)
            chatVM.fetchAsync(chatVM.chatState.value.toId)
        }
        chatListVM.loadPeers()
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is DeleteChatItemViewEvent -> {
                    chatVM.remove(event.id)
                }

                is HttpApiEvents.MessageCreatedEvent -> {
                    if (chatVM.chatState.value.toId == event.fromId) {
                        chatVM.addAll(event.items)
                        scope.launch {
                            scrollState.scrollToItem(0)
                        }
                    }
                }

                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SEND_MESSAGE) {
                        return@collect
                    }
                    handleFileSelection(event, context, chatVM, scrollState, focusManager)
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
        chatState.value.toName
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
                        NavigationBackIcon { navController.navigateUp() }
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
                    } else if (chatState.value.peer != null) {
                        PIconButton(
                            icon = R.drawable.ellipsis,
                            contentDescription = stringResource(R.string.more),
                            click = {
                                showPeerInfoDialog = true
                            }
                        )
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
                                peer = chatState.value.peer,
                                index = index,
                                imageWidthDp = imageWidthDp,
                                imageWidthPx = imageWidthPx.value,
                                focusManager = focusManager,
                                previewerState = previewerState,
                                onForward = { message ->
                                    messageToForward = message
                                    showForwardDialog = true
                                }
                            )
                        }

                    }
                }
            }
            val peer = chatState.value.peer
            if (!chatVM.showBottomActions() && (peer == null || peer.status == "paired")) {
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
                        if (inputValue.isEmpty()) return@ChatInput

                        scope.launch {
                            chatVM.sendTextMessage(inputValue, context)
                            inputValue = ""
                            withIO { ChatInputTextPreference.putAsync(context, inputValue) }
                            scrollState.scrollToItem(0)
                        }
                    },
                )
            }
        }
    }

    if (showForwardDialog) {
        ForwardTargetDialog(
            chatListVM = chatListVM,
            onDismiss = {
                showForwardDialog = false
                messageToForward = null
            },
            onTargetSelected = { target ->
                messageToForward?.let { message ->
                    when (target) {
                        is ForwardTarget.Local -> {
                            chatVM.forwardMessageToLocal(message.id) { success ->
                                DialogHelper.showSuccess(R.string.sent)
                            }
                        }
                        is ForwardTarget.Peer -> {
                            chatVM.forwardMessage(message.id, target.peer) { success ->
                                DialogHelper.showSuccess(R.string.sent)
                            }
                        }
                    }
                }
            }
        )
    }

    MediaPreviewer(state = previewerState)

    if (showPeerInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPeerInfoDialog = false },
            title = { Text(chatState.value.peer?.name ?: "") },
            text = {
                Column {
                    chatState.value.peer?.let { peer ->
                        PDialogListItem(
                            title = stringResource(R.string.ip_address),
                            subtitle = peer.ip,
                        )
                        PDialogListItem(
                            title = stringResource(R.string.port),
                            subtitle = peer.port.toString(),
                        )
                        PDialogListItem(
                            title = stringResource(R.string.device_type),
                            subtitle = DeviceType.fromValue(peer.deviceType).getText(),
                        )
                        PDialogListItem(
                            title = stringResource(R.string.status),
                            subtitle = peer.getStatusText(),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPeerInfoDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }
}

private fun handleFileSelection(
    event: PickFileResultEvent,
    context: Context,
    chatVM: ChatViewModel,
    scrollState: LazyListState,
    focusManager: FocusManager
) {
    coMain {
        // --- 1. Build placeholder DMessageFile list from original content:// URIs
        //        so Coil can immediately render thumbnails.
        val placeholderItems = mutableListOf<DMessageFile>()
        event.uris.forEach { uri ->
            val file = context.contentResolver.queryOpenableFile(uri)
            if (file != null) {
                var fileName = file.displayName
                val mimeType = context.contentResolver.getType(uri) ?: ""
                if (event.type == PickFileType.IMAGE_VIDEO) {
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
                    if (extension.isNotEmpty()) {
                        fileName = fileName.getFilenameWithoutExtension() + "." + extension
                    }
                }
                placeholderItems.add(
                    DMessageFile(
                        id = StringHelper.shortUUID(),
                        uri = uri.toString(), // content:// URI for immediate preview
                        size = file.size,
                        fileName = fileName,
                    )
                )
            }
        }

        if (placeholderItems.isEmpty()) return@coMain

        // --- 2. Insert message immediately with status="pending"
        val isImageVideo = event.type == PickFileType.IMAGE_VIDEO
        val messageId = withIO { chatVM.sendFilesImmediate(placeholderItems, isImageVideo) }
        scrollToLatestAfterFileSend(chatVM, scrollState, null)
        delay(200)
        focusManager.clearFocus()

        // --- 3. Import files in background, then update the message with real fid: URIs
        withIO {
            val finalItems = mutableListOf<DMessageFile>()
            event.uris.forEachIndexed { index, uri ->
                try {
                    val placeholder = placeholderItems[index]
                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    val fidUri = ChatFileSaveHelper.importFromUri(context, uri, mimeType)
                    val realPath = AppFileStore.resolveUri(context, fidUri)
                    val intrinsicSize = if (placeholder.fileName.isImageFast())
                        ImageHelper.getIntrinsicSize(realPath, ImageHelper.getRotation(realPath))
                    else if (placeholder.fileName.isVideoFast())
                        VideoHelper.getIntrinsicSize(realPath)
                    else IntSize.Zero
                    finalItems.add(
                        DMessageFile(
                            id = placeholder.id,
                            uri = fidUri,
                            size = placeholder.size,
                            duration = java.io.File(realPath).getDuration(context),
                            width = intrinsicSize.width,
                            height = intrinsicSize.height,
                            summary = placeholder.summary,
                            fileName = placeholder.fileName,
                        )
                    )
                } catch (ex: Exception) {
                    DialogHelper.showMessage(ex)
                    ex.printStackTrace()
                    finalItems.add(placeholderItems[index])
                }
            }
            chatVM.updateFilesMessage(messageId, finalItems, isImageVideo)
        }
    }
}

private suspend fun scrollToLatestAfterFileSend(
    chatVM: ChatViewModel,
    scrollState: LazyListState,
    previousTopMessageId: String?
) {
    repeat(20) {
        val currentTopMessageId = chatVM.itemsFlow.value.firstOrNull()?.id
        if (currentTopMessageId != null && currentTopMessageId != previousTopMessageId) {
            scrollState.scrollToItem(0)
            return
        }
        delay(50)
    }

    // Fallback when list update is delayed or no new item is created.
    scrollState.scrollToItem(0)
}
