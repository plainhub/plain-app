package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.content.Context
import android.os.Environment
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.ValidateHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.ImageMediaStoreHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.theme.darkMask
import com.ismartcoding.plain.ui.theme.lightMask
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ImagePreviewActions(context: Context, uiAlpha: Float, item: PreviewItem, getViewerState: () -> MediaViewerState?, state: MediaPreviewerState) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 48.dp)
            .alpha(uiAlpha)
    ) {
        if (!state.showActions) {
            return
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.darkMask())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ActionIconButton(
                icon = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.share),
            ) {
                if (item.mediaId.isNotEmpty()) {
                    ShareHelper.shareUris(context, listOf(ImageMediaStoreHelper.getItemUri(item.mediaId)))
                } else if (ValidateHelper.isUrl(item.path)) {
                    scope.launch {
                        val cachedPath = context.imageLoader
                            .diskCache?.openSnapshot(item.path)?.data
                        val tempFile = File.createTempFile("imagePreviewShare", "." + item.path.getFilenameExtension(), File(context.cacheDir, "/image_cache"))
                        if (cachedPath != null) {
                            cachedPath.toFile().copyTo(tempFile, true)
                            ShareHelper.shareFile(context, tempFile)
                        } else {
                            DialogHelper.showLoading()
                            val r = withIO { DownloadHelper.downloadToTempAsync(item.path, tempFile) }
                            DialogHelper.hideLoading()
                            if (r.success) {
                                ShareHelper.shareFile(context, File(r.path))
                            } else {
                                DialogHelper.showMessage(r.message)
                            }
                        }
                    }
                } else {
                    ShareHelper.shareFile(context, File(item.path))
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = Icons.Rounded.Cast,
                contentDescription = stringResource(R.string.cast),
            ) {

            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = Icons.AutoMirrored.Rounded.RotateRight,
                contentDescription = stringResource(R.string.rotate),
            ) {
                scope.launch {
                    getViewerState()?.let {
                        it.rotation.animateTo(it.rotation.value + 90, SpringSpec())
                    }
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = Icons.Rounded.SaveAlt,
                contentDescription = stringResource(R.string.save),
            ) {
                scope.launch {
                    if (ValidateHelper.isUrl(item.path)) {
                        DialogHelper.showLoading()
                        val cachedPath = context.imageLoader
                            .diskCache?.openSnapshot(item.path)?.data
                        if (cachedPath != null) {
                            val r = withIO { FileHelper.copyFileToPublicDir(cachedPath.toString(), Environment.DIRECTORY_PICTURES, newName = item.path.getFilenameFromPath()) }
                            DialogHelper.hideLoading()
                            if (r.isNotEmpty()) {
                                DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r))
                            } else {
                                DialogHelper.showMessage(LocaleHelper.getString(R.string.image_save_to_failed))
                            }
                            return@launch
                        }
                        val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_PICTURES)
                        val r = withIO { DownloadHelper.downloadAsync(item.path, dir.absolutePath) }
                        DialogHelper.hideLoading()
                        if (r.success) {
                            DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r.path))
                        } else {
                            DialogHelper.showMessage(r.message)
                        }
                    } else {
                        val r = withIO { FileHelper.copyFileToPublicDir(item.path, Environment.DIRECTORY_PICTURES) }
                        if (r.isNotEmpty()) {
                            DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r))
                        } else {
                            DialogHelper.showMessage(LocaleHelper.getString(R.string.image_save_to_failed))
                        }
                    }
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = Icons.Outlined.MoreHoriz,
                contentDescription = stringResource(R.string.more_info),
            ) {
                state.showMediaInfo = true
            }
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, contentDescription: String, click: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.lightMask())
            .clickable {
                click()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}