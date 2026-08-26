package com.ismartcoding.plain.ui.page.shares

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

private data class ShareItem(val share: DShare, val link: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharesPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ShareItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        items = ShareManager.listShares().map { ShareItem(it, ShareManager.buildLink(it)) }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.share_links),
            )
        },
        content = { paddingValues ->
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.share_no_links),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item { TopSpace() }
                    items(items, key = { it.share.id }) { item ->
                        PCard {
                            PListItem(
                                title = item.share.name.ifBlank { item.share.id },
                                subtitle = item.subtitle(),
                                titleTrailing = {
                                    if (item.share.readOnly) {
                                        Text(
                                            text = stringResource(Res.string.share_read_only),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                action = {
                                    CopyIconButton(
                                        text = item.link,
                                        clipLabel = stringResource(Res.string.share_link),
                                        copiedMessage = stringResource(Res.string.share_link_copied),
                                    )
                                    PIconButton(
                                        icon = Res.drawable.delete_forever,
                                        contentDescription = stringResource(Res.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    ) {
                                        DialogHelper.confirmToDelete {
                                            scope.launch {
                                                ShareManager.deleteShare(item.share.id)
                                                items = ShareManager.listShares().map { ShareItem(it, ShareManager.buildLink(it)) }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        VerticalSpace(16.dp)
                    }
                    item { BottomSpace(paddingValues) }
                }
            }
        },
    )
}

@Composable
private fun ShareItem.subtitle(): String = this.share.expiryLabel()

/** Human-readable expiry status of a share, e.g. "Expired" / "Expires …" / "Never". */
@Composable
fun DShare.expiryLabel(): String {
    val expiresAt = this.expiresAt
    return when {
        isExpired -> stringResource(Res.string.share_expired)
        expiresAt != null -> stringResource(Res.string.share_expires_on, expiresAt.formatDateTime())
        else -> stringResource(Res.string.share_expiry_never)
    }
}
