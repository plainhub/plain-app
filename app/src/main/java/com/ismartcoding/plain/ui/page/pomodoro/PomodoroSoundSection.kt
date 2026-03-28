package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileEvent

@Composable
fun PomodoroSoundSection(
    soundPath: String,
    originalFileName: String,
    onClear: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.custom_sound),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            if (soundPath.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        text = originalFileName, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { sendEvent(PickFileEvent(PickFileTag.POMODORO, PickFileType.FILE, multiple = false)) }) {
                            Text(stringResource(R.string.change))
                        }
                        TextButton(onClick = onClear) {
                            Text(text = stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { sendEvent(PickFileEvent(PickFileTag.POMODORO, PickFileType.FILE, multiple = false)) }) {
                        Text(stringResource(R.string.select_sound))
                    }
                }
            }
        }
    }
}
