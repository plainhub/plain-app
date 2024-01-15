package com.ismartcoding.lib.apk

import com.ismartcoding.lib.apk.bean.ApkSignStatus
import com.ismartcoding.lib.apk.utils.Inputs
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ByteArrayApkFile(private var apkData: ByteArray?) : AbstractApkFile(), Closeable {
    @get:Throws(IOException::class)
    protected override val allCertificateData: List<CertificateFile>
        get() {
            val list: MutableList<CertificateFile> = ArrayList()
            ByteArrayInputStream(apkData).use { `in` ->
                ZipInputStream(`in`).use { zis ->
                    var entry: ZipEntry
                    while (zis.nextEntry.also { entry = it } != null) {
                        val name = entry.name
                        if (name.uppercase(Locale.getDefault()).endsWith(".RSA") || name.uppercase(
                                Locale.getDefault()
                            ).endsWith(".DSA")
                        ) {
                            list.add(CertificateFile(name, Inputs.readAll(zis)))
                        }
                    }
                }
            }
            return list
        }

    @Throws(IOException::class)
    override fun getFileData(path: String?): ByteArray? {
        ByteArrayInputStream(apkData).use { `in` ->
            ZipInputStream(`in`).use { zis ->
                var entry: ZipEntry
                while (zis.nextEntry.also { entry = it } != null) {
                    if (path == entry.name) {
                        return Inputs.readAll(zis)
                    }
                }
            }
        }
        return null
    }

    override fun fileData(): ByteBuffer {
        return apkData?.let { ByteBuffer.wrap(it).asReadOnlyBuffer() }!!
    }

    @Deprecated("")
    override fun verifyApk(): ApkSignStatus? {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun close() {
        super.close()
        apkData = null
    }
}