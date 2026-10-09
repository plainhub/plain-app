package com.ismartcoding.plain.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.tipsText
import androidx.compose.material3.MaterialTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import com.ismartcoding.plain.ui.resources.chevron_up as ui_drawable_chevron_up
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
fun OnlineConnectionHelp(onStayOnline: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(Res.string.connection_keeps_dropping)
    val expansion = stringResource(if (expanded) Res.string.online_help_expanded else Res.string.online_help_collapsed)
    PTextButton(
        modifier = Modifier.padding(horizontal = 8.dp).semantics {
            stateDescription = expansion
        },
        text = label,
        icon = painterResource(if (expanded) UiRes.drawable.ui_drawable_chevron_up else UiRes.drawable.ui_drawable_chevron_down),
        onClick = { expanded = !expanded },
    )
    AnimatedVisibility(expanded) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Text(
                text = stringResource(Res.string.connection_keeps_dropping_help),
                style = MaterialTheme.typography.tipsText(),
            )
            VerticalSpace(12.dp)
            POutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.stay_online),
                onClick = onStayOnline,
            )
        }
    }
}
