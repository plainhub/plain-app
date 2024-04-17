package com.ismartcoding.plain.ui.extensions

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ViewPageListBinding
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.RequestPermissionsEvent
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

fun ViewPageListBinding.checkPermission(
    context: Context,
    featureType: AppFeatureType
) {
    val p = featureType.getPermission() ?: return
    if (Permissions.allCan(context, p.permissions)) {
        page.visibility = View.VISIBLE
        empty.root.isVisible = false
        page.showLoading()
    } else {
        page.visibility = View.GONE
        empty.run {
            text.text = p.permission.getGrantAccessText()
            root.isVisible = true
            button.text = getString(R.string.grant_access)
            button.isVisible = true
            button.setSafeClick {
                sendEvent(RequestPermissionsEvent(*p.permissions.toTypedArray()))
            }
        }
    }
}
