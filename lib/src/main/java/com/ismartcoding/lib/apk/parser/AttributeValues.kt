package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.utils.Strings

object AttributeValues {
    // Activity constants begin. see:
    // http://developer.android.com/reference/android/content/pm/ActivityInfo.html
    // http://developer.android.com/guide/topics/manifest/activity-element.html
    fun getScreenOrientation(value: Int): String {
        return when (value) {
            0x00000003 -> "behind"
            0x0000000a -> "fullSensor"
            0x0000000d -> "fullUser"
            0x00000000 -> "landscape"
            0x0000000e -> "locked"
            0x00000005 -> "nosensor"
            0x00000001 -> "portrait"
            0x00000008 -> "reverseLandscape"
            0x00000009 -> "reversePortrait"
            0x00000004 -> "sensor"
            0x00000006 -> "sensorLandscape"
            0x00000007 -> "sensorPortrait"
            -0x1 -> "unspecified"
            0x00000002 -> "user"
            0x0000000b -> "userLandscape"
            0x0000000c -> "userPortrait"
            else -> "ScreenOrientation:" + Integer.toHexString(value)
        }
    }

    fun getLaunchMode(value: Int): String {
        return when (value) {
            0x00000000 -> "standard"
            0x00000001 -> "singleTop"
            0x00000002 -> "singleTask"
            0x00000003 -> "singleInstance"
            else -> "LaunchMode:" + Integer.toHexString(value)
        }
    }

    fun getConfigChanges(value: Int): String? {
        val list: MutableList<String> = ArrayList()
        if (value and 0x00001000 != 0) {
            list.add("density")
        } else if (value and 0x40000000 != 0) {
            list.add("fontScale")
        } else if (value and 0x00000010 != 0) {
            list.add("keyboard")
        } else if (value and 0x00000020 != 0) {
            list.add("keyboardHidden")
        } else if (value and 0x00002000 != 0) {
            list.add("direction")
        } else if (value and 0x00000004 != 0) {
            list.add("locale")
        } else if (value and 0x00000001 != 0) {
            list.add("mcc")
        } else if (value and 0x00000002 != 0) {
            list.add("mnc")
        } else if (value and 0x00000040 != 0) {
            list.add("navigation")
        } else if (value and 0x00000080 != 0) {
            list.add("orientation")
        } else if (value and 0x00000100 != 0) {
            list.add("screenLayout")
        } else if (value and 0x00000400 != 0) {
            list.add("screenSize")
        } else if (value and 0x00000800 != 0) {
            list.add("smallestScreenSize")
        } else if (value and 0x00000008 != 0) {
            list.add("touchscreen")
        } else if (value and 0x00000200 != 0) {
            list.add("uiMode")
        }
        return Strings.join(list, "|")
    }

    fun getWindowSoftInputMode(value: Int): String? {
        val adjust = value and 0x000000f0
        val state = value and 0x0000000f
        val list: MutableList<String> = ArrayList(2)
        when (adjust) {
            0x00000030 -> list.add("adjustNothing")
            0x00000020 -> list.add("adjustPan")
            0x00000010 -> list.add("adjustResize")
            0x00000000 -> {}
            else -> list.add("WindowInputModeAdjust:" + Integer.toHexString(adjust))
        }
        when (state) {
            0x00000003 -> list.add("stateAlwaysHidden")
            0x00000005 -> list.add("stateAlwaysVisible")
            0x00000002 -> list.add("stateHidden")
            0x00000001 -> list.add("stateUnchanged")
            0x00000004 -> list.add("stateVisible")
            0x00000000 -> {}
            else -> list.add("WindowInputModeState:" + Integer.toHexString(state))
        }
        return Strings.join(list, "|")
        //isForwardNavigation(0x00000100),
        //mode_changed(0x00000200),
    }

    //http://developer.android.com/reference/android/content/pm/PermissionInfo.html
    fun getProtectionLevel(value: Int): String? {
        var newValue = value
        val levels: MutableList<String> = ArrayList(3)
        if (newValue and 0x10 != 0) {
            newValue = newValue xor 0x10
            levels.add("system")
        }
        if (newValue and 0x20 != 0) {
            newValue = newValue xor 0x20
            levels.add("development")
        }
        when (newValue) {
            0 -> levels.add("normal")
            1 -> levels.add("dangerous")
            2 -> levels.add("signature")
            3 -> levels.add("signatureOrSystem")
            else -> levels.add("ProtectionLevel:" + Integer.toHexString(newValue))
        }
        return Strings.join(levels, "|")
    }
    // Activity constants end
    /**
     * get Installation string values from int
     */
    fun getInstallLocation(value: Int): String {
        return when (value) {
            0 -> "auto"
            1 -> "internalOnly"
            2 -> "preferExternal"
            else -> "installLocation:" + Integer.toHexString(value)
        }
    }
}