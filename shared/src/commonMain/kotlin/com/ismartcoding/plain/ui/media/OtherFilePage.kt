package com.ismartcoding.plain.ui.media

import com.ismartcoding.plain.ui.state.launchSafe

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.platform.system.shareFile
import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.files.stored.AppFileDisplayNameHelper
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_question as ui_drawable_file_question
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherFilePage(
    navController: NavHostController,
    path: String,
    title: String,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val fallbackName = path.substringAfterLast('/')
    var displayTitle by remember(path, title) { mutableStateOf(title.ifEmpty { fallbackName }) }
    var fileSize by remember(path) { mutableStateOf(0L) }

    LaunchedEffect(path, title) {
        displayTitle = withIO { title.ifEmpty { source.storedFiles.displayName(path.substringAfterLast('/').substringBeforeLast('.')).ifEmpty { fallbackName } } }
        fileSize = source.files.size(path)
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                title = displayTitle,
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_share_2,
                        contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        scope.launchSafe { shareFile(source.fileForOpen(path, displayTitle)) }
                    }
                },
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            modifier =
                                Modifier
                                    .padding(bottom = 32.dp)
                                    .size(56.dp),
                            painter = painterResource(UiRes.drawable.ui_drawable_file_question),
                            contentDescription = "",
                        )
                        SelectionContainer {
                            Text(
                                text = displayTitle,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                        SelectionContainer {
                            Text(
                                text = stringResource(Res.string.file_size) + ": " + fileSize.formatBytes(),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                        Text(
                            text = stringResource(Res.string.unknown_file_description),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        VerticalSpace(dp = 64.dp)
                        PFilledButton(
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            text = stringResource(Res.string.open_with_other_app),
                            onClick = {
                                scope.launchSafe { openFileExternal(source.fileForOpen(path, displayTitle)) }
                            })
                    }
                }
            }
        },
    )
}
