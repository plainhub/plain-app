package com.ismartcoding.plain.ui.editor.markdown

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.editor.markdown.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.apps.components.*
import com.ismartcoding.plain.ui.audio.components.*
import com.ismartcoding.plain.ui.notes.components.*
import com.ismartcoding.plain.ui.web.components.*
import com.ismartcoding.plain.ui.components.*
import com.ismartcoding.plain.ui.media.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.components.ColorPickerDialog
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.features.web.WebHelper
import com.ismartcoding.plain.ui.editor.markdown.MdEditorViewModel
import com.ismartcoding.plain.ui.editor.markdown.MdToolbarCategory
import com.ismartcoding.plain.ui.editor.markdown.MdToolbarItem
import com.ismartcoding.plain.ui.editor.markdown.mdToolbarCategories
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.checkColorHex
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.circle_help as ui_drawable_circle_help

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdEditorBottomAppBar(
    mdEditorVM: MdEditorViewModel,
) {
    // key of the category whose sub-toolbar is open, null when collapsed
    var activeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val active = mdToolbarCategories.firstOrNull { it.key == activeKey }

    if (mdEditorVM.showInsertImage.value) {
        MdEditorInsertImageDialog(mdEditorVM = mdEditorVM)
    }
    if (mdEditorVM.showColorPicker.value) {
        ColorPickerDialog(
            stringResource(Res.string.pick_color),
            initValue = "FFFFFFFF",
            onDismiss = {
                mdEditorVM.showColorPicker.value = false
            }, onConfirm = {
                val hex = it.checkColorHex()
                if (hex != null) {
                    mdEditorVM.insertColor("#$hex")
                } else {
                    DialogHelper.showMessage(LocaleHelper.getString(Res.string.invalid_value))
                }
            })
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .navigationBarsPadding()
    ) {
        // sub toolbar: icon-over-caption items of the active category, no animation
        if (active != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                active?.items?.forEach { item ->
                    SubToolbarButton(item) { item.click(mdEditorVM) }
                }
            }
        }
        // main toolbar: fixed category buttons + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            mdToolbarCategories.forEach { category ->
                CategoryButton(
                    category = category,
                    active = category.key == activeKey,
                    modifier = Modifier
                        .weight(1f),
                ) {
                    activeKey = if (activeKey == category.key) null else category.key
                }
            }
            CategoryButton(
                category = MdToolbarCategory("help", Res.string.help, icon = UiRes.drawable.ui_drawable_circle_help, items = emptyList()),
                active = false,
                modifier = Modifier
                    .weight(1f),
            ) {
                WebHelper.open("https://plainapp.app/docs/markdown")
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: MdToolbarCategory,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val pill = if (active) {
        Modifier.background(bgColor)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = bgColor
                ), onClick = onClick
            )
            .heightIn(min = 64.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(category.icon ?: UiRes.drawable.ui_drawable_circle_help),
                contentDescription = stringResource(category.tip),
                tint = contentColor,
            )
        }
        VerticalSpace(4.dp)
        Text(
            stringResource(category.tip),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = contentColor,
        )
    }
}

@Composable
private fun SubToolbarButton(
    item: MdToolbarItem,
    onClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .padding(start = 4.dp)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(item.icon ?: UiRes.drawable.ui_drawable_circle_help),
            contentDescription = stringResource(item.tip),
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        if (item.caption != null) {
            Text(
                stringResource(item.caption),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
