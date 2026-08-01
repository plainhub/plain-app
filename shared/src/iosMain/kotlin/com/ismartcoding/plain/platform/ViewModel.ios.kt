package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass

@Composable
actual fun <T : ViewModel> rememberViewModel(type: KClass<T>, factory: () -> T): T = remember { factory() }
