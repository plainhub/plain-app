package com.ismartcoding.lib.serialize

import android.os.Parcelable
import com.tencent.mmkv.MMKV
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun MMKV.serialize(vararg params: Pair<String, Any?>) {
    params.forEach {
        when (val value = it.second) {
            null -> remove(it.first)
            is Parcelable -> encode(it.first, value)
            else -> encode(it.first, value)
        }
        return@forEach
    }
}


@PublishedApi
internal fun <T> MMKV.deserialize(name: String, clazz: Class<T>): T {
    return when {
        Parcelable::class.java.isAssignableFrom(clazz) -> {
            decodeParcelable(name, clazz as Class<Parcelable>) as? T
        }
        else -> decode<T>(name)
    } ?: null as T
}

@PublishedApi
internal fun <T> MMKV.deserialize(name: String, clazz: Class<T>, defValue: T?): T {
    return when {
        Parcelable::class.java.isAssignableFrom(clazz) -> {
            decodeParcelable(
                name, clazz as Class<Parcelable>,
                defValue as Parcelable
            ) as? T
        }
        else -> decode(name, defValue)
    } ?: null as T
}

private fun MMKV.encode(name: String, obj: Any?) {
    if (obj == null) {
        remove(name)
        return
    }
    try {
        ByteArrayOutputStream().use { byteOutput ->
            ObjectOutputStream(byteOutput).use { objOutput ->
                objOutput.writeObject(obj)
                encode(name, byteOutput.toByteArray())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun <T> MMKV.decode(name: String): T? {
    val bytes = decodeBytes(name) ?: return null
    return try {
        var obj: Any?
        ByteArrayInputStream(bytes).use { byteInput ->
            ObjectInputStream(byteInput).use { objInput ->
                obj = objInput.readObject()
            }
        }
        obj as? T
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun <T> MMKV.decode(name: String, defValue: T): T {
    val bytes = decodeBytes(name) ?: return defValue
    return try {
        var obj: Any?
        ByteArrayInputStream(bytes).use { byteInput ->
            ObjectInputStream(byteInput).use { objInput ->
                obj = objInput.readObject()
            }
        }
        obj as T
    } catch (e: Exception) {
        defValue
    }
}
