package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    topBar: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = containerColor,
        topBar = topBar,
        content = content,
        bottomBar = { bottomBar?.invoke() },
        floatingActionButton = { floatingActionButton?.invoke() },
    )
}
