package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.platform.rememberClipboardWriter
import com.ismartcoding.plain.ui.resources.Res
import com.ismartcoding.plain.ui.resources.copy_text
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.listItemValue
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RowScope.PListKeyValueContent(
    title: String,
    value: String,
    subtitle: String,
    showMore: Boolean,
    contentColor: Color?,
) {
    PListItemValueLayout(
        modifier = Modifier.weight(1f),
        title = { Text(text = title, style = MaterialTheme.typography.listItemTitle().contentColored(contentColor)) },
        supporting = if (subtitle.isNotEmpty()) ({ PListItemSubtitle(subtitle, contentColor, Modifier.padding(top = 4.dp)) }) else null,
        value = { PListCopyableValue(title, value, contentColor) },
        end = if (showMore) ({ PListItemChevron(contentColor, spaced = true) }) else null,
    )
}

@Composable
private fun PListCopyableValue(title: String, value: String, contentColor: Color?) {
    var menuOpen by remember(title, value) { mutableStateOf(false) }
    val copyLabel = stringResource(Res.string.copy_text)
    val writeClipboard = rememberClipboardWriter()
    Box {
        DisableSelection {
            Text(
                text = value,
                style = MaterialTheme.typography.listItemValue().contentColored(contentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(title, value) {
                        awaitEachGesture {
                            awaitFirstDown()
                            // Leave a tap or scroll to the row; consume only after a long press.
                            val finished = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation()
                                true
                            }
                            if (finished == null) {
                                menuOpen = true
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
                    .semantics {
                        onLongClick(label = copyLabel) {
                            menuOpen = true
                            true
                        }
                    },
                textAlign = TextAlign.End,
            )
        }
        PDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            PDropdownMenuItem(
                text = { Text(copyLabel) },
                onClick = {
                    writeClipboard(title, value)
                    menuOpen = false
                },
            )
        }
    }
}
