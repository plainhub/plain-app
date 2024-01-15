package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.utils.Unsigned

class ResTableConfig {
    //uint32_t screenConfig;
    // Number of bytes in this structure. uint32_t
    var size = 0
        set(value) {
            field = Unsigned.ensureUInt(value.toLong())
        }

    // Mobile country code (from SIM).  0 means "any". uint16_t
    var mcc: Short = 0

    // Mobile network code (from SIM).  0 means "any". uint16_t
    var mnc: Short = 0

    //uint32_t imsi;
    // 0 means "any".  Otherwise, en, fr, etc. char[2]
    lateinit var language: String

    // 0 means "any".  Otherwise, US, CA, etc.  char[2]
    lateinit var country: String

    // uint32_t locale;
    // uint8_t
    var orientation: Byte = 0
        get() = (field.toInt() and 0xff).toByte()

    // uint8_t
    var touchscreen: Byte = 0
        get() = (field.toInt() and 0xff).toByte()

    // uint16_t
    var density: Short = 0
        get() = (field.toInt() and 0xffff).toShort()

    // uint32_t screenType;
    // uint8_t
    var keyboard: Short = 0

    // uint8_t
    var navigation: Short = 0

    // uint8_t
    var inputFlags: Short = 0

    // uint8_t
    var inputPad0: Short = 0

    // uint32_t input;
    // uint16_t
    var screenWidth = 0

    // uint16_t
    var screenHeight = 0

    // uint32_t screenSize;
    // uint16_t
    var sdkVersion = 0

    // For now minorVersion must always be 0!!!  Its meaning is currently undefined.
    // uint16_t
    var minorVersion = 0

    //uint32_t version;
    // uint8_t
    var screenLayout: Short = 0

    // uint8_t
    var uiMode: Short = 0

    // uint8_t
    var screenConfigPad1: Short = 0

    // uint8_t
    var screenConfigPad2: Short = 0
}