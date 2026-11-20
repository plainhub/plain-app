package com.ismartcoding.plain.tests

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.util.UUID

@Serializable
internal data class ApiPeerFixture(val id: String, val key: String, val publicKey: String) {
    fun validate() {
        val uuid = UUID.fromString(id)
        check(uuid.toString() == id && id.startsWith("706c6169-") && uuid.version() == 8 && uuid.variant() == 2)
        check(Base64.decode(key, Base64.NO_WRAP).size == 32)
        check(Base64.decode(publicKey, Base64.NO_WRAP).size == 32)
    }
}

object ApiPeerFixtureDatabase {
    @JvmStatic
    fun main(args: Array<String>) {
        check(args.size == 1)
        val database = File(args.single()).canonicalFile
        check(database.isFile && database.name == "plain.db" && database.parentFile.name == "files")
        val peer = JsonHelper.jsonDecode<ApiPeerFixture>(System.`in`.bufferedReader().readText())
        peer.validate()
        SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS).use { db ->
            check(db.version == 1)
            check(db.enableWriteAheadLogging())
            val now = Instant.now().toString()
            db.execSQL(
                "INSERT INTO peers (id,name,ip,key,public_key,status,port,device_type,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any>(peer.id, "API fixture peer", "127.0.0.1", peer.key, peer.publicKey, "PAIRED", 1, "OTHER", now, now),
            )
        }
    }
}
