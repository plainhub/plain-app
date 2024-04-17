package com.ismartcoding.plain.helpers

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DTextLine
import java.io.BufferedReader
import java.io.FileReader

object TextFileHelper {
    private const val BUFFER_SIZE = 8192 // 8 KB (adjust as needed)
    private val LINE_SEPARATOR_REGEX = "\r\n|\r|\n".toRegex()

    fun countLinesInFile(path: String): Int {
        var lineCount = 0
        try {
            BufferedReader(FileReader(path)).use { reader ->
                var nextChar = -1
                var prevChar = -1
                while (reader.read().also { nextChar = it } != -1) {
                    if (nextChar == '\n'.code || nextChar == '\r'.code) {
                        // Check for both newline and carriage return
                        if (prevChar != '\r'.code) {
                            lineCount++
                        }
                    }
                    prevChar = nextChar
                }

                if (prevChar != -1) {
                    if (prevChar == '\n'.code || lineCount == 0) {
                        lineCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e(e.toString())
        }
        return lineCount
    }

    fun readLines(path: String, lineCount: Int, offset: Int, limit: Int): List<DTextLine> {
        val lines = mutableListOf<DTextLine>()
        var lineNumber = 0
        var linesRemaining = limit

        try {
            BufferedReader(FileReader(path)).use { reader ->
                var line: String? = null
                while (reader.readLine().also { line = it  } != null) {
                    lineNumber++
                    if (lineNumber > offset) {
                        lines.add(DTextLine(lineNumber - 1, line!!))
                        linesRemaining--
                    }

                    // If all desired lines have been found, exit the loop
                    if (linesRemaining <= 0) {
                        break
                    }
                }
            }
            if (lines.size < limit) {
                val left = lineCount % limit
                if (lines.size < left) {
                    lines.add(DTextLine(lineNumber, ""))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e(e.toString())
        }

        return lines
    }

    fun readText(path: String, callback: (String) -> Unit) {
        var bufferedReader: BufferedReader? = null
        try {
            bufferedReader = BufferedReader(FileReader(path), BUFFER_SIZE)

            var bytesRead: Int
            val buffer = CharArray(BUFFER_SIZE)
            bytesRead = bufferedReader.read(buffer, 0, BUFFER_SIZE)

            while (bytesRead != -1) {
                callback(String(buffer, 0, bytesRead))
                bytesRead = bufferedReader.read(buffer, 0, BUFFER_SIZE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e(e.toString())
        } finally {
            bufferedReader?.close()
        }
    }
}