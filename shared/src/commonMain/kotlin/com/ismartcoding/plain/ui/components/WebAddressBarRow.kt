package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.platform.setClipboardText

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
            Text(
                text = url,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable {
                        setClipboardText("url", url)
                        DialogHelper.showTextCopiedMessage(url)
                    },
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                    ),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        PIconButton(
            icon = Res.drawable.pen,
            modifier = Modifier.size(32.dp),
            iconSize = 16.dp,
            contentDescription = if (isHostnameRow) "Edit hostname" else "Edit port",
            tint = MaterialTheme.colorScheme.onSurface,
            click = onEditClick,
        )
        PIconButton(
            icon = Res.drawable.qr_code,
            modifier = Modifier.size(32.dp),
            iconSize = 16.dp,
            contentDescription = "Show QR code",
            tint = MaterialTheme.colorScheme.onSurface,
            click = onQrClick,
        )
        HorizontalSpace(dp = 4.dp)
    }
}
