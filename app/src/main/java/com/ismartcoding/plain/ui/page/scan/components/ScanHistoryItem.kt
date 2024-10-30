package com.ismartcoding.plain.ui.page.scan.components

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.base.urlAt
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.theme.red

@Composable
private fun ActionButton(
    icon: Int,
    text: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}

@Composable
fun ScanHistoryItem(
    context: Context,
    text: String,
    onDelete: () -> Unit,
) {
    PCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val newText = text.linkify()
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                ClickableText(
                    text = newText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    onClick = { position -> newText.urlAt(context, position) },
                )
            }

            VerticalSpace(dp = 8.dp)

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionButton(
                    icon = R.drawable.copy,
                    text = stringResource(R.string.copy),
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        val clip = ClipData.newPlainText(getString(R.string.scan_result), text)
                        clipboardManager.setPrimaryClip(clip)
                        DialogHelper.showTextCopiedMessage(text)
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = R.drawable.delete_forever,
                    text = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.red,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}