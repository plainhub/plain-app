package com.ismartcoding.plain.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.CrashHandler
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import java.io.File

@Composable
fun CrashReportDialog(crashReport: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var includeAppLogs by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_report_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.crash_report_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = includeAppLogs,
                        onCheckedChange = { includeAppLogs = it },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.crash_include_app_logs))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val appVersion = MainApp.getAppVersion()
                val deviceInfo = buildString {
                    appendLine("--- Device Info ---")
                    appendLine("App: PlainApp $appVersion")
                    appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                    appendLine("Brand: ${android.os.Build.BRAND}")
                    appendLine("Product: ${android.os.Build.PRODUCT}")
                }
                val bodyText = buildString {
                    append(deviceInfo)
                    appendLine()
                    appendLine("--- Crash Report ---")
                    append(crashReport)
                    if (includeAppLogs) {
                        val logs = CrashHandler.getAppLogs(context)
                        if (logs.isNotEmpty()) {
                            appendLine()
                            appendLine()
                            appendLine("--- App Logs ---")
                            append(logs)
                        }
                    }
                }

                // Write crash report to a temp file so it can be attached to the email
                val crashFile = File(context.cacheDir, "crash_report.txt")
                try {
                    crashFile.writeText(bodyText)
                } catch (_: Exception) {}

                val attachmentUri: Uri? = try {
                    FileProvider.getUriForFile(context, Constants.AUTHORITY, crashFile)
                } catch (_: Exception) {
                    null
                }

                val intent = if (attachmentUri != null) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, "Crash Report - PlainApp $appVersion")
                        putExtra(Intent.EXTRA_TEXT, bodyText)
                        putExtra(Intent.EXTRA_STREAM, attachmentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${Constants.SUPPORT_EMAIL}")
                        putExtra(Intent.EXTRA_SUBJECT, "Crash Report - PlainApp $appVersion")
                        putExtra(Intent.EXTRA_TEXT, bodyText)
                    }
                }
                context.startActivity(Intent.createChooser(intent, null))
                onDismiss()
            }) {
                Text(stringResource(R.string.crash_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

