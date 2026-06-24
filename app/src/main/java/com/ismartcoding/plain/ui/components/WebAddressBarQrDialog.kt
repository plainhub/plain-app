package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.helpers.QrCodeGenerateHelper
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.enums.ButtonSize

@Composable
fun WebAddressBarQrDialog(
    url: String,
    onClose: () -> Unit,
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onClose,
        confirmButton = {
            PFilledButton(text = stringResource(Res.string.close), buttonSize = ButtonSize.MEDIUM, onClick = onClose)
        },
        title = {},
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.scan_qrcode_to_access_web),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Image(
                    bitmap = QrCodeGenerateHelper.generate(url, 300, 300).asImageBitmap(),
                    contentDescription = stringResource(Res.string.qrcode),
                    modifier = Modifier.size(300.dp),
                )
            }
        },
    )
}
