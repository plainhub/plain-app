package com.ismartcoding.plain.ui.page.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getDuration
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFilenameWithoutExtension
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.extensions.newPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ChatListItem
import com.ismartcoding.plain.ui.components.chat.ChatInput
import com.ismartcoding.plain.ui.file.FilesDialog
import com.ismartcoding.plain.ui.file.FilesType
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.web.HttpServerEvents
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
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
    val imageWidthDp = (configuration.screenWidthDp.dp - 74.dp) / 3
    val imageWidthPx = with(density) { imageWidthDp.toPx().toInt() }
    val refreshState =
        rememberRefreshLayoutState {
            viewModel.fetch(context)
            setRefreshState(RefreshContentState.Finished)
        }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    LaunchedEffect(Unit) {
        viewModel.fetch(context)
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
                val items = mutableListOf<DMessageFile>()
                withIO {
                    val cache = mutableMapOf<String, Int>()
                    event.uris.forEach { uri ->
                        try {
                            context.contentResolver.query(uri, null, null, null, null)
                                ?.use { cursor ->
                                    try {
                                        cursor.moveToFirst()
                                        var fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME, cache)
                                        if (event.type == PickFileType.IMAGE_VIDEO) {
                                            val mimeType = context.contentResolver.getType(uri)
                                            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
                                            if (extension.isNotEmpty()) {
                                                fileName = fileName.getFilenameWithoutExtension() + "." + extension
                                            }
                                        }
                                        val size = cursor.getLongValue(OpenableColumns.SIZE, cache)
                                        cursor.close()
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
                                        val dstFile = File(dst)
                                        if (dstFile.exists()) {
                                            dst = dstFile.newPath()
                                            FileHelper.copyFile(context, uri, dst)
                                        } else {
                                            FileHelper.copyFile(context, uri, dst)
                                        }
                                        items.add(DMessageFile("app://$dir/${dst.getFilenameFromPath()}", size, dstFile.getDuration(context)))
                                    } catch (ex: Exception) {
                                        // the picked file could be deleted
                                        DialogHelper.showMessage(ex)
                                        ex.printStackTrace()
                                    }
                                }
                        } catch (ex: Exception) {
                            // the picked file could be deleted
                            LogCat.e(ex.toString())
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
                    val item = ChatHelper.sendAsync(content)
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
                    scope.launch {
                        scrollState.scrollToItem(0)
                        delay(200)
                        focusManager.clearFocus()
                    }
                }
            },
        )
    }

    val insetsController = WindowCompat.getInsetsController(window, view)
    LaunchedEffect(viewModel.selectMode.value) {
        if (viewModel.selectMode.value) {
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

    BackHandler(enabled = viewModel.selectMode.value) {
        viewModel.exitSelectMode()
    }

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else {
        stringResource(id = R.string.file_transfer_assistant)
    }
    PScaffold(
        navController,
        topBarTitle = pageTitle,
        topBarOnDoubleClick = {
            scope.launch {
                scrollState.scrollToItem(0)
            }
        },
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
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.selectMode.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                SelectModeBottomActions(viewModel)
            }
        },
        content = {
            Column(
                Modifier
                    .fillMaxHeight(),
            ) {
                PullToRefresh(
                    refreshLayoutState = refreshState,
                    modifier =
                    Modifier
                        .weight(1F),
                ) {
                    LazyColumn(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        state = scrollState,
                        reverseLayout = true,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        itemsIndexed(itemsState.value, key = { _, a -> a.id }) { index, m ->
                            ChatListItem(
                                navController = navController,
                                viewModel = viewModel,
                                sharedViewModel = sharedViewModel,
                                itemsState.value,
                                m = m,
                                index = index,
                                imageWidthDp = imageWidthDp,
                                imageWidthPx = imageWidthPx,
                                focusManager = focusManager
                            )
                        }
                    }
                }
                ChatInput(
                    value = inputValue,
                    modifier =
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    hint = stringResource(id = R.string.chat_input_hint),
                    onValueChange = { inputValue = it },
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
                            scrollState.scrollToItem(0)
                        }
                    },
                )
            }
        },
    )
}
