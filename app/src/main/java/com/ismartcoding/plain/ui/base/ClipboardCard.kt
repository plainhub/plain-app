package com.ismartcoding.plain.ui.base

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.theme.cardBack

@Composable
fun ClipboardCard(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    color = MaterialTheme.colorScheme.cardBack(),
                    shape = RoundedCornerShape(16.dp),
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clip = ClipData.newPlainText(label, text)
                        clipboardManager.setPrimaryClip(clip)
                        DialogHelper.showMessage(R.string.copied)
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
