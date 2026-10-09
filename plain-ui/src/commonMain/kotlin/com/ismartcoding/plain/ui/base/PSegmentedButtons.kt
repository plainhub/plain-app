package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize

/** Single selection with immediate feedback. Use -1 for no selection and content-sized width for long labels. */
@Composable
fun <T> PSegmentedButtons(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    buttonSize: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    optionEnabled: (T) -> Boolean = { true },
    colors: PSegmentedButtonsColors = PSegmentedButtonsDefaults.colors(),
    content: (@Composable RowScope.(option: T, selected: Boolean) -> Unit)? = null,
) {
    require(selectedIndex == -1 || selectedIndex in options.indices) { "selectedIndex must be -1 or a valid option index" }
    val textStyle = when (buttonSize) {
        ButtonSize.SMALL -> MaterialTheme.typography.labelMedium
        ButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
        ButtonSize.LARGE -> MaterialTheme.typography.titleMedium
    }
    Row(
        modifier = modifier
            .selectableGroup()
            .background(colors.containerColor, MaterialTheme.shapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val optionLabel = label(option)
            val selected = index == selectedIndex
            val optionIsEnabled = enabled && optionEnabled(option)
            val contentColor = if (selected) colors.selectedContentColor else colors.contentColor
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (optionIsEnabled) 1f else 0.38f)
                    .background(
                        if (selected) colors.selectedContainerColor else Color.Transparent,
                        MaterialTheme.shapes.small,
                    )
                    .selectable(
                        selected = selected,
                        enabled = optionIsEnabled,
                        interactionSource = null,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .semantics { if (content != null) contentDescription = optionLabel }
                    .heightIn(min = buttonSize.height)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor, LocalTextStyle provides textStyle) {
                    if (content != null) {
                        content(option, selected)
                    } else {
                        Text(
                            text = optionLabel,
                            style = textStyle,
                            textAlign = TextAlign.Center,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
        }
    }
}
