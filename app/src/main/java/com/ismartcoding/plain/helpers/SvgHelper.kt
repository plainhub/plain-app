package com.ismartcoding.plain.helpers

import android.util.Size
import com.caverock.androidsvg.SVG
import java.io.File

object SvgHelper {
    fun getSize(path:String): Size {
        val svg = SVG.getFromString(File(path).readText())
        return Size(svg.documentWidth.toInt(), svg.documentHeight.toInt())
    }
}