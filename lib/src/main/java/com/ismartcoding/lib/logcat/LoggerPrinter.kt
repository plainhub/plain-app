package com.ismartcoding.lib.logcat

import com.ismartcoding.lib.logcat.LogCat.ASSERT
import com.ismartcoding.lib.logcat.LogCat.DEBUG
import com.ismartcoding.lib.logcat.LogCat.ERROR
import com.ismartcoding.lib.logcat.LogCat.INFO
import com.ismartcoding.lib.logcat.LogCat.VERBOSE
import com.ismartcoding.lib.logcat.LogCat.WARN
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.StringReader
import java.io.StringWriter
import java.util.ArrayList
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

internal class LoggerPrinter : Printer {
    /**
     * Provides one-time used tag for the log message
     */
    private val localTag = ThreadLocal<String>()
    private val logAdapters: MutableList<LogAdapter> = ArrayList()

    override fun t(tag: String?): Printer {
        if (tag != null) {
            localTag.set(tag)
        }
        return this
    }

    override fun d(
        message: String,
        vararg args: Any?,
    ) {
        log(DEBUG, null, message, args)
    }

    override fun d(`object`: Any?) {
        log(DEBUG, null, Utils.toString(`object`))
    }

    override fun e(
        message: String,
        vararg args: Any?,
    ) {
        e(null, message, *args)
    }

    override fun e(
        throwable: Throwable?,
        message: String,
        vararg args: Any?,
    ) {
        log(ERROR, throwable, message, args)
    }

    override fun w(
        message: String,
        vararg args: Any?,
    ) {
        log(WARN, null, message, args)
    }

    override fun i(
        message: String,
        vararg args: Any?,
    ) {
        log(INFO, null, message, args)
    }

    override fun v(
        message: String,
        vararg args: Any?,
    ) {
        log(VERBOSE, null, message, args)
    }

    override fun wtf(
        message: String,
        vararg args: Any?,
    ) {
        log(ASSERT, null, message, args)
    }

    override fun json(json: String?) {
        var newJson = json
        if (Utils.isEmpty(newJson)) {
            d("Empty/Null json content")
            return
        }
        try {
            newJson = newJson!!.trim { it <= ' ' }
            if (newJson.startsWith("{")) {
                val jsonObject = JSONObject(newJson)
                val message = jsonObject.toString(JSON_INDENT)
                d(message)
                return
            }
            if (newJson.startsWith("[")) {
                val jsonArray = JSONArray(newJson)
                val message = jsonArray.toString(JSON_INDENT)
                d(message)
                return
            }
            e("Invalid Json")
        } catch (e: JSONException) {
            e("Invalid Json")
        }
    }

    override fun xml(xml: String?) {
        if (Utils.isEmpty(xml)) {
            d("Empty/Null xml content")
            return
        }
        try {
            val xmlInput: Source = StreamSource(StringReader(xml))
            val xmlOutput = StreamResult(StringWriter())
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(xmlInput, xmlOutput)
            d(xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">\n"))
        } catch (e: TransformerException) {
            e("Invalid xml")
        }
    }

    @Synchronized
    override fun log(
        priority: Int,
        tag: String?,
        message: String?,
        throwable: Throwable?,
    ) {
        var newMessage = message
        if (throwable != null && newMessage != null) {
            newMessage += " : " + Utils.getStackTraceString(throwable)
        }
        if (throwable != null && newMessage == null) {
            newMessage = Utils.getStackTraceString(throwable)
        }
        if (Utils.isEmpty(newMessage)) {
            newMessage = "Empty/NULL log message"
        }
        for (adapter in logAdapters) {
            if (adapter.isLoggable(priority, tag)) {
                adapter.log(priority, tag, newMessage!!)
            }
        }
    }

    override fun clearLogAdapters() {
        logAdapters.clear()
    }

    override fun addAdapter(adapter: LogAdapter) {
        logAdapters.add(adapter)
    }

    /**
     * This method is synchronized in order to avoid messy of logs' order.
     */
    @Synchronized
    private fun log(
        priority: Int,
        throwable: Throwable?,
        msg: String,
        vararg args: Any?,
    ) {
        log(priority, tag, createMessage(msg, args), throwable)
    }

    /**
     * @return the appropriate tag based on local or global
     */
    private val tag: String?
        get() {
            val tag = localTag.get()
            if (tag != null) {
                localTag.remove()
                return tag
            }
            return null
        }

    private fun createMessage(
        message: String,
        vararg args: Any?,
    ): String {
        return if (args.size <= 1) message else String.format(message, *args)
    }

    companion object {
        /**
         * It is used for json pretty print
         */
        private const val JSON_INDENT = 2
    }
}
