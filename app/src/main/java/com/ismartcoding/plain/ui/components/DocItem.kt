package com.ismartcoding.plain.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.PdfViewerDialog
import com.ismartcoding.plain.ui.TextEditorDialog
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocItem(
    navController: NavController,
    m: DFile,
) {
    PListItem(
        title = m.name,
        desc = FormatHelper.formatBytes(m.size) + ", " +  m.updatedAt.formatDateTime(),
        icon = AppHelper.getFileIconPath(m.name.getFilenameExtension()),
        modifier = PlainTheme.getCardModifier(),
        onClick = {
            if (m.path.isTextFile()) {
                if (m.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                    TextEditorDialog(Uri.fromFile(File(m.path))).show()
                } else {
                    DialogHelper.showMessage(R.string.text_file_size_limit)
                }
            } else if (m.path.isPdfFile()) {
                PdfViewerDialog(Uri.fromFile(File(m.path))).show()
            } else {
                navController.navigate("${RouteName.OTHER_FILE.name}?path=${m.path}")
            }
        }
    )
}