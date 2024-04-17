package com.ismartcoding.plain.ui.page

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.base.AceEditor
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
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
            viewModel.showLoading.value = false
        }
    }

    PScaffold(
        navController,
        topBarTitle = title,
        actions = {
            PIconButton(
                icon = Icons.AutoMirrored.Outlined.WrapText,
                contentDescription = stringResource(R.string.wrap_content),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                viewModel.toggleWrapContent(context)
            }
            PIconButton(
                icon = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                ShareHelper.shareText(context, content)
            }
        },
        content = {
            if (viewModel.showLoading.value) {
                NoDataColumn(loading = true)
                return@PScaffold
            }
            if (!viewModel.isEditorReady.value) {
                NoDataColumn(loading = true)
            }
            AceEditor(viewModel, scope, content, language, isDarkTheme, readOnly = true)
        },
    )
}




