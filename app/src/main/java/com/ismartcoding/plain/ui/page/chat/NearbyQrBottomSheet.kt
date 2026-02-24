package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.helpers.QrCodeGenerateHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyQrBottomSheet(
    qrData: DQrPairData?,
    onDismiss: () -> Unit,
) {
    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = stringResource(R.string.pair_via_qr_title))
        TopSpace()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (qrData != null) {
                val qrContent = qrData.toQrContent()
                val bitmap = remember(qrContent) {
                    QrCodeGenerateHelper.generate(qrContent, 700, 700)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.qrcode),
                    modifier = Modifier.size(280.dp),
                )
                VerticalSpace(dp = 20.dp)
                Text(
                    text = stringResource(R.string.pair_via_qr_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        BottomSpace()
    }
}
