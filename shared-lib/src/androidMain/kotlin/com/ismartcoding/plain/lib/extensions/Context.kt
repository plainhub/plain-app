@file:JvmName("ContextLib")

package com.ismartcoding.plain.lib.extensions

import android.content.Context
import androidx.core.content.ContextCompat

fun <T> Context.getSystemServiceCompat(serviceClass: Class<T>): T =
    ContextCompat.getSystemService(this, serviceClass)!!