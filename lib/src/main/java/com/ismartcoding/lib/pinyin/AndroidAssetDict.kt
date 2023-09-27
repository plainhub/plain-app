package com.ismartcoding.lib.pinyin

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 从Asset中的文本文件构建词典的辅助类
 *
 * 词典格式为：每行一个词和对应的拼音，拼音在前，词在后，空格分隔，拼音间以'分隔
 * 例：  CHONG'QING 重庆
 *
 */
abstract class AndroidAssetDict(context: Context) : PinyinMapDict() {
    /**
     * 返回Asset中存储词典信息的文本文档的路径，必须非空
     *
     * @return
     */
    protected abstract fun assetFileName(): String?

    val mContext: Context
    val mDict: MutableMap<String, Array<String>> = hashMapOf()

    override fun mapping(): Map<String, Array<String>> {
        return mDict
    }

    private fun init() {
        var reader: BufferedReader? = null
        try {
            this.javaClass.classLoader?.getResourceAsStream(assetFileName())?.use { s ->
                reader = BufferedReader(InputStreamReader(s, "utf-8"))
                var line: String
                while (reader!!.readLine().also { line = it } != null) {
                    // process the line.
                    val keyAndValue = line.split("\\s+").toTypedArray()
                    if (keyAndValue.size == 2) {
                        val pinyinStrs = keyAndValue[0].split("'").toTypedArray()
                        mDict[keyAndValue[1]] = pinyinStrs
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    init {
        mContext = context.applicationContext
        init()
    }
}
