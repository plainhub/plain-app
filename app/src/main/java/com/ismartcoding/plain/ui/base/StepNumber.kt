package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun StepNumber(num: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(28.dp)) { drawCircle(primaryColor, radius = size.minDimension / 2) }
        Text(text = num.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = onPrimaryColor)
    }
}