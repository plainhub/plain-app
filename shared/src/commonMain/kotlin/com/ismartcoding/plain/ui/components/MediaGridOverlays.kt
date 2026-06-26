package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.theme.darkMask
import com.ismartcoding.plain.ui.theme.lightMask

@Composable
fun SelectedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.lightMask())
            .aspectRatio(1f)
    )
}

@Composable
fun CastModeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.darkMask())
            .aspectRatio(1f)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp),
            painter = painterResource(Res.drawable.cast),
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

@Composable
fun SelectionCheckbox(
    selected: Boolean,
    id: String,
    dragSelectState: DragSelectState,
) {
    Checkbox(
        modifier = Modifier,
        checked = selected,
        onCheckedChange = {
            dragSelectState.select(id)
        },
    )
}

@Composable
fun SizeLabel(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.darkMask()),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp),
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
        )
    }
}
