package com.ismartcoding.plain.ui.web.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.qrcode.generateQrCode
import androidx.compose.ui.graphics.FilterQuality
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard
import com.ismartcoding.plain.lib.extensions.toBreakableUrl
import com.ismartcoding.plain.ui.theme.dialogSheetBackground

@Composable
fun WebAddressBarQrDialog(
    url: String,
    verificationCode: String? = null,
    onClose: () -> Unit,
) {
    val qrCode = remember(url) { generateQrCode(url, 300, 300) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = onClose,
        confirmButton = {
            PTextButton(text = stringResource(Res.string.close), buttonSize = ButtonSize.MEDIUM, onClick = onClose)
        },
        title = {},
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(if (verificationCode == null) Res.string.scan_qrcode_to_access_web else Res.string.qrcode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Image(
                    bitmap = qrCode,
                    filterQuality = FilterQuality.None,
                    contentDescription = stringResource(Res.string.qrcode),
                    modifier = Modifier.size(300.dp),
                )
                if (verificationCode == null) Text(
                    text = url.toBreakableUrl(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ) else RequestVerificationCodeCard(verificationCode, stringResource(Res.string.pairing_compare_code))
            }
        },
    )
}
