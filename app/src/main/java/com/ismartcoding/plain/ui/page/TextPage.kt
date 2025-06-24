package com.ismartcoding.plain.ui.page

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.AceEditor
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPage(
    navController: NavHostController,
    title: String,
    content: String,
    language: String,
    textFileVM: TextFileViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = DarkTheme.isDarkTheme(darkTheme)

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            textFileVM.loadConfigAsync(context)
            textFileVM.isDataLoading.value = false
        }
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
            Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                if (textFileVM.isDataLoading.value) {
                    NoDataColumn(loading = true)
                    return@PScaffold
                }
                if (!textFileVM.isEditorReady.value) {
                    NoDataColumn(loading = true)
                }
                AceEditor(
                    textFileVM, scope, EditorData(
                        language,
                        textFileVM.wrapContent.value,
                        isDarkTheme = isDarkTheme,
                        readOnly = textFileVM.readOnly.value,
                        gotoEnd = false,
                        content = content
                    )
                )
            }
        },
    )
}




