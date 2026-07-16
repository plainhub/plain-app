package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.helpers.DeviceInfoHelper

actual fun getDeviceInfo(): DDeviceInfo = DeviceInfoHelper.getDeviceInfo(appContext)
