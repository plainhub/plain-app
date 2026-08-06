package com.ismartcoding.plain

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

var mainActivity: AppCompatActivity? = null

// Forwarding val for existing callers that use nullable style (appContextValue ?: return).
// Source of truth lives in shared-lib (com.ismartcoding.plain.appContextOrNull).
val appContextValue: Context?
    get() = appContextOrNull