package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getWindowWidth
import com.ismartcoding.lib.extensions.px2dp
import com.ismartcoding.lib.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.measureTextWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPage(
    navController: NavHostController,
    title: String,
    content: String,
) {
    val context = LocalContext.current
    val lines by remember(content) {
        derivedStateOf {
            content.split("\n")
        }
    }

    var wrapContent by rememberSaveable {
        mutableStateOf(true)
    }

    val lineNumberWidth = measureTextWidth(" ${lines.size + 1} ", MaterialTheme.typography.bodyLarge)
    val maxLengthLine = lines.maxByOrNull { it.length } ?: ""
    var maxLineWidth = context.px2dp(context.getWindowWidth().toFloat()).dp
    if (maxLengthLine.isNotEmpty()) {
        val newWidth = measureTextWidth(maxLengthLine, MaterialTheme.typography.bodyLarge)  + lineNumberWidth + 24.dp
        if (newWidth > maxLineWidth) {
            maxLineWidth = newWidth
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
                wrapContent = !wrapContent
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
            Box(
                modifier = if (wrapContent) {
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                } else {
                    Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                }
            ) {
                VerticalDivider(
                    Modifier
                        .width(lineNumberWidth)
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxHeight()
                )
                LazyColumn(
                    modifier = if (wrapContent) {
                        Modifier
                            .fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .width(maxLineWidth)
                    }
                ) {
                    itemsIndexed(lines) { index, it ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .width(lineNumberWidth),
                                text = " ${index + 1} ",
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                textAlign = TextAlign.End,
                            )
                            SelectionContainer(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp, end = 16.dp)
                            ) {
                                Text(
                                    text = it,
                                    softWrap = wrapContent,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                    item {
                        BottomSpace()
                    }
                }
            }
        },
    )
}
