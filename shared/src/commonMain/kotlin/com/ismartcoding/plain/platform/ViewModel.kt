package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass

/**
 * Returns a [ViewModel] scoped to the current composable. On Android this is
 * backed by [androidx.lifecycle.ViewModelProvider] (tied to the current
 * ViewModelStoreOwner, surviving configuration changes); on iOS by
 * [androidx.compose.runtime.remember] (process-lifetime cache).
 *
 * @param type   the ViewModel class; used as the provider key on Android
 * @param factory creates a fresh instance when the store has no cached entry
 */
@Composable
expect fun <T : ViewModel> rememberViewModel(type: KClass<T>, factory: () -> T): T

