package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import android.provider.DocumentsContract
import android.os.Environment
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.preferences.ChatFilesSaveFolderPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.launch

private fun convertDocumentTreeUriToPath(uri: Uri): String? {
    return try {
        if (uri.authority != "com.android.externalstorage.documents") {
            return null
        }

        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val parts = documentId.split(":")

        if (parts.size >= 2) {
            val type = parts[0]
            val path = parts[1]

            when (type) {
                "primary" -> {
                    val primaryStorage = Environment.getExternalStorageDirectory().absolutePath
                    if (path.isEmpty()) {
                        primaryStorage
                    } else {
                        "$primaryStorage/$path"
                    }
                }

                else -> {
                    // Handle secondary storage if needed
                    null
                }
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsPage(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chatFilesSaveFolder by remember { mutableStateOf("") }
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(Unit) {
        scope.launch {
            withIO {
                chatFilesSaveFolder = ChatFilesSaveFolderPreference.getAsync(context)
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.CHAT_FILES_SAVE_FOLDER) {
                        return@collect
                    }
                    if (event.uris.isNotEmpty()) {
                        val uri = event.uris.first()
                        try {
                            // Convert document tree URI to actual file system path
                            val actualPath = convertDocumentTreeUriToPath(uri)
                            if (actualPath != null) {
                                // Save the actual directory path
                                scope.launch {
                                    withIO {
                                        ChatFilesSaveFolderPreference.putAsync(context, actualPath)
                                        chatFilesSaveFolder = actualPath
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(R.string.chat_settings)
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                item {
                    PCard {
                        PListItem(
                            modifier = Modifier.clickable {
                                sendEvent(PickFileEvent(PickFileTag.CHAT_FILES_SAVE_FOLDER, PickFileType.FOLDER, false))
                            },
                            title = stringResource(R.string.chat_files_save_directory),
                            subtitle = chatFilesSaveFolder.ifEmpty {
                                stringResource(R.string.default_app_directory)
                            },
                            icon = R.drawable.folder,
                            showMore = true,
                        )
                    }
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    BottomSpace(paddingValues)
                }
            }
        }
    )
} 