package com.ismartcoding.plain.ui.page.chat

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.share
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.base.PClickableText
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.base.urlAt
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.print as ui_drawable_print
import com.ismartcoding.plain.i18n.print

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTextPage(
    navController: NavHostController,
    content: String,
    onPrint: (TextMeasurer, String, String) -> Unit,
) {
    val text = content.linkify()
    val textMeasurer = rememberTextMeasurer()
    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                navigationIcon = {
                    NavigationCloseIcon { navController.navigateUp() }
                },
                title = "",
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_print,
                        contentDescription = stringResource(Res.string.print),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        onPrint(textMeasurer, "Chat Text", content)
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = paddingValues.calculateTopPadding())
                        .verticalScroll(rememberScrollState()),
            ) {
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PClickableText(
                        text = text,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                lineHeight = 36.sp,
                            ),
                        onClick = { position -> text.urlAt(position) },
                    )
                }
            }
        },
    )
}
