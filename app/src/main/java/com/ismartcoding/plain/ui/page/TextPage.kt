package com.ismartcoding.plain.ui.page

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preference.LocalDarkTheme
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
    viewModel: TextFileViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = DarkTheme.isDarkTheme(darkTheme)

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadConfigAsync(context)
            viewModel.isDataLoading.value = false
        }
    }

    if (viewModel.showMoreActions.value) {
        ViewTextContentBottomSheet(viewModel, content)
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = title, actions = {
                ActionButtonMore {
                    viewModel.showMoreActions.value = true
                }
            })
        },
        content = {
            if (viewModel.isDataLoading.value) {
                NoDataColumn(loading = true)
                return@PScaffold
            }
            if (!viewModel.isEditorReady.value) {
                NoDataColumn(loading = true)
            }
            AceEditor(
                viewModel, scope, EditorData(
                    language,
                    viewModel.wrapContent.value,
                    isDarkTheme = isDarkTheme,
                    readOnly = viewModel.readOnly.value,
                    gotoEnd = false,
                    content = content
                )
            )
        },
    )
}




