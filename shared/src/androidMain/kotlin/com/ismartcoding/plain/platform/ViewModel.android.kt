package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.reflect.KClass

@Composable
actual fun <T : ViewModel> rememberViewModel(type: KClass<T>, factory: () -> T): T {
    val owner = LocalViewModelStoreOwner.current
        ?: error("rememberViewModel requires a ViewModelStoreOwner in the composition")
    val provider = ViewModelProvider(owner.viewModelStore, object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <M : ViewModel> create(modelClass: Class<M>): M = factory() as M
    })
    @Suppress("UNCHECKED_CAST")
    return provider[type.java]
}
