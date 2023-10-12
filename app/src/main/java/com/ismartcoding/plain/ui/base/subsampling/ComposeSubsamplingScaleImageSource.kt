package com.ismartcoding.plain.ui.base.subsampling

import java.io.InputStream

class ComposeSubsamplingScaleImageSource(
  /**
   * If [debugKey] is specified it will be printed to the logs along with other debug information so
   * that you can easily distinguish logs if there are multiple source loaded/processed at the same
   * time (like when you use HorizontalPager)
   * */
  val debugKey: String? = null,
  val inputStream: InputStream
)