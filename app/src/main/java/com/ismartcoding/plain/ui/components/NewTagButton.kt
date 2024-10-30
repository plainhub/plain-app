package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.cardContainer

@Composable
fun NewTagButton(click: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.cardContainer())
            .clickable { click() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.plus),
            contentDescription = stringResource(R.string.add_tag),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}