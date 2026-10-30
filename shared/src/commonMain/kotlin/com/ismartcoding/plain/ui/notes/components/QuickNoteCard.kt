package com.ismartcoding.plain.ui.notes.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.ismartcoding.plain.features.notes.NotesClient
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.i18n.edit
import com.ismartcoding.plain.ui.resources.maximize_2 as ui_drawable_maximize_2
import com.ismartcoding.plain.i18n.note_saved
import com.ismartcoding.plain.i18n.quick_note_hint
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.components.ToastManager
import com.ismartcoding.plain.ui.components.ToastType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.i18n.undo
import com.ismartcoding.plain.i18n.save

/** In-process cache so re-entering the page renders the draft immediately, without an async swap. */
object QuickNoteDraftCache {
    val drafts = mutableMapOf<String, String>()
}

@Composable
fun QuickNoteCard(
    modifier: Modifier = Modifier,
    onOpenNote: (String) -> Unit,
    onEmptyExpand: () -> Unit,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val drafts = QuickNoteDraftCache.drafts
    val scope = rememberCoroutineScope()
    var text by remember(source.key) { mutableStateOf(drafts[source.key] ?: "") }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        if (drafts[source.key] == null) {
            val saved = source.notes.readDraft()
            drafts[source.key] = saved
            text = saved
        }
    }
    // Persist the draft (debounced) so it survives process death.
    LaunchedEffect(source) {
        snapshotFlow { text }
            .drop(1)
            .collectLatest { t ->
                drafts[source.key] = t
                delay(300)
                source.notes.writeDraft(t)
            }
    }
    DisposableEffect(source) {
        onDispose {
            val t = text
            drafts[source.key] = t
            coIO { source.notes.writeDraft(t) }
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
                    source.notes.save(id = "", title = "", content = content).id
                }
            onSaved(id)
        }
    }

    PCard(
        modifier = modifier.border(
            BorderStroke(1.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent),
            MaterialTheme.shapes.medium,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f)
                        .heightIn(min = 48.dp, max = 112.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { innerField ->
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
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
                PIconButton(
                    icon = UiRes.drawable.ui_drawable_maximize_2,
                    iconSize = 20.dp,
                    contentDescription = stringResource(Res.string.edit),
                    click = {
                        if (canSave) saveAsync(onSaved = onOpenNote)
                        else onEmptyExpand()
                    },
                )
            }
            if (canSave) {
                Row(modifier = Modifier.align(Alignment.End)) {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_check,
                        contentDescription = stringResource(Res.string.save),
                        tint = MaterialTheme.colorScheme.primary,
                        click = {
                            val draft = text
                            saveAsync { id ->
                                ToastManager.showToast(
                                    savedText,
                                    type = ToastType.SUCCESS,
                                    durationMs = 4000L,
                                    actionLabel = undoText,
                                ) {
                                    coIO { NotesClient.trashAsync(setOf(id)) }
                                    text = draft
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
