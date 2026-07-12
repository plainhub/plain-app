package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.graphics.Color

internal val Color.hexCode: String
  inline get() {
    val a: Int = (alpha * 255).toInt()
    val r: Int = (red * 255).toInt()
    val g: Int = (green * 255).toInt()
    val b: Int = (blue * 255).toInt()
    return buildString {
      appendHexByte(a)
      appendHexByte(r)
      appendHexByte(g)
      appendHexByte(b)
    }
  }

private fun StringBuilder.appendHexByte(value: Int) {
  val v = value and 0xFF
  append(HEX_CHARS[v ushr 4])
  append(HEX_CHARS[v and 0x0F])
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
