package com.ismartcoding.lib.apk

import com.ismartcoding.lib.apk.bean.ApkSignStatus
import com.ismartcoding.lib.apk.utils.Inputs
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.jar.JarFile
import java.util.zip.ZipFile

class ApkFile(private val apkFile: File) : AbstractApkFile(), Closeable {
    private val zf: ZipFile = ZipFile(apkFile)
    private var fileChannel: FileChannel? = null

    constructor(filePath: String) : this(File(filePath))

    @get:Throws(IOException::class)
    override val allCertificateData: List<CertificateFile>
        get() {
            val enu = zf.entries()
            val list: MutableList<CertificateFile> = ArrayList()
            while (enu.hasMoreElements()) {
                val ne = enu.nextElement()
                if (ne.isDirectory) {
                    continue
                }
                val name = ne.name.uppercase(Locale.getDefault())
                if (name.endsWith(".RSA") || name.endsWith(".DSA")) {
                    list.add(CertificateFile(name, Inputs.readAllAndClose(zf.getInputStream(ne))))
                }
            }
            return list
        }

    @Throws(IOException::class)
    override fun getFileData(path: String?): ByteArray? {
        val entry = zf.getEntry(path) ?: return null
        val inputStream = zf.getInputStream(entry)
        return Inputs.readAllAndClose(inputStream)
    }

    /** @noinspection resource
     */
    @Throws(IOException::class)
    override fun fileData(): ByteBuffer {
        fileChannel = FileInputStream(apkFile).channel
        return fileChannel!!.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel!!.size())
    }

    /**
     * {@inheritDoc}
     *
     */
    @Deprecated("using google official ApkVerifier of apksig lib instead.")
    @Throws(
        IOException::class
    )
    override fun verifyApk(): ApkSignStatus {
        zf.getEntry("META-INF/MANIFEST.MF")
            ?: // apk is not signed;
            return ApkSignStatus.notSigned
        JarFile(apkFile).use { jarFile ->
            val entries = jarFile.entries()
            val buffer = ByteArray(8192)
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (e.isDirectory) {
                    continue
                }
                try {
                    jarFile.getInputStream(e).use { `in` ->
                        // Read in each jar entry. A security exception will be thrown if a signature/digest check fails.
                        var count: Int
                        while (`in`.read(buffer, 0, buffer.size).also { count = it } != -1) {
                            // Don't care
                        }
                    }
                } catch (se: SecurityException) {
                    return ApkSignStatus.incorrect
                }
            }
        }
        return ApkSignStatus.signed
    }

    /** @noinspection EmptyTryBlock
     */
    @Throws(IOException::class)
    override fun close() {
        Closeable { super@ApkFile.close() }.use { zf.use { fileChannel.use { } } }
    }
}