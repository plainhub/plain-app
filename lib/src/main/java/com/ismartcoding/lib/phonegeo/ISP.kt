package com.ismartcoding.lib.phonegeo

enum class ISP(val carrier: String, val code: Int) {
    CHINA_MOBILE("中国移动", 1),
    CHINA_UNICOM("中国联通", 2),
    CHINA_TELECOM("中国电信", 3),
    CHINA_UNICOM_VIRTUAL("中国联通虚拟运营商", 4),
    CHINA_TELECOM_VIRTUAL("中国电信虚拟运营商", 5),
    CHINA_MOBILE_VIRTUAL("中国移动虚拟运营商", 6),
    UNKNOWN("未知", 0),
    ;

    companion object {
        fun of(ispMark: Int): ISP {
            return entries.firstOrNull { it.code == ispMark } ?: UNKNOWN
        }
    }
}
