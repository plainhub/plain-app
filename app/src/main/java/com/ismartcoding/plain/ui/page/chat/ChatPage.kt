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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.extensions.getDuration
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ChatFileSaveHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.preferences.ChatFilesSaveFolderPreference
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
    id: String = "",
) {
    val context = LocalContext.current
    val itemsState = chatVM.itemsFlow.collectAsState()
    val chatState = chatVM.chatState.collectAsState()
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf("") }
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
                                peer = chatState.value.peer,
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
    MediaPreviewer(state = previewerState)
}

private fun handleFileSelection(
    event: PickFileResultEvent,
    context: Context,
    chatVM: ChatViewModel,
    scrollState: LazyListState,
    focusManager: FocusManager
) {
    coMain {
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

                        val chatFilePath = ChatFileSaveHelper.generateChatFilePathAsync(context, fileName, "", event.type)

                        // Copy file to destination
                        FileHelper.copyFile(context, uri, chatFilePath.path)

                        val dstFile = File(chatFilePath.path)
                        val intrinsicSize = if (chatFilePath.path.isImageFast()) ImageHelper.getIntrinsicSize(
                            chatFilePath.path,
                            ImageHelper.getRotation(chatFilePath.path)
                        ) else if (chatFilePath.path.isVideoFast()) VideoHelper.getIntrinsicSize(chatFilePath.path) else IntSize.Zero
                        items.add(
                            DMessageFile(
                                StringHelper.shortUUID(),
                                chatFilePath.getFinalPath(),
                                size,
                                dstFile.getDuration(context),
                                intrinsicSize.width,
                                intrinsicSize.height,
                                "",
                                fileName
                            )
                        )
                    }
                } catch (ex: Exception) {
                    DialogHelper.showMessage(ex)
                    ex.printStackTrace()
                }
            }
        }

        DialogHelper.hideLoading()
        chatVM.sendFiles(items, event.type == PickFileType.IMAGE_VIDEO)
        scrollState.scrollToItem(0)
        delay(200)
        focusManager.clearFocus()
    }
}