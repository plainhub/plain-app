package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.music2 as ui_drawable_music2
import com.ismartcoding.plain.ui.resources.shuffle as ui_drawable_shuffle

/** Quick actions: shuffle the whole library / browse all items. */
@Composable
fun HomeQuickActions(
    onShuffleAll: () -> Unit,
    onViewAllItems: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        PFilledButton(
            text = stringResource(Res.string.shuffle_play),
            icon = painterResource(UiRes.drawable.ui_drawable_shuffle),
            modifier = Modifier.weight(1f),
            onClick = onShuffleAll,
        )
        Spacer(Modifier.width(12.dp))
        POutlinedButton(
            text = stringResource(Res.string.view_all),
            icon = painterResource(UiRes.drawable.ui_drawable_music2),
            modifier = Modifier.weight(1f),
            onClick = onViewAllItems,
        )
    }
}
