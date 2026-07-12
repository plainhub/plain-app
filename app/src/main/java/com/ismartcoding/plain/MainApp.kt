package com.ismartcoding.plain

import android.app.Application

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setAppContext(this, buildChannel = BuildConfig.CHANNEL)
        MainAppHelper.init(this)
    }
}
