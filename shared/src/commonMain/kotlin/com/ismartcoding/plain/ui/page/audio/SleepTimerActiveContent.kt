package com.ismartcoding.plain.ui.page.audio

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.ui.base.CircularTimer
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
internal fun SleepTimerActiveContent(
    remainingTimeMs: Long,
    selectedTimeMinutes: Int,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
        val hours = remainingTimeMs / (1000L * 60 * 60)
        val minutes = (remainingTimeMs / (1000L * 60)) % 60
        val seconds = (remainingTimeMs / 1000L) % 60
        val totalDurationMs = selectedTimeMinutes * 60f * 1000f
        val elapsedTimeMs = totalDurationMs - remainingTimeMs
        val progress = (elapsedTimeMs / totalDurationMs).coerceIn(0f, 1f)

        CircularTimer(progress = progress)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 48.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            VerticalSpace(dp = 8.dp)
            Text(
                text = stringResource(Res.string.remaining),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
