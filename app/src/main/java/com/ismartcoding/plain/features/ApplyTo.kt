package com.ismartcoding.plain.features

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.device.getName
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.fragment.DeviceFragment
import com.ismartcoding.plain.fragment.NetworkFragment

enum class ApplyToType(val value: String) {
    ALL("all"),
    DEVICE("mac"),
    TAG("tag"),
    INTERFACE("iface"),
    ;

    companion object {
        fun parse(
            value: String,
            default: ApplyToType = ALL,
        ): ApplyToType {
            return values().find { it.value == value } ?: default
        }
    }
}

data class ApplyTo(var type: ApplyToType = ApplyToType.ALL, var value: String = "") {
    fun getText(
        devices: List<DeviceFragment>,
        networks: List<NetworkFragment>,
    ): String {
        return when (type) {
            ApplyToType.ALL -> {
                getString(R.string.all_devices)
            }
            ApplyToType.DEVICE -> {
                devices.find { it.mac == value }?.getName() ?: value
            }
            ApplyToType.INTERFACE -> {
                networks.find { it.ifName == value }?.name ?: value
            }
            else -> ""
        }
    }

    fun toValue(): String {
        if (type == ApplyToType.ALL) {
            return type.value
        }

        return "${type.value}:$value"
    }

    companion object {
        fun parse(applyTo: String): ApplyTo {
            val r = ApplyTo()
            val split = applyTo.split(":", limit = 2)
            r.type = ApplyToType.parse(split[0])
            if (split.size > 1) {
                r.value = split[1]
            }

            return r
        }
    }
}
