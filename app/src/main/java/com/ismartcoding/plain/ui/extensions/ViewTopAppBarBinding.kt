package com.ismartcoding.plain.ui.extensions

import android.annotation.SuppressLint
import android.view.View
import android.view.Window
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.gyf.immersionbar.ImmersionBar
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.ViewTopAppBarBinding
import com.ismartcoding.plain.features.box.FetchInitDataEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.ui.app.HttpServerDialog
import com.ismartcoding.plain.ui.scan.ScanDialog
import kotlin.math.abs

fun ViewTopAppBarBinding.refreshUI() {
    val context = layout.context
    layout.setBackgroundColor(context.getColor(R.color.canvas))
    toolbar.run {
        findViewById<View>(R.id.refresh)?.actionBarItemBackground()
        findViewById<View>(R.id.web)?.actionBarItemBackground()
        findViewById<View>(R.id.scan)?.actionBarItemBackground()
    }
    val primary = context.getColor(R.color.primary)
    customTitle.setTextColor(primary)
    customTitle.setDrawableColor(primary)
    customSubtitle.setTextColor(context.getColor(R.color.secondary))
}

@SuppressLint("CheckResult")
fun ViewTopAppBarBinding.mainRefresh() {
    val box = UIDataCache.current().box
    toolbar.run {
        navigationIcon = null

        initMenu(R.menu.main)

        menu.findItem(R.id.refresh)?.isVisible = box != null
        menu.findItem(R.id.keep_screen_on)?.isChecked = LocalStorage.keepScreenOn
        menu.findItem(R.id.web)?.setIcon(if (LocalStorage.webConsoleEnabled) R.drawable.ic_pc else R.drawable.ic_pc_off)

        onMenuItemClick {
            when (itemId) {
                R.id.refresh -> {
                    customSubtitle.isVisible = true
                    customSubtitle.text = getString(R.string.syncing_data_from_box)
                    sendEvent(FetchInitDataEvent.createDefault())
                }
                R.id.web -> {
                    HttpServerDialog().show()
                }
                R.id.scan -> {
                    ScanDialog().show()
                }
                R.id.keep_screen_on -> {
                    val enable = !isChecked
                    if (ScreenHelper.keepScreenOn(enable)) {
                        this.isChecked = enable
                    }
                }
            }
        }
    }
    customSubtitle.isVisible = false
    customTitle.run {
        isVisible = true
//        if (BuildConfig.DEBUG) {
//            text = box?.name ?: getString(R.string.select_box_title)
//            setSafeClick {
//                SelectBoxDialog().show()
//            }
//        } else {
        setCompoundDrawables(null, null, null, null)
        text = getString(R.string.app_name)
        //   }
    }

    notification.run {
        if (UIDataCache.current().boxNetworkReachable == false) {
            text = when (UIDataCache.current().boxBluetoothReachable) {
                null -> {
                    setBackgroundColor(context.getColor(R.color.red))
                    LocaleHelper.getStringF(R.string.unreachable_via_network, "box_name", box?.name ?: "")
                }
                true -> {
                    setBackgroundColor(context.getColor(R.color.yellow))
                    LocaleHelper.getStringF(R.string.unreachable_via_network_except_bluetooth, "box_name", box?.name ?: "")
                }
                else -> {
                    setBackgroundColor(context.getColor(R.color.red))
                    LocaleHelper.getStringF(R.string.unreachable_via_network_and_bluetooth, "box_name", box?.name ?: "")
                }
            }
            isVisible = true
        } else {
            isVisible = false
        }
    }
}

fun ViewTopAppBarBinding.autoStatusBar(window: Window?) {
    layout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
        if (abs(verticalOffset) == appBarLayout.totalScrollRange) {
            window?.let {
                ImmersionBar.hideStatusBar(it)
            }
        } else if (verticalOffset == 0) {
            window?.let {
                ImmersionBar.showStatusBar(it)
            }
        }
    }
}

fun ViewTopAppBarBinding.setScrollBehavior(enabled: Boolean) {
    val flags = if (enabled) {
        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
    } else 0
    quickNav.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
    toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
    progressBar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
    notification.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
}

fun ViewTopAppBarBinding.hideNotification() {
    notification.isVisible = false
}

fun ViewTopAppBarBinding.showNotification(text: String, @ColorRes colorId: Int) {
    notification.isVisible = true
    notification.setBackgroundColor(notification.context.getColor(colorId))
    notification.text = text
}
