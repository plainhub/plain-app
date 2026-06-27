package com.ismartcoding.plain.lib.extensions

import android.content.Intent
import android.os.Parcelable
import com.ismartcoding.plain.lib.isTPlus

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? =
    when {
        isTPlus() -> getParcelableExtra(key, T::class.java)
        else ->
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
                as? T
    }

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? =
    when {
        isTPlus() -> getParcelableArrayListExtra(key, T::class.java)
        else ->
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra<T>(key)
    }
