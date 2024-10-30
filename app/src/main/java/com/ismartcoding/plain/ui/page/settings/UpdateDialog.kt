package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.toVersion
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.preference.LocalNewVersion
import com.ismartcoding.plain.preference.LocalNewVersionDownloadUrl
import com.ismartcoding.plain.preference.LocalNewVersionLog
import com.ismartcoding.plain.preference.LocalNewVersionPublishDate
import com.ismartcoding.plain.preference.LocalNewVersionSize
import com.ismartcoding.plain.preference.SkipVersionPreference
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.UpdateViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(updateVM: UpdateViewModel) {
    val context = LocalContext.current
    val newVersion = LocalNewVersion.current.toVersion()
    val newVersionPublishDate = LocalNewVersionPublishDate.current
    val newVersionLog = LocalNewVersionLog.current
    val newVersionSize = LocalNewVersionSize.current
    val newVersionDownloadUrl = LocalNewVersionDownloadUrl.current
    val scope = rememberCoroutineScope()

    if (updateVM.updateDialogVisible.value) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.heightIn(max = 400.dp),
            onDismissRequest = { updateVM.updateDialogVisible.value = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rocket),
                    contentDescription = stringResource(R.string.change_log),
                )
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "Release $newVersion")
                    VerticalSpace(dp = 16.dp)
                    Text(
                        text = "${Instant.parse(newVersionPublishDate).formatDateTime()} ${newVersionSize.formatBytes()}",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    VerticalSpace(dp = 16.dp)
                }
            },
            text = {
                SelectionContainer {
                    Text(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        text = newVersionLog,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        WebHelper.open(context, newVersionDownloadUrl)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.update)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withIO { SkipVersionPreference.putAsync(context, newVersion.toString()) }
                            updateVM.hideDialog()
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.skip_this_version))
                }
            },
        )
    }
}