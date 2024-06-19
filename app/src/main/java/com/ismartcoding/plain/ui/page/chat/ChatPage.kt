package com.ismartcoding.plain.ui.page.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.extensions.getDuration
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFilenameWithoutExtension
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.lib.extensions.queryOpenableFile
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.preference.ChatInputTextPreference
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ChatListItem
import com.ismartcoding.plain.ui.components.chat.ChatInput
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.file.FilesDialog
import com.ismartcoding.plain.ui.file.FilesType
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.web.HttpServerEvents
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    viewModel: ChatViewModel = viewModel(),
) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    val context = LocalContext.current
    val itemsState = viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val imageWidthDp = remember {
        (configuration.screenWidthDp.dp - 74.dp) / 3
    }
    val imageWidthPx = remember(imageWidthDp) {
        derivedStateOf {
            density.run { imageWidthDp.toPx().toInt() }
        }
    }
    val refreshState =
        rememberRefreshLayoutState {
            viewModel.fetch(context)
            setRefreshState(RefreshContentState.Finished)
        }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
    val previewerState = rememberPreviewerState()

    val once = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!once.value) {
            once.value = true
            inputValue = ChatInputTextPreference.getAsync(context)
            viewModel.fetch(context)
        }
        events.add(
            receiveEventHandler<DeleteChatItemViewEvent> { event ->
                viewModel.remove(event.id)
            },
        )

        events.add(
            receiveEventHandler<HttpServerEvents.MessageCreatedEvent> { event ->
                viewModel.addAll(event.items)
                scope.launch {
                    scrollState.scrollToItem(0)
                }
            },
        )

        events.add(
            receiveEventHandler<PickFileResultEvent> { event ->
                if (event.tag != PickFileTag.SEND_MESSAGE) {
                    return@receiveEventHandler
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
                    val item = withIO { ChatHelper.sendAsync(content) }
                    DialogHelper.hideLoading()
                    viewModel.addAll(arrayListOf(item))
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
                    scrollState.scrollToItem(0)
                    delay(200)
                    focusManager.clearFocus()
                }
            },
        )
    }

    val insetsController = WindowCompat.getInsetsController(window, view)
    LaunchedEffect(viewModel.selectMode.value, (previewerState.visible && !context.isGestureInteractionMode())) {
        if (viewModel.selectMode.value || (previewerState.visible && !context.isGestureInteractionMode())) {
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            events.forEach { it.cancel() }
            events.clear()
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    BackHandler(enabled = viewModel.selectMode.value || previewerState.visible) {
        if (previewerState.visible) {
            scope.launch {
                previewerState.closeTransform()
            }
        } else {
            viewModel.exitSelectMode()
        }
    }

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else {
        stringResource(id = R.string.file_transfer_assistant)
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
                    if (viewModel.selectMode.value) {
                        NavigationCloseIcon {
                            viewModel.exitSelectMode()
                        }
                    } else {
                        NavigationBackIcon {
                            navController.popBackStack()
                        }
                    }
                },
                title = pageTitle,
                actions = {
                    if (viewModel.selectMode.value) {
                        PMiniOutlineButton(
                            text = stringResource(if (viewModel.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            onClick = {
                                viewModel.toggleSelectAll()
                            },
                        )
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        PIconButton(
                            icon = Icons.Outlined.Folder,
                            contentDescription = stringResource(R.string.folder),
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                FilesDialog(FilesType.APP).show()
                            },
                        )
                    }
                },
            )
        },

        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                SelectModeBottomActions(viewModel)
            }
            if (!viewModel.showBottomActions()) {
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
                            val item = withIO { ChatHelper.sendAsync(DMessageContent(DMessageType.TEXT.value, DMessageText(inputValue))) }
                            viewModel.addAll(arrayListOf(item))
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
                            inputValue = ""
                            withIO { ChatInputTextPreference.putAsync(context, inputValue) }
                            scrollState.scrollToItem(0)
                        }
                    },
                )
            }
        },
        content = { paddingValues ->
            PullToRefresh(
                refreshLayoutState = refreshState,
            ) {
                LazyColumnScrollbar(
                    state = scrollState,
                ) {
                    LazyColumn(
                        modifier =
                        Modifier
                            .padding(bottom = paddingValues.calculateBottomPadding())
                            .fillMaxSize(),
                        state = scrollState,
                        reverseLayout = true,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        itemsIndexed(itemsState.value, key = { _, a -> a.id }) { index, m ->
                            ChatListItem(
                                navController = navController,
                                viewModel = viewModel,
                                itemsState.value,
                                m = m,
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

        },
    )
    MediaPreviewer(state = previewerState)
}
