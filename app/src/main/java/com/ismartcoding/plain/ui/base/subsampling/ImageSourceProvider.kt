package com.ismartcoding.plain.ui.base.subsampling


interface ImageSourceProvider {
  suspend fun provide(): Result<ComposeSubsamplingScaleImageSource>
}