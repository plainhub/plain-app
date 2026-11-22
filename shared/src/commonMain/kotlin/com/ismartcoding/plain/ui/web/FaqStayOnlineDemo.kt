package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PExpandableSection
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.ui.theme.tipsText
import org.jetbrains.compose.resources.stringResource


private val DemoPillShape = CircleShape

@Composable
fun FaqStayOnlineDemo() {
    DemoPlayer(
        stepDurationsMs = listOf(2000L, 2400L),
        endHoldMs = 1200L,
        contentMinHeight = 232.dp,
        contentBackground = MaterialTheme.colorScheme.background,
    ) { step, elapsedMs ->
        Box(
            Modifier.matchParentSize().background(if (step >= 1) Color.Black else Color.Transparent),
            contentAlignment = if (step >= 1) Alignment.Center else Alignment.TopCenter,
        ) {
            if (step == 1) {
                DemoStayOnlineOverlay(ring = true)
            } else if (step == 0) {
                PCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    PExpandableSection(
                        text = stringResource(Res.string.connection_keeps_dropping),
                        expanded = true,
                        stateDescription = stringResource(Res.string.expanded),
                        onClick = {},
                    ) {
                        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                            Text(stringResource(Res.string.connection_keeps_dropping_help), style = MaterialTheme.typography.tipsText())
                            VerticalSpace(16.dp)
                            Box(Modifier.fillMaxWidth().demoTapRing(elapsedMs > 0f)) {
                                PFilledButton(
                                    type = ButtonType.TONAL,
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(Res.string.stay_online),
                                    onClick = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoStayOnlineOverlay(ring: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.stay_online_keep_running),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier.demoTapRing(ring),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(DemoPillShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), DemoPillShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.stay_online_go_dark_now),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.stay_online_tap_to_exit),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
