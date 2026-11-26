package com.ismartcoding.plain.ui.home

import com.ismartcoding.plain.features.settings.AppUpdateDownloads

import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue

import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.features.web.WebHelper
import com.ismartcoding.plain.ui.settings.UpdateViewModel
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.lightbulb as ui_drawable_lightbulb

private const val GITHUB_RELEASES_URL = "https://github.com/plainhub/plain-app/releases/latest"

@Composable
fun UpdateBanner(updateVM: UpdateViewModel) {
    if (!AppFeatureType.CHECK_UPDATES.has()) return
    val updateInfo = Prefs.updateInfo.collectAsStateValue()
    val newVersion = updateInfo.newVersion
    val isDownloading = updateVM.isDownloading.value
    val downloadProgress = updateVM.downloadProgress.intValue
    val downloadFailed = updateVM.downloadFailed.value
    val isDownloadComplete = updateVM.isDownloadComplete.value

    val needsUpdate = Prefs.appUpdateAvailable.collectAsStateValue()

    when {
        isDownloading -> {
            DownloadProgressBanner(
                newVersion = newVersion.toString(),
                progress = downloadProgress,
                onCancel = {
                    AppUpdateDownloads.cancel()
                },
            )
            VerticalSpace(dp = 16.dp)
        }

        isDownloadComplete -> {
            DownloadCompleteBanner(
                onInstall = {
                    AppUpdateDownloads.install(MainViewModel.installAppUpdate)
                },
                onDismiss = {
                    AppUpdateDownloads.cancel()
                },
            )
            VerticalSpace(dp = 16.dp)
        }

        downloadFailed -> {
            DownloadFailedBanner(
                onGitHub = { WebHelper.open(GITHUB_RELEASES_URL) },
                onRetry = {
                    AppUpdateDownloads.start()
                },
                onDismiss = { AppUpdateDownloads.cancel() },
            )
            VerticalSpace(dp = 16.dp)
        }

        needsUpdate -> {
            PBanner(
                title = stringResource(Res.string.get_new_updates, newVersion.toString()),
                desc = stringResource(Res.string.get_new_updates_desc),
                icon = UiRes.drawable.ui_drawable_lightbulb,
            ) { updateVM.showDialog() }
            VerticalSpace(dp = 16.dp)
        }
    }
}

@Composable
private fun DownloadProgressBanner(
    newVersion: String,
    progress: Int,
    onCancel: () -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.downloading_update, progress),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(Res.string.cancel),
                        color = contentColor,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun DownloadCompleteBanner(
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    PBanner(
        title = stringResource(Res.string.update_downloaded),
        icon = UiRes.drawable.ui_drawable_lightbulb,
        action = {
            PFilledButton(
                text = stringResource(Res.string.install_update),
                buttonSize = ButtonSize.SMALL,
                onClick = onInstall,
            )
        },
        onClick = onDismiss,
    )
}

@Composable
private fun DownloadFailedBanner(
    onGitHub: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    PBanner(
        title = stringResource(Res.string.download_update_failed),
        backgroundColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        icon = UiRes.drawable.ui_drawable_lightbulb,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PFilledButton(
                    text = stringResource(Res.string.try_again),
                    buttonSize = ButtonSize.SMALL,
                    onClick = onRetry,
                )
                PFilledButton(
                    text = stringResource(Res.string.download_on_github),
                    buttonSize = ButtonSize.SMALL,
                    onClick = onGitHub,
                )
            }
        },
        onClick = onDismiss,
    )
}
