package com.ismartcoding.plain.ui.home.views

import android.content.Context
import android.text.InputType
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.HomeItemExchangeBinding
import com.ismartcoding.plain.databinding.ItemRowBinding
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.exchange.DExchangeRate
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.exchange.SelectCurrencyDialog
import com.ismartcoding.plain.ui.extensions.initTheme
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setEndIcon
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.ResourceHelper
import com.ismartcoding.plain.ui.models.ListItemModel

data class RateModel(val rate: DExchangeRate, val value: Double) : ListItemModel()

fun HomeItemExchangeBinding.bindData(context: Context) {
    val latestExchangeRates = UIDataCache.current().latestExchangeRates
    if (latestExchangeRates == null) {
        sendEvent(FetchLatestExchangeRatesEvent())
        state.showLoading()
    }
    updateUI(context)
}

fun HomeItemExchangeBinding.initView() {
    rv.linear().setup {
        addType<RateModel>(R.layout.item_row)
        onBind {
            val b = getBinding<ItemRowBinding>()
            b.initTheme()
        }
        R.id.container.onClick {
            val m = getModel<RateModel>()
            val rate = m.rate
            EditValueDialog(
                rate.currency, LocaleHelper.getString(R.string.value),
                FormatHelper.formatDouble(m.value, isGroupingUsed = false),
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) {
                dismiss()
                val a = LocalStorage.exchange
                a.base = rate.currency
                a.value = binding.value.text.toDoubleOrNull() ?: 100.0
                LocalStorage.exchange = a
                sendEvent(UpdateHomeItemEvent(HomeItemType.EXCHANGE))
                updateUI(requireContext())
            }.show()
        }
    }
}

fun HomeItemExchangeBinding.updateUI(context: Context) {
    title.setTextColor(title.context.getColor(R.color.primary))
    UIDataCache.current().latestExchangeRates?.let { exchangeRates ->
        this.subtitle
            .initTheme()
            .setKeyText(LocaleHelper.getString(R.string.date) + " " + exchangeRates.date)
            .setEndIcon(R.drawable.ic_add)
            .setClick {
                SelectCurrencyDialog { rate ->
                    val config = LocalStorage.exchange
                    if (!config.selected.contains(rate.currency)) {
                        config.selected.add(rate.currency)
                        LocalStorage.exchange = config
                        sendEvent(UpdateHomeItemEvent(HomeItemType.EXCHANGE))
                    }
                }.show()
            }
        val config = LocalStorage.exchange
        val baseRate = exchangeRates.getBaseRate(config.base)
        val selected = config.selected
        rv.models = exchangeRates.rates.filter {
            selected.contains(it.currency)
        }.map {
            RateModel(it, config.value * it.rate / baseRate).apply {
                startIconId = ResourceHelper.getCurrencyFlagResId(context, it.currency)
                keyText = it.currency
                swipeEnable = true
                rightSwipeText = LocaleHelper.getString(R.string.delete)
                rightSwipeClick = {
                    DialogHelper.confirmToAction(context, R.string.confirm_to_delete) {
                        val c = LocalStorage.exchange
                        c.selected.remove(rate.currency)
                        LocalStorage.exchange = c
                        sendEvent(UpdateHomeItemEvent(HomeItemType.EXCHANGE))
                    }
                }
                valueText = FormatHelper.formatMoney(value, it.currency)
            }
        }
    }
}

