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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PMiniButton
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.components.CastDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.theme.darkMask
import com.ismartcoding.plain.ui.theme.lightMask
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ImagePreviewActions(
    context: Context, castViewModel: CastViewModel,
    m: PreviewItem, state: MediaPreviewerState
) {
    val scope = rememberCoroutineScope()

    CastDialog(castViewModel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .alpha(state.uiAlpha.value)
    ) {
        if (!state.showActions) {
            return
        }
        if (castViewModel.castMode.value) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.darkMask())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                PMiniButton(label = stringResource(id = R.string.cast)) {
                    castViewModel.cast(m.path)
                }
                HorizontalSpace(dp = 20.dp)
                PMiniOutlineButton(label = stringResource(id = R.string.exit_cast_mode), color = Color.LightGray) {
                    castViewModel.exitCastMode()
                }
            }
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
                icon = R.drawable.share_2,
                contentDescription = stringResource(R.string.share),
            ) {
                if (m.mediaId.isNotEmpty()) {
                    ShareHelper.shareUris(context, listOf(ImageMediaStoreHelper.getItemUri(m.mediaId)))
                } else if (m.path.isUrl()) {
                    scope.launch {
                        val cachedPath = context.imageLoader
                            .diskCache?.openSnapshot(m.path)?.data
                        val tempFile = File.createTempFile("imagePreviewShare", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/image_cache"))
                        if (cachedPath != null) {
                            cachedPath.toFile().copyTo(tempFile, true)
                            ShareHelper.shareFile(context, tempFile)
                        } else {
                            DialogHelper.showLoading()
                            val r = withIO { DownloadHelper.downloadToTempAsync(m.path, tempFile) }
                            DialogHelper.hideLoading()
                            if (r.success) {
                                ShareHelper.shareFile(context, File(r.path))
                            } else {
                                DialogHelper.showMessage(r.message)
                            }
                        }
                    }
                } else {
                    ShareHelper.shareFile(context, File(m.path))
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = R.drawable.cast,
                contentDescription = stringResource(R.string.cast),
            ) {
                castViewModel.showCastDialog.value = true
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = R.drawable.rotate_cw_square,
                contentDescription = stringResource(R.string.rotate),
            ) {
                scope.launch {
                    state.viewerContainerState?.viewerState?.let {
                        it.rotation.animateTo(it.rotation.value + 90, SpringSpec())
                    }
                }
            }
            if (m.data !is DImage && m.data !is DFile) {
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(
                    icon = R.drawable.save,
                    contentDescription = stringResource(R.string.save),
                ) {
                    scope.launch {
                        if (m.path.isUrl()) {
                            DialogHelper.showLoading()
                            val cachedPath = context.imageLoader
                                .diskCache?.openSnapshot(m.path)?.data
                            if (cachedPath != null) {
                                val r = withIO { FileHelper.copyFileToPublicDir(cachedPath.toString(), Environment.DIRECTORY_PICTURES, newName = m.path.getFilenameFromPath()) }
                                DialogHelper.hideLoading()
                                if (r.isNotEmpty()) {
                                    DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r))
                                } else {
                                    DialogHelper.showMessage(LocaleHelper.getString(R.string.image_save_to_failed))
                                }
                                return@launch
                            }
                            val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_PICTURES)
                            val r = withIO { DownloadHelper.downloadAsync(m.path, dir.absolutePath) }
                            DialogHelper.hideLoading()
                            if (r.success) {
                                DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r.path))
                            } else {
                                DialogHelper.showMessage(r.message)
                            }
                        } else {
                            val r = withIO { FileHelper.copyFileToPublicDir(m.path, Environment.DIRECTORY_PICTURES) }
                            if (r.isNotEmpty()) {
                                DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r))
                            } else {
                                DialogHelper.showMessage(LocaleHelper.getString(R.string.image_save_to_failed))
                            }
                        }
                    }
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(
                icon = R.drawable.ellipsis,
                contentDescription = stringResource(R.string.more_info),
            ) {
                state.showMediaInfo = true
            }
        }
    }
}

@Composable
fun ActionIconButton(icon: Int, contentDescription: String, click: () -> Unit) {
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
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}