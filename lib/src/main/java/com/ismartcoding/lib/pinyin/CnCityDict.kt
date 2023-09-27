package com.ismartcoding.lib.pinyin

import android.content.Context
import kotlin.jvm.Volatile

class CnCityDict(context: Context) : AndroidAssetDict(context) {
    override fun assetFileName(): String {
        return "cncity.txt"
    }

    companion object {
        @Volatile
        var singleton: CnCityDict? = null

        fun getInstance(context: Context): CnCityDict {
            if (singleton == null) {
                synchronized(CnCityDict::class.java) {
                    if (singleton == null) {
                        singleton = CnCityDict(context)
                    }
                }
            }

            return singleton!!
        }
    }
}
