package com.ismartcoding.plain.ui.page.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.preference.ExchangeConfig
import com.ismartcoding.plain.preference.ExchangeRatePreference
import com.ismartcoding.plain.preference.ExchangeRateProvider
import com.ismartcoding.plain.preference.LocalExchangeRate
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.DExchangeRate
import com.ismartcoding.plain.helpers.ExchangeHelper
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.exchange.SelectCurrencyDialog
import com.ismartcoding.plain.ui.helpers.ResourceHelper
import kotlinx.coroutines.launch

data class RateItem(val rate: DExchangeRate, val value: Double)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExchangeRatePage(navController: NavHostController) {
    ExchangeRateProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var updatedTs by remember { mutableLongStateOf(0L) }
        var rateItems by remember { mutableStateOf<List<RateItem>?>(null) }
        val config = LocalExchangeRate.current
        var editValueDialogVisible by remember { mutableStateOf(false) }
        var selectedItem by remember { mutableStateOf<DExchangeRate?>(null) }
        var editValue by remember { mutableStateOf("") }
        val showContextMenu = remember { mutableStateOf(false) }

        val refreshState =
            rememberRefreshLayoutState {
                scope.launch {
                    val r = withIO { ExchangeHelper.getRates() }
                    if (r != null) {
                        updatedTs = System.currentTimeMillis()
                    }
                    setRefreshState(RefreshContentState.Finished)
                }
            }

        LaunchedEffect(updatedTs) {
            rateItems = getItems(config)
        }

        LaunchedEffect(config) {
            val data = UIDataCache.current().latestExchangeRates
            if (data != null) {
                updatedTs = System.currentTimeMillis()
            } else {
                refreshState.setRefreshState(RefreshContentState.Refreshing)
            }
        }

        PScaffold(
            topBar = {
                PTopAppBar(navController = navController, title = "", actions = {
                    if (rateItems != null) {
                        PIconButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.add),
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                SelectCurrencyDialog { rate ->
                                    scope.launch {
                                        val selected = config.selected
                                        if (!selected.contains(rate.currency)) {
                                            selected.add(rate.currency)
                                            withIO { ExchangeRatePreference.putAsync(context, config) }
                                            updatedTs = System.currentTimeMillis()
                                        }
                                    }
                                }.show()
                            },
                        )
                    }
                })
            },
            content = {
                PullToRefresh(refreshLayoutState = refreshState) {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        item {
                            TopSpace()
                            DisplayText(
                                title = stringResource(id = R.string.exchange_rate),
                                description = if (rateItems != null) stringResource(R.string.date) + " " + UIDataCache.current().latestExchangeRates?.date?.formatDateTime() else "",
                            )
                        }
                        item {
                            rateItems?.forEach { rate ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    PListItem(
                                        modifier = Modifier.combinedClickable(onClick = {
                                            selectedItem = rate.rate
                                            editValue = FormatHelper.formatDouble(rate.value, isGroupingUsed = false)
                                            editValueDialogVisible = true
                                        }, onLongClick = {
                                            selectedItem = rate.rate
                                            showContextMenu.value = true
                                        }),
                                        title = rate.rate.currency,
                                        value = FormatHelper.formatMoney(rate.value, rate.rate.currency),
                                        icon =
                                        painterResource(
                                            id = ResourceHelper.getCurrencyFlagResId(context, rate.rate.currency),
                                        ),
                                    )
                                    Box(
                                        modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(top = 32.dp)
                                            .wrapContentSize(Alignment.Center),
                                    ) {
                                        PDropdownMenu(
                                            expanded = showContextMenu.value && selectedItem == rate.rate,
                                            onDismissRequest = { showContextMenu.value = false },
                                        ) {
                                            PDropdownMenuItem(text = { Text(stringResource(id = R.string.delete)) }, onClick = {
                                                scope.launch {
                                                    showContextMenu.value = false
                                                    val selected = config.selected
                                                    selected.remove(rate.rate.currency)
                                                    withIO {
                                                        ExchangeRatePreference.putAsync(context, config)
                                                    }
                                                    updatedTs = System.currentTimeMillis()
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            BottomSpace()
                        }
                    }
                }
                if (editValueDialogVisible) {
                    TextFieldDialog(
                        title = selectedItem?.currency ?: "",
                        value = editValue,
                        placeholder = "",
                        onValueChange = {
                            editValue = it
                        },
                        onDismissRequest = {
                            editValueDialogVisible = false
                        },
                        keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        onConfirm = {
                            scope.launch {
                                config.base = selectedItem!!.currency
                                config.value = editValue.toDoubleOrNull() ?: 100.0
                                withIO { ExchangeRatePreference.putAsync(context, config) }
                                updatedTs = System.currentTimeMillis()
                                editValueDialogVisible = false
                            }
                        },
                    )
                }
            },
        )
    }
}

fun getItems(config: ExchangeConfig): List<RateItem>? {
    val items = mutableListOf<RateItem>()
    val data = UIDataCache.current().latestExchangeRates
    if (data != null) {
        val baseRate = data.getBaseRate(config.base)
        data.rates.forEach {
            if (config.selected.contains(it.currency)) {
                items.add(RateItem(it, config.value * it.rate / baseRate))
            }
        }
        return items
    }

    return null
}
