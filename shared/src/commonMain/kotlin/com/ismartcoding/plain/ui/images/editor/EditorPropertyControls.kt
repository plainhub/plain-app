package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.components.ColorPickerDialog
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditorNumberField(label: String, value: Double, range: ClosedFloatingPointRange<Double>, onChange: (Double) -> Unit) {
    var draft by remember(value) { mutableStateOf(if (value % 1 == 0.0) value.toInt().toString() else value.toString()) }
    val number = draft.toDoubleOrNull()
    PTextField(readOnly = false, value = draft, label = label, showPasteAction = false,
        errorMessage = if (number != null && number.isFinite() && number in range) "" else stringResource(Res.string.image_editor_invalid_number),
        onValueChange = { draft = it; it.toDoubleOrNull()?.takeIf { number -> number.isFinite() && number in range }?.let(onChange) })
}

@Composable
internal fun EditorToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = value, role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = null)
    }
}

@Composable
internal fun EditorChoices(label: String, value: String, choices: List<Pair<String, String>>, onChange: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        choices.forEach { (key, text) -> PTextButton(text, { onChange(key) }, contentColor = if (value == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun EditorColorField(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    PTextButton("$label: $value", { open = true })
    if (open) ColorPickerDialog(label, if (value.startsWith('#')) value else "#000000", { open = false }, { onChange(it); open = false })
}
