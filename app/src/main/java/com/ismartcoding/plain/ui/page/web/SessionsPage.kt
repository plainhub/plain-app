package com.ismartcoding.plain.ui.page.web

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DisplayText
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.theme.palette.onDark
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.SessionList
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val sessionDao = AppDatabase.instance.sessionDao()
    val sessions by sessionDao.getAllFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var updatedTs by remember { mutableLongStateOf(0L) }

    val refreshState = rememberRefreshLayoutState {
        updatedTs = System.currentTimeMillis()
        setRefreshState(RefreshContentState.Stop)
    }

    PScaffold(
        navController,
        content = {
            PullToRefresh(refreshLayoutState = refreshState) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    item {
                        DisplayText(text = stringResource(id = R.string.sessions))
                    }
                    items(sessions, key = { it.token + updatedTs }) { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp, end = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.last_visit_at) + " " + m.updatedAt.formatDateTime(),
                                modifier = Modifier
                                    .weight(1f),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                            PIconButton(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onSurface,
                            ) {
                                DialogHelper.confirmToAction(context, R.string.confirm_to_delete) {
                                    scope.launch {
                                        withIO {
                                            SessionList.deleteAsync(m)
                                            HttpServerManager.loadTokenCache()
                                        }
                                    }
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface onDark MaterialTheme.colorScheme.inverseOnSurface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp, 16.dp, 16.dp, 8.dp)
                            ) {
                                SubItem(R.string.client_id, m.clientId)
                                SubItem(R.string.ip_address, m.clientIP)
                                SubItem(R.string.created_at, m.createdAt.formatDateTime())
                                SubItem(R.string.os, m.osName.capitalize() + " " + m.osVersion)
                                SubItem(R.string.browser, m.browserName.capitalize() + " " + m.browserVersion)
                                SubItem(R.string.status, stringResource(id = if (HttpServerManager.wsSessions.any { it.clientId == m.clientId }) R.string.online else R.string.offline))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        BottomSpace()
                    }
                }
            }
        }
    )
}

@Composable
fun SubItem(@StringRes titleId: Int, value: String) {
    Text(
        text = stringResource(id = titleId),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    SelectionContainer {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

