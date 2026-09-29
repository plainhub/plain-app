package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Contacts-style alphabet rail pinned to the end edge of the parent [Box].
 * Tapping or vertically dragging a letter invokes [onLetterSelect]; the host
 * list is expected to scroll to the matching section header.
 *
 * [letters] should match the section keys of the host list (A..Z plus "#" for
 * non-alphabetic entries).
 */
@Composable
fun BoxScope.AlphabetIndexBar(
    letters: List<String>,
    onLetterSelect: (String) -> Unit,
) {
    if (letters.isEmpty()) return

    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(28.dp)
            .padding(vertical = 8.dp)
            .pointerInput(letters) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val index = (change.position.y / size.height * letters.size).toInt()
                        .coerceIn(0, letters.lastIndex)
                    onLetterSelect(letters[index])
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .width(28.dp),
            )
        }
    }
}
