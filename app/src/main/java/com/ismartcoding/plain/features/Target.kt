package com.ismartcoding.plain.features

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.fragment.NetworkFragment

enum class TargetType(val value: String) {
    IP("ip"),
    NET("net"),
    DNS("dns"),
    REMOTE_PORT("remote_port"),
    INTERNET("internet"),
    INTERFACE("iface"),
    LIST("list"),
    ;

    fun getExamples(): String {
        return when {
            this == IP -> {
                getString(R.string.examples_ip)
            }
            this == NET -> {
                getString(R.string.examples_net)
            }
            this == DNS -> {
                getString(R.string.examples_dns)
            }
            else -> ""
        }
    }

    fun getPlaceholder(): String {
        return when {
            this == IP -> {
                "10.10.10.2"
            }
            this == NET -> {
                "10.10.10.0/24"
            }
            this == DNS -> {
                "example.com"
            }
            this == REMOTE_PORT -> {
                getString(R.string.placeholder_remote_port)
            }
            else -> ""
        }
    }

    fun validate(value: String): String {
        return when {
            this == IP -> {
                if (NetworkHelper.isIPWithOptionalPort(value)) {
                    return ""
                }

                return getString(R.string.invalid_value)
            }
            this == NET -> {
                if (NetworkHelper.isNetWithOptionalPort(value)) {
                    return ""
                }

                return getString(R.string.invalid_value)
            }
            this == DNS -> {
                if (NetworkHelper.isDomainWithOptionalPort(value)) {
                    return ""
                }

                return getString(R.string.invalid_value)
            }
            this == REMOTE_PORT -> {
                if (NetworkHelper.isPortOrPortRangeMultiple(value)) {
                    return ""
                }

                return getString(R.string.invalid_value)
            }
            else -> ""
        }
    }

    fun getText(): String {
        return getString("target_type_" + this.value)
    }

    companion object {
        fun parse(
            value: String,
            default: TargetType = DNS,
        ): TargetType {
            return values().find { it.value == value } ?: default
        }
    }
}

data class Target(var type: TargetType = TargetType.DNS, var value: String = "") {
    fun toValue(): String {
        if (value.isEmpty()) {
            return type.value
        }

        return "${type.value}:$value"
    }

    fun isEmpty(): Boolean {
        return type == TargetType.DNS && value.isEmpty()
    }

    fun getText(networks: List<NetworkFragment>): String {
        if (type == TargetType.INTERNET) {
            return type.getText()
        } else if (type == TargetType.INTERFACE) {
            return if (value.isEmpty()) {
                getString(R.string.all_local_networks)
            } else {
                networks.find { it.ifName == value }?.name ?: value
            }
        }

        return value
    }

    companion object {
        fun parse(target: String): Target {
            val r = Target()
            val split = target.split(":", limit = 2)
            r.type = TargetType.parse(split[0])
            if (split.size > 1) {
                r.value = split[1]
            }
            return r
        }
    }
}
