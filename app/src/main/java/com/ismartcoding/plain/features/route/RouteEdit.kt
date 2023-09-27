package com.ismartcoding.plain.features.route

import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.data.enums.ConfigType
import com.ismartcoding.plain.features.ApplyTo
import com.ismartcoding.plain.features.Target
import com.ismartcoding.plain.features.TargetType
import com.ismartcoding.plain.type.ConfigInput

data class RouteEdit(
    var applyTo: ApplyTo,
    var target: Target,
    var gateway: String,
    var ifName: String,
    var isEnabled: Boolean,
    var notes: String,
) {
    fun toRouteInput(): ConfigInput {
        return ConfigInput(
            ConfigType.ROUTE.value,
            jsonEncode(
                RouteInput(
                    applyTo.toValue(),
                    target.toValue(),
                    gateway,
                    ifName,
                    isEnabled,
                    notes,
                ),
            ),
        )
    }

    fun ifDisplayName(): String {
        return UIDataCache.current().getNetworks().find { it.ifName == ifName }?.name ?: ifName
    }

    companion object {
        fun createDefault(): RouteEdit {
            return RouteEdit(
                ApplyTo(),
                Target(TargetType.INTERNET),
                "",
                "",
                true,
                "",
            )
        }
    }
}
