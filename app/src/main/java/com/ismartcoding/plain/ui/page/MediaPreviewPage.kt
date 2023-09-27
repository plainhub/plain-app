package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.PScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewPage(navController: NavHostController) {
    PScaffold(
        navController,
        content = {
            LazyColumn {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    }
                }
            }
        },
    )
}
