package com.ismartcoding.plain.ui.base

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.ui.helpers.DialogHelper

@Composable
fun ClipboardCard(
    label: String,
    text: String,
) {
    PCard {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    val clip = ClipData.newPlainText(label, text)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(text)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
