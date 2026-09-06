package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.check
import com.ismartcoding.plain.i18n.edit
import com.ismartcoding.plain.i18n.maximize_2
import com.ismartcoding.plain.i18n.note_saved
import com.ismartcoding.plain.i18n.quick_note_hint
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.i18n.undo
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.extensions.getMarkdownTitle
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.preferences.QuickNoteDraftPreference
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.ToastManager
import com.ismartcoding.plain.ui.base.ToastType
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** In-process cache so re-entering the page renders the draft immediately, without an async swap. */
object QuickNoteDraftCache {
    var text: String? = null
}

@Composable
fun QuickNoteCard(
    modifier: Modifier = Modifier,
    onOpenNote: (String) -> Unit,
    onEmptyExpand: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(QuickNoteDraftCache.text ?: "") }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (QuickNoteDraftCache.text == null) {
            val saved = QuickNoteDraftPreference.getAsync()
            QuickNoteDraftCache.text = saved
            text = saved
        }
    }
    // Persist the draft (debounced) so it survives process death.
    LaunchedEffect(Unit) {
        snapshotFlow { text }
            .drop(1)
            .collectLatest { t ->
                QuickNoteDraftCache.text = t
                delay(300)
                QuickNoteDraftPreference.putAsync(t)
            }
    }
    DisposableEffect(Unit) {
        onDispose {
            val t = text
            QuickNoteDraftCache.text = t
            coIO { QuickNoteDraftPreference.putAsync(t) }
        }
    }

    val savedText = stringResource(Res.string.note_saved)
    val undoText = stringResource(Res.string.undo)
    val canSave = text.isNotBlank()

    fun saveAsync(onSaved: (String) -> Unit) {
        scope.launch {
            val content = text
            text = ""
            val id =
                withIO {
                    NoteHelper.addOrUpdateAsync("") {
                        title = content.getMarkdownTitle()
                        this.content = content
                    }.id
                }
            onSaved(id)
        }
    }

    PCard(
        modifier =
            modifier.border(
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                    ),
                shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
            )
    ) {
        Box(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp, max = 108.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { innerField ->
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.quick_note_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerField()
                    }
                },
            )
            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
            PIconButton(
                icon = Res.drawable.maximize_2,
                iconSize = 20.dp,
                contentDescription = stringResource(Res.string.edit),
                click = {
                    if (canSave) {
                        saveAsync(onSaved = { id -> onOpenNote(id) })
                    } else {
                        onEmptyExpand()
                    }
                },
            )
            if (canSave) {
                PIconButton(
                    icon = Res.drawable.check,
                contentDescription = stringResource(Res.string.save),
                tint = MaterialTheme.colorScheme.primary,
                click = {
                    val draft = text
                    saveAsync(
                        onSaved = { id ->
                            ToastManager.showToast(
                                savedText,
                                type = ToastType.SUCCESS,
                                duration = 4000L,
                                actionLabel = undoText,
                            ) {
                                coIO { NoteHelper.trashAsync(setOf(id)) }
                                text = draft
                            }
                        },
                    )
                },
                )
            }
            }
        }
    }
}
