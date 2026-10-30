package com.ismartcoding.plain.ui.chat


import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.features.chat.ChatChannelStatus
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.components.ActionButtonMore
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.components.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.chat.VChat
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.state.exitSelectMode
import com.ismartcoding.plain.ui.state.isAllSelected
import com.ismartcoding.plain.ui.state.showBottomActions
import com.ismartcoding.plain.ui.state.toggleSelectAll
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.chat.components.ChatInput
import com.ismartcoding.plain.ui.chat.components.ChatListItem
import com.ismartcoding.plain.ui.chat.components.TransportIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    id: String = "",
) {
    val remote = chatVM.source as? com.ismartcoding.plain.rust.graphql.ContentSource.Remote
    val filesAllowed = remote == null || com.ismartcoding.plain.platform.permissions.Permission.WRITE_EXTERNAL_STORAGE in remote.client.state.collectAsState().value.permissions
    val itemsState = chatVM.itemsFlow.collectAsState()
    val chatTarget = chatVM.target.collectAsState()
    val channelsState = channelVM.channels.collectAsState()
    val peers by peerVM.peersMap.collectAsState()
    val scope = rememberCoroutineScope()
    val requestFilePermission = {
        scope.launchSafe { chatVM.source.api.execute(com.ismartcoding.plain.graphql.Mutation.openWebSettings(com.ismartcoding.plain.features.web.WebSettingsFeature.FILES)) }
        Unit
    }
    var inputValue by remember { mutableStateOf("") }
    var showForwardDialog by remember { mutableStateOf(false) }
    var messageToForward by remember { mutableStateOf<VChat?>(null) }
    var showFolderShareSheet by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val imageWidthDp = remember(windowInfo.containerSize.width) {
        with(density) { (windowInfo.containerSize.width.toDp() - 44.dp) / 3 }
    }
    val imageWidthPx = remember(imageWidthDp) { derivedStateOf { density.run { imageWidthDp.toPx().toInt() } } }
    val refreshState = rememberRefreshLayoutState {
        scope.launch(Dispatchers.Default) {
            chatVM.fetchAsync(chatTarget.value.toId)
            setRefreshState(RefreshContentState.Finished)
        }
    }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val previewerState = rememberPreviewerState()

    LaunchedEffect(id) {
        inputValue = chatVM.client.readDraft(id)
    }

    ChatPageEffects(chatVM, peerVM, channelVM, id, chatTarget.value.type, focusManager, scrollState)

    PBackHandler(enabled = chatVM.selectMode.value || previewerState.visible) {
        if (previewerState.visible) scope.launch { previewerState.closeTransform() }
        else chatVM.exitSelectMode()
    }

    val pageTitle = if (chatVM.selectMode.value) {
        LocaleHelper.getStringF(Res.string.x_selected, chatVM.selectedIds.size)
    } else {
        val target = chatTarget.value
        if (target.type == ChatTargetType.CHANNEL) {
            val channel = channelsState.value.find { it.id == target.toId }
            if (channel != null) "${channel.name} (${channel.joinedMembers().size})" else ""
        } else if (target.isLocal()) {
            LocaleHelper.getString(Res.string.local_chat)
        } else {
            peers[target.toId]?.name ?: ""
        }
    }

    val channel = if (chatTarget.value.type == ChatTargetType.CHANNEL) channelsState.value.find { it.id == chatTarget.value.toId } else null

    // Transport badge: only visible while a send is in flight. Reflects the
    // transport the router is currently attempting (LAN/AWARE/BLE) in real
    // time; disappears once the send succeeds or all transports are exhausted.
    val currentTransportMap = chatVM.currentTransportMap.collectAsState()
    val transportType = if (chatTarget.value.type == ChatTargetType.PEER && !chatTarget.value.isLocal()) {
        currentTransportMap.value[chatTarget.value.toId]
    } else null

    androidx.compose.runtime.CompositionLocalProvider(LocalChatDownloads provides chatVM.downloads) {
    PScaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            PTopAppBar(
                modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
                onNavigateBack = { navController.navigateUp() },
                navigationIcon = {
                    if (chatVM.selectMode.value) NavigationCloseIcon { chatVM.exitSelectMode() }
                    else NavigationBackIcon { navController.navigateUp() }
                },
                title = pageTitle,
                subtitle = remote?.info?.deviceName.orEmpty(),
                titleTrailing = if (!chatVM.selectMode.value && transportType != null) {
                    { TransportIcon(transportType) }
                } else null,
                actions = {
                    if (chatVM.selectMode.value) {
                        PTopRightButton(label = stringResource(if (chatVM.isAllSelected()) Res.string.unselect_all else Res.string.select_all), click = { chatVM.toggleSelectAll() })
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        ActionButtonMore(onClick = {
                            navController.navigate(Routing.ChatInfo(id))
                        })
                    }
                },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = chatVM.showBottomActions()) {
                PBottomAppBar {
                    BottomActionButtons {
                        IconTextSmallButtonDelete {
                            chatVM.delete(chatVM.selectedIds.toSet())
                            chatVM.exitSelectMode()
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            PullToRefresh(modifier = Modifier.weight(1f), refreshLayoutState = refreshState) {
                LazyColumnScrollbar(state = scrollState, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = scrollState, reverseLayout = true, verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize()) {
                        item(key = "bottomSpace") { VerticalSpace(dp = paddingValues.calculateBottomPadding()) }
                        itemsIndexed(itemsState.value, key = { _, a -> a.id }) { index, m ->
                            ChatListItem(
                                navController = navController,
                                chatVM = chatVM,
                                filesAllowed = filesAllowed,
                                onRequestFilePermission = requestFilePermission,
                                chatTarget = chatTarget.value,
                                audioQueueVM = audioQueueVM,
                                items = itemsState.value,
                                m = m,
                                peer = (if (chatTarget.value.type == ChatTargetType.PEER) peers[chatTarget.value.toId] else null)
                                    ?: peers[m.fromId],
                                index = index,
                                imageWidthDp = imageWidthDp,
                                imageWidthPx = imageWidthPx.value,
                                focusManager = focusManager,
                                previewerState = previewerState,
                                onForward = { message ->
                                    messageToForward = message
                                    showForwardDialog = true
                                },
                            )
                        }
                    }
                }
            }
            val peer = if (chatTarget.value.type == ChatTargetType.PEER) peers[chatTarget.value.toId] else null
            val notAllowChat = (channel != null && !channel.isJoined()) || peer?.status == PeerStatus.UNPAIRED
            if (notAllowChat) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            if (peer?.status == PeerStatus.UNPAIRED)
                                Res.string.unpaired
                            else if (channel?.status == ChatChannelStatus.KICKED)
                                Res.string.channel_kicked_notice
                            else
                                Res.string.channel_left_notice
                        ),
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            } else if (!chatVM.showBottomActions() && (peer == null || peer.status == PeerStatus.PAIRED)) {
                ChatInput(
                    filesAllowed = filesAllowed,
                    onRequestFilePermission = requestFilePermission,
                    value = inputValue,
                    hint = stringResource(Res.string.chat_input_hint),
                    onValueChange = {
                        inputValue = it
                        scope.launch(Dispatchers.Default) {
                            chatVM.client.writeDraft(id, it)
                        }
                    },
                    onSend = {
                        if (inputValue.isEmpty()) return@ChatInput
                        scope.launch {
                            chatVM.sendTextMessage(inputValue)
                            inputValue = ""
                            chatVM.client.writeDraft(id, "")
                        }
                    },
                    onShareFolder = { showFolderShareSheet = true })
            }
        }
    }

    MediaPreviewer(state = previewerState)

    ChatPageDialogs(
        showFolderShare = showFolderShareSheet,
        target = chatTarget.value,
        onDismissFolder = { showFolderShareSheet = false },
        forwardMessage = messageToForward.takeIf { showForwardDialog },
        onDismissForward = { showForwardDialog = false; messageToForward = null },
        onForward = { message, targets -> chatVM.forwardMessage(message.id, targets) },
    )
    }
}
