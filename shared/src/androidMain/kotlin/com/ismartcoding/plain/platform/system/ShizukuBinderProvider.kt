package com.ismartcoding.plain.platform.system

import android.os.Bundle
import rikka.shizuku.ShizukuProvider

class ShizukuBinderProvider : ShizukuProvider() {
    @Synchronized
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = super.call(method, arg, extras)
}
