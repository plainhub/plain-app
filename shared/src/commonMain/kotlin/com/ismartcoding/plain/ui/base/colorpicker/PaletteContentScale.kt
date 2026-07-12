package com.ismartcoding.plain.ui.base.colorpicker

/** Represents a rule to apply to scale a source rectangle to be inscribed into a destination. */
public enum class PaletteContentScale {
  /**
   * Scale the source with maintaining the source's aspect ratio
   * so that both dimensions (width and height) of the source will be equal to or less than the
   * corresponding dimension of the target size.
   */
  FIT,

  /**
   * Crop ths source the corresponding dimension of the target size.
   * so that if the dimensions (width and height) source is bigger than the target size,
   * it will be cut off from the center.
   */
  CROP,

  /**
   * Scale the source with maintaining the source's aspect ratio
   * so that if both dimensions (width and height) of the source is smaller than the target size,
   * it will not be scaled.
   */
//    INSIDE,
}
