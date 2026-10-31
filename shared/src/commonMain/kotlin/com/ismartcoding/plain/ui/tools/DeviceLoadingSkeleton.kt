package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun DeviceLoadingSkeleton(tools: Boolean) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    BoxWithConstraints(Modifier.fillMaxWidth().testTag(if (tools) "device-tools-skeleton" else "device-chat-skeleton")) {
        val columns = (maxWidth / 104.dp).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (tools) {
                repeat(3) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(columns) {
                            Column(Modifier.weight(1f).height(88.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(color))
                                Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(MaterialTheme.shapes.small).background(color))
                            }
                        }
                    }
                }
            } else {
                repeat(6) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(color))
                        Column(Modifier.weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth(0.56f).height(16.dp).clip(MaterialTheme.shapes.small).background(color))
                            Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(MaterialTheme.shapes.small).background(color))
                        }
                    }
                }
            }
        }
    }
}
