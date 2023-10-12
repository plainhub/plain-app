package com.ismartcoding.plain.ui.base.subsampling

interface ImageDecoderProvider {
  suspend fun provide(): ComposeSubsamplingScaleImageDecoder
}