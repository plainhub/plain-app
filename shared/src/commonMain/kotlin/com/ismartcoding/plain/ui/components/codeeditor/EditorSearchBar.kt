package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PIconButton
import org.jetbrains.compose.resources.stringResource

/** Compact find bar: query, case toggle, prev/next and match counter. */
@Composable
fun EditorSearchBar(controller: EditorController) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(controller.searchVisible.value) {
        if (controller.searchVisible.value) {
            runCatching { focusRequester.requestFocus() }
        }
    }
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = controller.searchQuery.value,
            onValueChange = { controller.requestSearch(it, controller.searchCaseSensitive.value, controller.searchRegex.value) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { controller.nextMatch() }),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                if (controller.searchQuery.value.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.search),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = tint.copy(alpha = 0.7f),
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .focusRequester(focusRequester),
        )
        val count = controller.matchCount()
        // Subscribe to search result publication: matchCount() reads a plain field.
        controller.matchesVersion.value
        val countText = if (controller.searchQuery.value.isEmpty()) {
            ""
        } else if (count == 0) {
            if (controller.searchComplete.value) "0/0" else "…"
        } else if (controller.searchTruncated.value && count >= EditorController.MAX_MATCHES) {
            "${EditorController.MAX_MATCHES}+"
        } else {
            "${controller.currentMatchIndex.value + 1}/$count"
        }
        if (countText.isNotEmpty()) {
            Text(
                text = countText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = tint,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        PIconButton(
            icon = Res.drawable.match_case,
            tint = if (controller.searchCaseSensitive.value) accent else tint,
        ) {
            controller.requestSearch(controller.searchQuery.value, !controller.searchCaseSensitive.value, controller.searchRegex.value)
        }
        PIconButton(icon = Res.drawable.chevron_up, tint = tint, enabled = count > 0) { controller.prevMatch() }
        PIconButton(icon = Res.drawable.chevron_down, tint = tint, enabled = count > 0) { controller.nextMatch() }
        PIconButton(icon = Res.drawable.x, tint = tint) { controller.setSearchVisible(false) }
    }
}
