package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Data transfer object that includes updated color factors.
 *
 * @param color ARGB color value.
 * @param hexCode Color hex code, which represents [color] value.
 * @param fromUser Represents this event is triggered by user or not.
 */
@Immutable
public data class ColorEnvelope(
  val color: Color,
  val hexCode: String,
  val fromUser: Boolean,
)
