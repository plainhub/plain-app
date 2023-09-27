package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.base.urlAt
import com.ismartcoding.plain.ui.models.SharedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTextPage(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
) {
    val context = LocalContext.current
    val text =
        sharedViewModel.chatContent.value.linkify(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        )
    PScaffold(
        navController,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                SelectionContainer {
                    ClickableText(
                        text = text,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                lineHeight = 36.sp,
                            ),
                        onClick = { position -> text.urlAt(context, position) },
                    )
                }
            }
        },
    )
}
