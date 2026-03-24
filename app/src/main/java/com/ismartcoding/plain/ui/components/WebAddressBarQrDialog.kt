package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.QrCodeGenerateHelper

@Composable
fun WebAddressBarQrDialog(
    url: String,
    onClose: () -> Unit,
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onClose,
        confirmButton = {
            Button(onClick = onClose) {
                Text(stringResource(id = R.string.close))
            }
        },
        title = {},
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.scan_qrcode_to_access_web),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Image(
                    bitmap = QrCodeGenerateHelper.generate(url, 300, 300).asImageBitmap(),
                    contentDescription = stringResource(id = R.string.qrcode),
                    modifier = Modifier.size(300.dp),
                )
            }
        },
    )
}
