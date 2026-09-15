package com.ismartcoding.plain.ui.page
import com.ismartcoding.plain.preferences.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.codeeditor.CodeEditor
import com.ismartcoding.plain.ui.components.codeeditor.EditorLoadState
import com.ismartcoding.plain.ui.models.TextFileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPage(
    navController: NavHostController,
    title: String,
    content: String,
    language: String,
    textFileVM: TextFileViewModel = viewModel { TextFileViewModel() },
) {
    LaunchedEffect(Unit) {
        textFileVM.loadConfigAsync()
        textFileVM.controller.openText(content, language)
    }

    if (textFileVM.showMoreActions.value) {
        ViewTextContentBottomSheet(textFileVM, content)
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = title, actions = {
                ActionButtonMore {
                    textFileVM.showMoreActions.value = true
                }
            })
        },
        content = { paddingValues ->
            val loadState = textFileVM.controller.loadState.value
            Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                if (loadState is EditorLoadState.Ready) {
                    CodeEditor(
                        controller = textFileVM.controller,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
    )
}
