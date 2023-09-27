package com.ismartcoding.plain.features.rule

enum class RuleProtocol(val value: String) {
    ALL("all"),
    TCP("tcp"),
    UDP("udp"),
    ICMP("icmp"),
    ;

    companion object {
        fun parse(
            value: String,
            default: RuleProtocol = ALL,
        ): RuleProtocol {
            return values().find { it.value == value } ?: default
        }
    }
}
