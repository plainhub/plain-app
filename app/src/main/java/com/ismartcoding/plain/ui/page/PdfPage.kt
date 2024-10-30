package com.ismartcoding.plain.ui.page

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toFile
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PdfView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPage(
    navController: NavHostController,
    uri: Uri,
) {
    val context = LocalContext.current

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = uri.getFileName(context),
                actions = {
                    PIconButton(
                        icon = R.drawable.share_2,
                        contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        if (uri.scheme == "content") {
                            ShareHelper.shareUri(context, uri)
                        } else {
                            ShareHelper.shareFile(context, uri.toFile())
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
