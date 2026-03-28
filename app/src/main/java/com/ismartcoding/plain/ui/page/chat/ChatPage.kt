package com.ismartcoding.plain.ui.page.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.ChatInputTextPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatType
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.sendTextMessage
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    id: String = "",
) {
    val context = LocalContext.current
    val itemsState = chatVM.itemsFlow.collectAsState()
    val chatState = chatVM.chatState.collectAsState()
    val channelsState = channelVM.channels.collectAsState()
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val imageWidthDp = remember { (configuration.screenWidthDp.dp - 44.dp) / 3 }
    val imageWidthPx = remember(imageWidthDp) { derivedStateOf { density.run { imageWidthDp.toPx().toInt() } } }
    val refreshState = rememberRefreshLayoutState {
        scope.launch(Dispatchers.IO) {
            chatVM.fetchAsync(chatState.value.toId)
            setRefreshState(RefreshContentState.Finished)
        }
    }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val previewerState = rememberPreviewerState()

    ChatPageEffects(id, chatVM, peerVM, scrollState, focusManager, scope, onInputLoaded = { inputValue = it })

    BackHandler(enabled = chatVM.selectMode.value || previewerState.visible) {
        if (previewerState.visible) scope.launch { previewerState.closeTransform() }
        else chatVM.exitSelectMode()
    }

    val pageTitle = if (chatVM.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", chatVM.selectedIds.size)
    } else {
        val state = chatState.value
        if (state.chatType == ChatType.CHANNEL) {
            val channel = channelsState.value.find { it.id == state.toId }
            if (channel != null) "${state.toName} (${channel.joinedMembers().size})" else state.toName
        } else state.toName
    }

    val channel = if (chatState.value.chatType == ChatType.CHANNEL) channelsState.value.find { it.id == chatState.value.toId } else null

    PScaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            PTopAppBar(
                modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
                navController = navController,
                navigationIcon = {
                    if (chatVM.selectMode.value) NavigationCloseIcon { chatVM.exitSelectMode() }
                    else NavigationBackIcon { navController.navigateUp() }
                },
                title = pageTitle,
                actions = {
                    if (chatVM.selectMode.value) {
                        PTopRightButton(label = stringResource(if (chatVM.isAllSelected()) R.string.unselect_all else R.string.select_all), click = { chatVM.toggleSelectAll() })
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        PIconButton(icon = R.drawable.ellipsis, contentDescription = stringResource(R.string.more), click = { navController.navigate(Routing.ChatInfo(id.ifEmpty { "local" })) })
                    }
                },
            )
        },
        bottomBar = { AnimatedBottomAction(visible = chatVM.showBottomActions()) { ChatSelectModeBottomActions(chatVM) } },
    ) { paddingValues ->
        ChatPageContent(
            navController = navController, chatVM = chatVM, audioPlaylistVM = audioPlaylistVM,
            chatState = chatState.value, itemsState = itemsState.value, channelStatus = channel?.status,
            paddingValues = paddingValues, refreshState = refreshState, scrollState = scrollState,
            focusManager = focusManager, previewerState = previewerState,
            imageWidthDp = imageWidthDp, imageWidthPx = imageWidthPx.value,
            inputValue = inputValue,
            onInputChange = { inputValue = it; scope.launch(Dispatchers.IO) { ChatInputTextPreference.putAsync(context, it) } },
            onSend = {
                if (inputValue.isEmpty()) return@ChatPageContent
                scope.launch {
                    chatVM.sendTextMessage(inputValue, context)
                    inputValue = ""
                    withIO { ChatInputTextPreference.putAsync(context, "") }
                    scrollState.scrollToItem(0)
                }
            },
            peerVM = peerVM,
        )
    }

    MediaPreviewer(state = previewerState)
}
