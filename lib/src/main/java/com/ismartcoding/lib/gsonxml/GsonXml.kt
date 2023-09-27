package com.ismartcoding.lib.gsonxml

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.Primitives
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.MalformedJsonException
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.lang.reflect.Type

class GsonXml internal constructor(val gson: Gson, val options: XmlReader.Options) {
    @Throws(JsonSyntaxException::class)
    fun <T> fromXml(
        json: String,
        classOfT: Class<T>,
    ): T {
        val `object` = fromXml<Any>(json, classOfT as Type)
        return Primitives.wrap(classOfT).cast(`object`)
    }

    @Throws(JsonSyntaxException::class)
    fun <T> fromXml(
        json: String,
        typeOfT: Type,
    ): T {
        val reader = StringReader(json)
        return fromXml<Any>(reader, typeOfT) as T
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    fun <T> fromXml(
        json: Reader,
        typeOfT: Type,
    ): T {
        val jsonReader = XmlReader(json, options) // change reader
        val `object` = fromXml<Any>(jsonReader, typeOfT)
        assertFullConsumption(`object`, jsonReader)
        return `object` as T
    }

    /**
     * Reads the next JSON value from `reader` and convert it to an object
     * of type `typeOfT`.
     * Since Type is not parameterized by T, this method is type unsafe and should be used carefully
     *
     * @return deserialized object
     * @param <T> type to deserialize
     * @param reader XML source
     * @param typeOfT type to deserialize
     * @throws JsonIOException if there was a problem writing to the Reader
     * @throws JsonSyntaxException if json is not a valid representation for an object of type
     </T> */
    @Throws(JsonIOException::class, JsonSyntaxException::class)
    fun <T> fromXml(
        reader: XmlReader,
        typeOfT: Type,
    ): T {
        return gson.fromJson(reader, typeOfT)
    }

    override fun toString(): String {
        return gson.toString()
    }

    companion object {
        private fun assertFullConsumption(
            obj: Any?,
            reader: JsonReader,
        ) {
            try {
                if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
                    throw JsonIOException("JSON document was not fully consumed.")
                }
            } catch (e: MalformedJsonException) {
                throw JsonSyntaxException(e)
            } catch (e: IOException) {
                throw JsonIOException(e)
            }
        }
    }
}
