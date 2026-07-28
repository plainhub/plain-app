package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.ismartcoding.plain.ui.base.PTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.TextFileType
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.writeCrashReport
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.nav.navigateTextFile

@Composable
fun CrashReportDialog(
    crashReport: String,
    navController: NavHostController?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.crash_report_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(Res.string.crash_report_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.view),
                buttonSize = ButtonSize.MEDIUM,
                onClick = {
                    val path = writeCrashReport(crashReport)
                    if (path.isNotEmpty()) {
                        navController?.navigateTextFile(path, type = TextFileType.CRASH_REPORT)
                    }
                    onDismiss()
                })
        },
        dismissButton = {
            PTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
        },
    )
}
