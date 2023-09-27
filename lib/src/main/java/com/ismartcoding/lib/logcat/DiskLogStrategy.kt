package com.ismartcoding.lib.logcat

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.io.File
import java.io.FileWriter
import java.io.IOException

class DiskLogStrategy(val handler: Handler) : LogStrategy {
    override fun log(
        level: Int,
        tag: String?,
        message: String,
    ) {
        handler.sendMessage(handler.obtainMessage(level, message))
    }

    class WriteHandler(looper: Looper, private val folder: String, private val maxFileSize: Int) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val content = msg.obj as String
            var fileWriter: FileWriter? = null
            try {
                fileWriter = FileWriter(getLogFile(folder), true)
                fileWriter.append(content)
                fileWriter.flush()
                fileWriter.close()
            } catch (e: IOException) {
                try {
                    fileWriter?.flush()
                    fileWriter?.close()
                } catch (e1: IOException) {
                }
            }
        }

        private fun getLogFile(folderName: String): File {
            val folder = File(folderName)
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val newFile = File(folder, "latest.log")
            if (newFile.exists() && newFile.length() >= maxFileSize) {
                newFile.renameTo(File(folder, "old.log"))
            }

            return newFile
        }
    }
}
