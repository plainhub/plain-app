package com.ismartcoding.plain.features.call

import android.content.ContentValues
import android.provider.BlockedNumberContract
import android.telephony.PhoneNumberUtils
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.queryCursor
import com.ismartcoding.lib.extensions.trimToComparableNumber
import com.ismartcoding.plain.MainApp

object BlockedNumberHelper {
    fun getAll(): List<BlockedNumber> {
        val blockedNumbers = mutableListOf<BlockedNumber>()
        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val projection =
            arrayOf(
                BlockedNumberContract.BlockedNumbers.COLUMN_ID,
                BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER,
            )

        MainApp.instance.queryCursor(uri, projection) { cursor, cache ->
            val id = cursor.getStringValue(BlockedNumberContract.BlockedNumbers.COLUMN_ID, cache)
            val number = cursor.getStringValue(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, cache)
            val normalizedNumber = cursor.getStringValue(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER, cache)
            val comparableNumber = normalizedNumber.trimToComparableNumber()
            blockedNumbers.add(BlockedNumber(id, number, normalizedNumber, comparableNumber))
        }

        return blockedNumbers
    }

    fun add(number: String) {
        ContentValues().apply {
            put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            put(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER, PhoneNumberUtils.normalizeNumber(number))
            MainApp.instance.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, this)
        }
    }

    fun delete(number: String) {
        val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
        val selectionArgs = arrayOf(number)
        MainApp.instance.contentResolver.delete(BlockedNumberContract.BlockedNumbers.CONTENT_URI, selection, selectionArgs)
    }

    fun isBlocked(
        number: String,
        blockedNumbers: List<BlockedNumber> = getAll(),
    ): Boolean {
        val numberToCompare = number.trimToComparableNumber()
        return blockedNumbers.map { it.numberToCompare }.contains(numberToCompare) || blockedNumbers.map { it.number }.contains(numberToCompare)
    }
}
