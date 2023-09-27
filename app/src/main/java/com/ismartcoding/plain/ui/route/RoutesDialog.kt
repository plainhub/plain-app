package com.ismartcoding.plain.ui.route

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.DeleteConfigMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.databinding.DialogRoutesBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.box.FetchNetworksEvent
import com.ismartcoding.plain.features.box.NetworksResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.route.Route
import com.ismartcoding.plain.features.route.bindRoute
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class RoutesDialog : BaseDialog<DialogRoutesBinding>() {
    private var searchQ: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.rv.linear().setup {
            addType<Route>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<Route>()
                binding.bindRoute(requireContext(), lifecycleScope, m)
                binding.enableSwipeMenu(true)
                binding.setRightSwipeButton(getString(R.string.delete)) {
                    DialogHelper.confirmToAction(requireContext(), R.string.confirm_to_delete) {
                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            val r = withIO { BoxApi.mixMutateAsync(DeleteConfigMutation(m.id)) }
                            DialogHelper.hideLoading()
                            if (!r.isSuccess()) {
                                DialogHelper.showErrorDialog(requireContext(), r.getErrorMessage())
                                return@launch
                            }

                            UIDataCache.current().routes?.removeAll { it.id == m.id }
                            sendEvent(ActionEvent(ActionSourceType.ROUTE, ActionType.DELETED, setOf(m.id)))
                        }
                    }
                }
            }
        }

        binding.topAppBar.setScrollBehavior(false)
        binding.topAppBar.toolbar.run {
            setTitle(R.string.routes)
            initMenu(R.menu.routes)
            onBack {
                dismiss()
            }

            onSearch { q ->
                if (searchQ != q) {
                    searchQ = q
                    search()
                }
            }

            onSearchFocusChange { hasFocus ->
                menu.findItem(R.id.more).isVisible = !hasFocus
            }
        }

        binding.list.page.onRefresh {
            fetch()
        }

        binding.fab.setSafeClick {
            RouteDialog(null).show()
        }

        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.ROUTE) {
                search()
            }
        }

        receiveEvent<NetworksResultEvent> { event ->
            val r = event.result
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.list.page.finishRefresh(false)
                binding.list.page.showError()
                return@receiveEvent
            }
            search()
        }

        if (UIDataCache.current().routes == null) {
            binding.list.page.showLoading()
        } else {
            search()
        }
    }

    private fun search() {
        binding.list.page.replaceData(UIDataCache.current().getRoutes(searchQ))
        binding.topAppBar.toolbar.title = LocaleHelper.getStringF(R.string.routes_title, "total", UIDataCache.current().getRoutes(searchQ).size.toString())
    }

    private fun fetch() {
        sendEvent(FetchNetworksEvent(TempData.selectedBoxId))
    }
}
