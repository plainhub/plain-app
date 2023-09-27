package com.ismartcoding.lib.phonegeo

import com.ismartcoding.lib.phonegeo.algo.BinarySearchAlgorithm
import com.ismartcoding.lib.phonegeo.algo.LookupAlgorithm
import com.ismartcoding.lib.phonegeo.algo.ProspectBinarySearchAlgorithm
import com.ismartcoding.lib.phonegeo.algo.SequenceSearchAlgorithm
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

class PhoneNumberLookup private constructor() {
    private var srcPhoneBytes: ByteArray? = null
    private var algoType: LookupAlgorithm.IMPL = LookupAlgorithm.IMPL.BINARY_SEARCH
    private val algorithmCache = mutableMapOf<LookupAlgorithm.IMPL, WeakReference<LookupAlgorithm>?>()

    init {
        try {
            this.javaClass.classLoader?.getResourceAsStream(PHONE_GEO_DAT)?.use { geoStream ->
                ByteArrayOutputStream().use { baos ->
                    val buffer = ByteArray(1024 * 4)
                    var n = geoStream.read(buffer)
                    while (-1 != n) {
                        baos.write(buffer, 0, n)
                        n = geoStream.read(buffer)
                    }
                    srcPhoneBytes = baos.toByteArray()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load phone.dat")
        }
    }

    fun with(algorithm: LookupAlgorithm.IMPL): PhoneNumberLookup {
        this.algoType = algorithm
        return this
    }

    fun lookup(phoneNumber: String): PhoneNumberInfo? {
        if (srcPhoneBytes == null) {
            throw IllegalStateException("Something happens when loading phone.dat")
        }
        if (algorithmCache[algoType]?.get() == null) {
            algorithmCache[algoType] =
                WeakReference(
                    when (algoType) {
                        LookupAlgorithm.IMPL.BINARY_SEARCH -> BinarySearchAlgorithm(srcPhoneBytes!!)
                        LookupAlgorithm.IMPL.BINARY_SEARCH_PROSPECT -> ProspectBinarySearchAlgorithm(srcPhoneBytes!!)
                        else -> SequenceSearchAlgorithm(srcPhoneBytes!!)
                    },
                )
        }
        return algorithmCache[algoType]?.get()?.lookup(phoneNumber)
    }

    companion object {
        private const val PHONE_GEO_DAT = "phone.dat"

        private var INSTANCE: PhoneNumberLookup? = null

        fun instance(): PhoneNumberLookup {
            if (INSTANCE == null) {
                synchronized(PhoneNumberLookup::class) {
                    if (INSTANCE == null) {
                        INSTANCE = PhoneNumberLookup()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}
