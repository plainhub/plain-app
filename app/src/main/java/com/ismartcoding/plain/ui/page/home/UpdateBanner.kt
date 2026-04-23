package com.ismartcoding.plain.ui.page.home

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.data.toVersion
import com.ismartcoding.plain.events.CancelUpdateDownloadEvent
import com.ismartcoding.plain.events.DownloadUpdateEvent
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.preferences.LocalNewVersion
import com.ismartcoding.plain.preferences.LocalSkipVersion
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.UpdateViewModel
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.launch

private const val GITHUB_RELEASES_URL = "https://github.com/plainhub/plain-app/releases/latest"

@Composable
fun UpdateBanner(updateVM: UpdateViewModel) {
    val newVersion = LocalNewVersion.current.toVersion()
    val skipVersion = LocalSkipVersion.current.toVersion()
    val currentVersion = Version(BuildConfig.VERSION_NAME)
    val isDownloading = updateVM.isDownloading.value
    val downloadProgress = updateVM.downloadProgress.intValue
    val downloadFailed = updateVM.downloadFailed.value
    val isDownloadComplete = updateVM.isDownloadComplete.value
    val downloadedFilePath = updateVM.downloadedFilePath.value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val needsUpdate = newVersion.whetherNeedUpdate(currentVersion, skipVersion)

    LaunchedEffect(Unit) {
        val path = withIO { UpdateInfoPreference.getValueAsync(context).downloadedApkPath }
        if (path.isNotEmpty()) {
            if (File(path).exists() && newVersion.whetherNeedUpdate(currentVersion, skipVersion)
                && !updateVM.isDownloading.value && !updateVM.isDownloadComplete.value
            ) {
                updateVM.onDownloadComplete(path)
            } else {
                withIO { UpdateInfoPreference.updateAsync(context) { it.copy(downloadedApkPath = "") } }
            }
        }
    }

    when {
        isDownloading -> DownloadProgressBanner(
            newVersion = newVersion.toString(),
            progress = downloadProgress,
            onCancel = {
                updateVM.cancelDownload()
                sendEvent(CancelUpdateDownloadEvent())
            },
        )

        isDownloadComplete -> DownloadCompleteBanner(
            onInstall = {
                PackageHelper.install(context, File(downloadedFilePath))
                exitProcess(0)
            },
            onDismiss = {
                scope.launch { withIO { UpdateInfoPreference.updateAsync(context) { it.copy(downloadedApkPath = "") } } }
                updateVM.resetDownload()
            },
        )

        downloadFailed -> DownloadFailedBanner(
            onGitHub = { WebHelper.open(context, GITHUB_RELEASES_URL) },
            onRetry = {
                updateVM.startDownload()
                sendEvent(DownloadUpdateEvent())
            },
            onDismiss = { updateVM.resetDownload() },
        )

        needsUpdate -> PBanner(
            title = stringResource(R.string.get_new_updates, newVersion.toString()),
            desc = stringResource(R.string.get_new_updates_desc),
            icon = R.drawable.lightbulb,
        ) { updateVM.showDialog() }
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
                    text = stringResource(R.string.downloading_update, progress),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(R.string.cancel),
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
        title = stringResource(R.string.update_downloaded),
        icon = R.drawable.lightbulb,
        action = {
            PFilledButton(
                text = stringResource(R.string.install_update),
                small = true,
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
        title = stringResource(R.string.download_update_failed),
        backgroundColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        icon = R.drawable.lightbulb,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PFilledButton(
                    text = stringResource(R.string.try_again),
                    small = true,
                    onClick = onRetry,
                )
                PFilledButton(
                    text = stringResource(R.string.download_on_github),
                    small = true,
                    onClick = onGitHub,
                )
            }
        },
        onClick = onDismiss,
    )
}
