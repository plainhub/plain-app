package com.ismartcoding.plain.ui.page

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.UpdateMessageEvent
import com.ismartcoding.plain.features.chat.ChatHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.chat.ChatDate
import com.ismartcoding.plain.ui.components.chat.ChatFiles
import com.ismartcoding.plain.ui.components.chat.ChatImages
import com.ismartcoding.plain.ui.components.chat.ChatInput
import com.ismartcoding.plain.ui.components.chat.ChatName
import com.ismartcoding.plain.ui.components.chat.ChatText
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.web.HttpServerEvents
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatPage(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
    viewModel: ChatViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState = viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val imageWidthDp = (configuration.screenWidthDp.dp - 34.dp) / 3
    val imageWidthPx = with(density) { imageWidthDp.toPx().toInt() }
    val refreshState = rememberRefreshLayoutState {
        viewModel.fetch(context)
        setRefreshState(RefreshContentState.Stop)
    }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
    var selectedItem by remember { mutableStateOf<VChat?>(null) }
    val showContextMenu = remember { mutableStateOf(false) }
    var showEditTextSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetch(context)
        events.add(receiveEventHandler<DeleteChatItemViewEvent> { event ->
            viewModel.remove(event.id)
        })

        events.add(receiveEventHandler<HttpServerEvents.MessageCreatedEvent> { event ->
            viewModel.addAll(event.items)
            scope.launch {
                scrollState.scrollToItem(0)
            }
        })

        events.add(receiveEventHandler<UpdateMessageEvent> { event ->
            scope.launch {
                val update = ChatItemDataUpdate(event.id, DMessageContent(DMessageType.TEXT.value, DMessageText(event.content)))
                withIO {
                    AppDatabase.instance.chatDao().updateData(update)
                }
                val c = withIO { AppDatabase.instance.chatDao().getById(event.id) }
                if (c != null) {
                    viewModel.update(c)
                    sendEvent(WebSocketEvent(EventType.MESSAGE_UPDATED, JsonHelper.jsonEncode(listOf(c.toModel().apply {
                        data = this.getContentData()
                    }))))
                }
                focusManager.clearFocus()
            }
        })

        events.add(receiveEventHandler<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.SEND_MESSAGE) {
                return@receiveEventHandler
            }
            val items = mutableListOf<DMessageFile>()
            withIO {
                event.uris.forEach { uri ->
                    context.contentResolver.query(uri, null, null, null, null)
                        ?.use { cursor ->
                            try {
                                cursor.moveToFirst()
                                var fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
                                if (event.type == PickFileType.IMAGE_VIDEO) {
                                    val mimeType = context.contentResolver.getType(uri)
                                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
                                    if (extension.isNotEmpty()) {
                                        fileName = fileName.getFilenameWithoutExtension() + "." + extension
                                    }
                                }
                                val size = cursor.getLongValue(OpenableColumns.SIZE)
                                cursor.close()
                                val dir = when {
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
                }
                val content = if (event.type == PickFileType.IMAGE_VIDEO) DMessageContent(DMessageType.IMAGES.value, DMessageImages(items)) else DMessageContent(
                    DMessageType.FILES.value,
                    DMessageFiles(items)
                )
                val item = ChatHelper.sendAsync(content)
                viewModel.addAll(arrayListOf(item))
                sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(arrayListOf(item.toModel().apply {
                    data = this.getContentData()
                }))))
                scope.launch {
                    scrollState.scrollToItem(0)
                    delay(200)
                    focusManager.clearFocus()
                }
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            events.forEach { it.cancel() }
        }
    }

    if (showEditTextSheet) {
        EditTextBottomSheet(selectedItem?.id ?: "", (selectedItem?.value as? DMessageText)?.text ?: "") {
            showEditTextSheet = false
        }
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.my_phone),
        content = {
            Column(
                Modifier
                    .fillMaxHeight()
            ) {
                PullToRefresh(
                    refreshLayoutState = refreshState, modifier = Modifier
                        .weight(1F)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        state = scrollState,
                        reverseLayout = true,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        itemsIndexed(itemsState.value, key = { _, a -> a.id }) { index, m ->
                            Column(modifier = Modifier.fillMaxSize()) {
                                ChatDate(itemsState.value, m, index)
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .combinedClickable(
                                                onClick = {
                                                    focusManager.clearFocus()
                                                },
                                                onDoubleClick = {
                                                    if (m.value is DMessageText) {
                                                        val content = (m.value as DMessageText).text
                                                        sharedViewModel.chatContent.value = content
                                                        navController.navigate(RouteName.CHAT_TEXT)
                                                    }
                                                },
                                                onLongClick = {
                                                    selectedItem = m
                                                    showContextMenu.value = true
                                                })
                                    ) {
                                        ChatName(m)
                                        when (m.type) {
                                            DMessageType.IMAGES.value -> {
                                                ChatImages(context, m, imageWidthDp, imageWidthPx)
                                            }

                                            DMessageType.FILES.value -> {
                                                ChatFiles(context, m)
                                            }

                                            DMessageType.TEXT.value -> {
                                                ChatText(context, m)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 32.dp)
                                            .wrapContentSize(Alignment.Center)
                                    ) {
                                        PDropdownMenu(
                                            expanded = showContextMenu.value && selectedItem == m,
                                            onDismissRequest = { showContextMenu.value = false }
                                        ) {
                                            if (m.value is DMessageText) {
                                                DropdownMenuItem(text = { Text(stringResource(id = R.string.copy_text)) },
                                                    onClick = {
                                                        showContextMenu.value = false
                                                        val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.message), (m.value as DMessageText).text)
                                                        clipboardManager.setPrimaryClip(clip)
                                                        DialogHelper.showMessage(R.string.copied)
                                                    })
                                                DropdownMenuItem(text = { Text(stringResource(id = R.string.edit_text)) },
                                                    onClick = {
                                                        showContextMenu.value = false
                                                        showEditTextSheet = true
                                                    })
                                            }
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.delete)) },
                                                onClick = {
                                                    showContextMenu.value = false
                                                    scope.launch {
                                                        ChatHelper.deleteAsync(context, m.id, m.value)
                                                        val json = JSONArray()
                                                        json.put(m.id)
                                                        sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, json.toString()))
                                                    }
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ChatInput(
                    value = inputValue,
                    modifier = Modifier
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
                            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(arrayListOf(item.toModel().apply {
                                data = this.getContentData()
                            }))))
                            inputValue = ""
                            scrollState.scrollToItem(0)
                        }
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTextBottomSheet(id: String, value: String, onDismiss: () -> Unit) {
    var inputValue by remember { mutableStateOf(value) }
    PModalBottomSheet(
        topBarTitle = stringResource(id = R.string.edit_text),
        onDismissRequest = { onDismiss() },
    ) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                )
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default,
            shape = RoundedCornerShape(8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            PIconButton(
                imageVector = Icons.Outlined.Send,
                contentDescription = stringResource(R.string.send_message),
                tint = MaterialTheme.colorScheme.primary
            ) {
                if (inputValue.isNotEmpty()) {
                    onDismiss()
                    sendEvent(UpdateMessageEvent(id, inputValue))
                }
            }
        }
    }
}