package com.ismartcoding.plain.ui.page

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewTextFileBottomSheet(
    viewModel: TextFileViewModel,
    path: String,
    m: DFile?,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onDismiss = {
        viewModel.showMoreActions.value = false
    }
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    LaunchedEffect(Unit) {
        groupButtons.addAll(
            listOf(
                GroupButton(
                    icon = Icons.Outlined.Share,
                    text = context.getString(R.string.share),
                    onClick = {
                        ShareHelper.sharePaths(context, setOf(path))
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.VerticalAlignTop,
                    text = context.getString(R.string.jump_to_top),
                    onClick = {
                        viewModel.gotoTop()
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.VerticalAlignBottom,
                    text = context.getString(R.string.jump_to_bottom),
                    onClick = {
                        viewModel.gotoEnd()
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.DeleteForever,
                    text = context.getString(R.string.delete),
                    onClick = {
                        DialogHelper.confirmToDelete {
                            scope.launch(Dispatchers.IO) {
                                val paths = mutableListOf(path)
                                paths.forEach {
                                    File(it).deleteRecursively()
                                }
                                MainApp.instance.scanFileByConnection(paths.toTypedArray())
                                onDismiss()
                                onDeleted()
                            }
                        }
                    }
                )
            ))
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        GroupButtons(
            buttons = groupButtons
        )
        VerticalSpace(dp = 24.dp)
        if (m != null) {
            PCard {
                PListItem(title = m.path, action = {
                    PIconButton(icon = Icons.Outlined.ContentCopy, contentDescription = stringResource(id = R.string.copy_path), onClick = {
                        val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.file_path), m.path)
                        clipboardManager.setPrimaryClip(clip)
                        DialogHelper.showTextCopiedMessage(m.path)
                    })
                })
            }
            VerticalSpace(dp = 16.dp)
            PCard {
                PListItem(title = stringResource(id = R.string.file_size), value = m.size.formatBytes())
                PListItem(title = stringResource(id = R.string.type), value = m.path.getMimeType())
                if (m.createdAt != null) {
                    PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
                }
                PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
            }
            VerticalSpace(dp = 16.dp)
            PCard {
                PListItem(title = stringResource(id = R.string.wrap_content), action = {
                    PSwitch(
                        activated = viewModel.wrapContent.value,
                    ) {
                        viewModel.toggleWrapContent(context)
                    }
                })
            }
        }
        BottomSpace()
    }
}


