package com.ismartcoding.plain.ui.page.dlna

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preferences.DlnaAllowedSendersPreference
import com.ismartcoding.plain.preferences.DlnaDeniedSendersPreference
import com.ismartcoding.plain.preferences.decodeSenderEntry
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DlnaCastRulesViewModel : ViewModel() {
    val allowedFlow = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val deniedFlow = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    fun load(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            allowedFlow.value = DlnaAllowedSendersPreference.getAsync(context).map { decodeSenderEntry(it) }
            deniedFlow.value = DlnaDeniedSendersPreference.getAsync(context).map { decodeSenderEntry(it) }
        }
    }

    fun removeAllowed(context: android.content.Context, ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DlnaAllowedSendersPreference.removeAsync(context, ip)
            allowedFlow.value = allowedFlow.value.filter { it.first != ip }
        }
    }

    fun removeDenied(context: android.content.Context, ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DlnaDeniedSendersPreference.removeAsync(context, ip)
            deniedFlow.value = deniedFlow.value.filter { it.first != ip }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaCastHistoryPage(
    navController: NavHostController,
    vm: DlnaCastRulesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val allowed by vm.allowedFlow.collectAsState()
    val denied by vm.deniedFlow.collectAsState()

    LaunchedEffect(Unit) { vm.load(context) }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.dlna_cast_history))
        },
    ) { paddingValues ->
        if (allowed.isEmpty() && denied.isEmpty()) {
            NoDataColumn()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
            ) {
                item { TopSpace() }
                if (allowed.isNotEmpty()) {
                    item { Subtitle(text = stringResource(R.string.dlna_cast_accepted)) }
                    items(allowed, key = { it.first }) { (ip, name) ->
                        PCard {
                            PListItem(
                                title = if (name.isNotEmpty() && name != ip) "$name ($ip)" else ip,
                                action = {
                                    IconButton(onClick = { vm.removeAllowed(context, ip) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.trash_2),
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                            )
                        }
                        VerticalSpace(8.dp)
                    }
                }
                if (denied.isNotEmpty()) {
                    item { VerticalSpace(8.dp) }
                    item { Subtitle(text = stringResource(R.string.dlna_cast_rejected)) }
                    items(denied, key = { it.first }) { (ip, name) ->
                        PCard {
                            PListItem(
                                title = if (name.isNotEmpty() && name != ip) "$name ($ip)" else ip,
                                action = {
                                    IconButton(onClick = { vm.removeDenied(context, ip) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.trash_2),
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                            )
                        }
                        VerticalSpace(8.dp)
                    }
                }
                item { BottomSpace() }
            }
        }
    }
}


