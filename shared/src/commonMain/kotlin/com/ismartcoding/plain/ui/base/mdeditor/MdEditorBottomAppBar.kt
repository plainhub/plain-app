package com.ismartcoding.plain.ui.base.mdeditor

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.components.ColorPickerDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.models.MdToolbarCategory
import com.ismartcoding.plain.ui.models.MdToolbarItem
import com.ismartcoding.plain.ui.models.mdToolbarCategories
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.checkColorHex

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
                    .height(50.dp)
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
                .height(54.dp),
        ) {
            mdToolbarCategories.forEach { category ->
                CategoryButton(
                    category = category,
                    active = category.key == activeKey,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    activeKey = if (activeKey == category.key) null else category.key
                }
            }
            CategoryButton(
                category = MdToolbarCategory("help", stringResource(Res.string.help), icon = Res.drawable.circle_help, items = emptyList()),
                active = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                WebHelper.open("https://www.markdownguide.org/basic-syntax")
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
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(category.icon ?: Res.drawable.circle_help),
                contentDescription = category.tip,
                tint = contentColor,
            )
        }
        Text(
            category.tip,
            fontSize = 9.sp,
            lineHeight = 10.sp,
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
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(item.icon ?: Res.drawable.circle_help),
            contentDescription = item.tip,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        if (item.caption != null) {
            Text(
                item.caption,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = contentColor,
            )
        }
    }
}
