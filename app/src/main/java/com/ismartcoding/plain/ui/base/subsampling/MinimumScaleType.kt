package com.ismartcoding.plain.ui.base.subsampling

sealed class MinimumScaleType {
  object ScaleTypeCenterInside : MinimumScaleType()
  object ScaleTypeCenterCrop : MinimumScaleType()
  object ScaleTypeFitWidth : MinimumScaleType()
  object ScaleTypeFitHeight : MinimumScaleType()
  object ScaleTypeOriginalSize : MinimumScaleType()
  object ScaleTypeSmartFit : MinimumScaleType()
  object ScaleTypeCustom : MinimumScaleType()
}