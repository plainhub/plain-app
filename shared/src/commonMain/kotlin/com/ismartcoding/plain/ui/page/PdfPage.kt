package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.PdfView
import com.ismartcoding.plain.platform.shareFileAs
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPage(
    navController: NavHostController,
    uri: String,
    fileName: String = "",
) {
    val title = fileName.ifEmpty { uri.substringAfterLast('/').decodeURLPartSafe() }

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
                        // Strip the file:// scheme so ShareHelper gets a real path;
                        // for content:// URIs this falls back to a graceful error.
                        val filePath = uri.removePrefix("file://")
                        shareFileAs(filePath, fileName)
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

private fun String.decodeURLPartSafe(): String {
    return try { decodeURLPart() } catch (_: Exception) { this }
}

private fun String.decodeURLPart(): String {
    val sb = StringBuilder(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == '%' && i + 2 < length) {
            val hex = substring(i + 1, i + 3)
            val code = hex.toIntOrNull(16)
            if (code != null) {
                sb.append(code.toChar())
                i += 3
                continue
            }
        }
        if (c == '+') {
            sb.append(' ')
        } else {
            sb.append(c)
        }
        i++
    }
    return sb.toString()
}
