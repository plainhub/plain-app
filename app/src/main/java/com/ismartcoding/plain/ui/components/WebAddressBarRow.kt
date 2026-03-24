package com.ismartcoding.plain.ui.components

import android.content.ClipData
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.helpers.DialogHelper

@Composable
fun WebAddressBarRow(
    url: String,
    isHostnameRow: Boolean,
    onEditClick: () -> Unit,
    onQrClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionContainer {
            ClickableText(
                text = AnnotatedString(url),
                modifier = Modifier.padding(start = 16.dp),
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                    ),
                onClick = {
                    val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), url)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(url)
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        PIconButton(
            icon = R.drawable.pen,
            modifier = Modifier.size(32.dp),
            iconSize = 16.dp,
            contentDescription = if (isHostnameRow) "Edit hostname" else "Edit port",
            tint = MaterialTheme.colorScheme.onSurface,
            click = onEditClick,
        )
        PIconButton(
            icon = R.drawable.qr_code,
            modifier = Modifier.size(32.dp),
            iconSize = 16.dp,
            contentDescription = "Show QR code",
            tint = MaterialTheme.colorScheme.onSurface,
            click = onQrClick,
        )
        HorizontalSpace(dp = 4.dp)
    }
}
