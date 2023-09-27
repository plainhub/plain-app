package com.ismartcoding.plain.ui

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.upnp.UPnPController
import com.ismartcoding.lib.upnp.UPnPDevice
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.databinding.DialogScreencastBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.StartHttpServerEvent
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.models.CastViewModel
import kotlinx.coroutines.launch

class CastDialog(val items: List<IMedia>, private val singlePath: String = "") :
    BaseBottomSheetDialog<DialogScreencastBinding>() {
    private var viewModel: CastViewModel? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CastViewModel::class.java]
        sendEvent(StartHttpServerEvent())
        binding.list.rv.isNestedScrollingEnabled = false
        binding.list.rv.linear().setup {
            addType<UPnPDevice>(R.layout.view_list_item)
            onBind {
                val b = getBinding<ViewListItemBinding>()
                val m = getModel<UPnPDevice>()
                b.setKeyText(m.description?.device?.friendlyName ?: "")
                b.setClick {
                    lifecycleScope.launch {
                        CastPlayer.currentDevice = m
                        CastPlayer.items = items
                        val path = if (items.isNotEmpty()) items[0].path else singlePath
                        withIO { UPnPController.setAVTransportURIAsync(m, UrlHelper.getMediaHttpUrl(path)) }
                        CastPlayer.currentUri = path
                        if (items.size > 1) {
                            withIO {
                                if (CastPlayer.sid.isNotEmpty()) {
                                    UPnPController.renewEvent(m, CastPlayer.sid)
                                } else {
                                    CastPlayer.sid = UPnPController.subscribeEvent(m, UrlHelper.getCastCallbackUrl())
                                }
                            }
//                            if (!LocalStorage.keepScreenOn) {
//                                val type = if (path.isVideoFast()) {
//                                    getString(R.string.videos)
//                                } else {
//                                    getString(R.string.audios)
//                                }
//                                DialogHelper.confirmToAction(context, LocaleHelper.getStringF(R.string.keep_screen_on_confirm, "type", type)) {
//                                    ScreenHelper.keepScreenOn(true)
//                                }
//                            }
                        } else {
                            if (CastPlayer.sid.isNotEmpty()) {
                                withIO { UPnPController.unsubscribeEvent(m, CastPlayer.sid, UrlHelper.getCastCallbackUrl()) }
                                CastPlayer.sid = ""
                            }
                        }
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
        binding.list.page.stateLayout?.apply {
            canRetry = false
            onEmpty {
                findViewById<TextView>(R.id.msg).text = getString(R.string.no_devices_found)
            }
            onLoading {
                findViewById<TextView>(R.id.msg).text = getString(R.string.searching_devices)
            }
        }
        binding.list.page.setEnableRefresh(false)
        binding.list.page.showLoading()

        viewModel?.devices?.observe(viewLifecycleOwner) { devices ->
            binding.list.page.replaceData(devices)
        }

        countdown()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.searchAsync()
            }
        }
    }

    private fun countdown() {
        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (view != null && binding.list.rv.models == null) {
                    binding.list.page.addData(listOf())
                }
            },
            5000,
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel = null
    }
}
