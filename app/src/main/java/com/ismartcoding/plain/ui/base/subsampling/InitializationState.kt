package com.ismartcoding.plain.ui.base.subsampling

internal sealed class InitializationState {
  object Uninitialized : InitializationState()
  data class Error(val exception: Throwable) : InitializationState()
  object Success : InitializationState()
}