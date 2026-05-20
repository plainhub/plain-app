package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.i18n.*
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.core.net.toFile
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PdfView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPage(
    navController: NavHostController,
    uri: Uri,
    fileName: String = "",
) {
    val context = LocalContext.current
    val title = fileName.ifEmpty { uri.getFileName(context) }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = title,
                actions = {
                    PIconButton(
                        icon = Res.drawable.share_2,
                        contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        if (uri.scheme == "content") {
                            ShareHelper.shareUri(context, uri)
                        } else {
                            ShareHelper.shareFile(context, uri.toFile(), displayName = fileName)
                        }
                    }
                },
            )
        },
        content = { paddingValues ->
            PdfView(
                uri = uri, modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            )
        },
    )
}
