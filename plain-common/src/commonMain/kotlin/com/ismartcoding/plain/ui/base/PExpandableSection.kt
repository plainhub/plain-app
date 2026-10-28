package com.ismartcoding.plain.ui.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import com.ismartcoding.plain.ui.resources.chevron_up as ui_drawable_chevron_up

@Composable
fun PExpandableSection(
    text: String,
    expanded: Boolean,
    stateDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    toggleModifier: Modifier = Modifier,
    fadeContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { this.stateDescription = stateDescription }
                .heightIn(min = 48.dp)
                .then(toggleModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                painterResource(if (expanded) UiRes.drawable.ui_drawable_chevron_up else UiRes.drawable.ui_drawable_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + if (fadeContent) fadeIn() else EnterTransition.None,
            exit = shrinkVertically() + if (fadeContent) fadeOut() else ExitTransition.None,
        ) {
            content()
        }
    }
}
