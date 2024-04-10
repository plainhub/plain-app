package com.ismartcoding.lib.apk

import com.ismartcoding.lib.apk.bean.AdaptiveIcon
import com.ismartcoding.lib.apk.bean.ApkMeta
import com.ismartcoding.lib.apk.bean.ApkSignStatus
import com.ismartcoding.lib.apk.bean.ApkSigner
import com.ismartcoding.lib.apk.bean.ApkV2Signer
import com.ismartcoding.lib.apk.bean.CertificateMeta
import com.ismartcoding.lib.apk.bean.DexClass
import com.ismartcoding.lib.apk.bean.Icon
import com.ismartcoding.lib.apk.bean.IconFace
import com.ismartcoding.lib.apk.bean.IconPath
import com.ismartcoding.lib.apk.exception.ParserException
import com.ismartcoding.lib.apk.parser.AdaptiveIconParser
import com.ismartcoding.lib.apk.parser.ApkMetaTranslator
import com.ismartcoding.lib.apk.parser.ApkSignBlockParser
import com.ismartcoding.lib.apk.parser.BinaryXmlParser
import com.ismartcoding.lib.apk.parser.CertificateMetas
import com.ismartcoding.lib.apk.parser.CertificateParser
import com.ismartcoding.lib.apk.parser.CompositeXmlStreamer
import com.ismartcoding.lib.apk.parser.DexParser
import com.ismartcoding.lib.apk.parser.ResourceTableParser
import com.ismartcoding.lib.apk.parser.XmlStreamer
import com.ismartcoding.lib.apk.parser.XmlTranslator
import com.ismartcoding.lib.apk.struct.AndroidConstants
import com.ismartcoding.lib.apk.struct.resource.Densities
import com.ismartcoding.lib.apk.struct.resource.ResourceTable
import com.ismartcoding.lib.apk.struct.signingv2.ApkSigningBlock
import com.ismartcoding.lib.apk.struct.zip.EOCD
import com.ismartcoding.lib.apk.utils.Buffers
import com.ismartcoding.lib.apk.utils.Unsigned
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.cert.CertificateException
import java.util.Locale

abstract class AbstractApkFile : Closeable {
    private var dexClasses: Array<DexClass?>? = null
    private var resourceTableParsed = false
    private var resourceTable: ResourceTable? = null
    private var locales: Set<Locale?>? = null
        @Throws(IOException::class)
        get() {
            parseResourceTable()
            return field
        }
    private var manifestParsed = false
    var manifestXml: String = ""
        get() {
            parseManifest()
            return field
        }
    var apkMeta: ApkMeta? = null
        @Throws(IOException::class)
        get() {
            parseManifest()
            return field
        }
    private var iconPaths: List<IconPath?>? = null
    private var apkSigners: MutableList<ApkSigner>? = null
    private var apkV2Signers: List<ApkV2Signer>? = null

    /**
     * default use empty locale
     */
    var preferredLocale: Locale = DEFAULT_LOCALE
        /**
         * The locale preferred. Will cause getManifestXml / getApkMeta to return different values.
         * The default value is from os default locale setting.
         */
        set(value) {
            if (this.preferredLocale != value) {
                field = value
                manifestXml = ""
                apkMeta = null
                manifestParsed = false
            }
        }

    @get:Throws(IOException::class, CertificateException::class)
    @get:Deprecated("use {{@link #getApkSingers()}} instead")
    val certificateMetaList: List<CertificateMeta?>
        /**
         * Get the apk's certificate meta. If have multi signer, return the certificate the first signer used.
         *
         */
        get() {
            if (apkSigners == null) {
                parseCertificates()
            }
            if (apkSigners!!.isEmpty()) {
                throw ParserException("ApkFile certificate not found")
            }
            return apkSigners!![0].certificateMetas
        }

    @get:Throws(IOException::class, CertificateException::class)
    @get:Deprecated("use {{@link #getApkSingers()}} instead")
    val allCertificateMetas: Map<String?, List<CertificateMeta?>?>
        /**
         * Get the apk's all certificates.
         * For each entry, the key is certificate file path in apk file, the value is the certificates info of the certificate file.
         *
         */
        get() {
            val apkSigners = apkSingers
            val map: MutableMap<String?, List<CertificateMeta?>?> = LinkedHashMap()
            for (apkSigner in apkSigners!!) {
                map[apkSigner.path] = apkSigner.certificateMetas
            }
            return map
        }

    @get:Throws(IOException::class, CertificateException::class)
    val apkSingers: List<ApkSigner>?
        /**
         * Get the apk's all cert file info, of apk v1 signing.
         * If cert faile not exist, return empty list.
         */
        get() {
            if (apkSigners == null) {
                parseCertificates()
            }
            return apkSigners
        }

    private fun parseCertificates() {
        apkSigners = ArrayList()
        for (file in allCertificateData) {
            val parser: CertificateParser = CertificateParser.getInstance(
                file.data!!
            )
            val certificateMetas = parser.parse()
            (apkSigners as ArrayList<ApkSigner>).add(ApkSigner(file.path, certificateMetas))
        }
    }

    @get:Throws(IOException::class, CertificateException::class)
    val apkV2Singers: List<ApkV2Signer>?
        /**
         * Get the apk's all signer in apk sign block, using apk singing v2 scheme.
         * If apk v2 signing block not exists, return empty list.
         */
        get() {
            if (apkV2Signers == null) {
                parseApkSigningBlock()
            }
            return apkV2Signers
        }

    @Throws(IOException::class, CertificateException::class)
    private fun parseApkSigningBlock() {
        val list: MutableList<ApkV2Signer> = ArrayList()
        val apkSignBlockBuf = findApkSignBlock()
        if (apkSignBlockBuf != null) {
            val parser = ApkSignBlockParser(apkSignBlockBuf)
            val apkSigningBlock = parser.parse()
            for (signerBlock in apkSigningBlock.signerBlocks) {
                val certificates = signerBlock.certificates
                val certificateMetas = CertificateMetas.from(certificates)
                val apkV2Signer = ApkV2Signer(certificateMetas)
                list.add(apkV2Signer)
            }
        }
        apkV2Signers = list
    }

    @get:Throws(IOException::class)
    protected abstract val allCertificateData: List<CertificateFile>

    protected class CertificateFile(val path: String, val data: ByteArray?)

    @Throws(IOException::class)
    private fun parseManifest() {
        if (manifestParsed) {
            return
        }
        parseResourceTable()
        val xmlTranslator = XmlTranslator()
        val apkTranslator = resourceTable?.let { ApkMetaTranslator(it, preferredLocale) }
        val xmlStreamer = apkTranslator?.let {
            CompositeXmlStreamer(
                xmlTranslator,
                it
            )
        }
        val data = getFileData(AndroidConstants.MANIFEST_FILE)
            ?: throw ParserException("Manifest file not found")
        xmlStreamer?.let { transBinaryXml(data, it) }
        manifestXml = xmlTranslator.xml
        apkMeta = apkTranslator?.apkMeta
        iconPaths = apkTranslator?.iconPaths
        manifestParsed = true
    }

    /**
     * read file in apk into bytes
     */
    @Throws(IOException::class)
    abstract fun getFileData(path: String?): ByteArray?

    /**
     * return the whole apk file as ByteBuffer
     */
    @Throws(IOException::class)
    protected abstract fun fileData(): ByteBuffer

    /**
     * trans binary xml file to text xml file.
     *
     * @param path the xml file path in apk file
     * @return the text. null if file not exists
     * @throws IOException
     */
    @Throws(IOException::class)
    fun transBinaryXml(path: String?): String? {
        val data = getFileData(path) ?: return null
        parseResourceTable()
        val xmlTranslator = XmlTranslator()
        transBinaryXml(data, xmlTranslator)
        return xmlTranslator.xml
    }

    @Throws(IOException::class)
    private fun transBinaryXml(data: ByteArray, xmlStreamer: XmlStreamer) {
        parseResourceTable()
        val buffer = ByteBuffer.wrap(data)
        val binaryXmlParser =
            resourceTable?.let { BinaryXmlParser(buffer, it, xmlStreamer, preferredLocale) }
        binaryXmlParser?.parse()
    }

    @get:Throws(IOException::class)
    val allIcons: List<IconFace>
        /**
         * This method return icons specified in android manifest file, application.
         * The icons could be file icon, color icon, or adaptive icon, etc.
         *
         * @return icon files.
         */
        get() {
            val iconPaths = getIconPaths()
            if (iconPaths!!.isEmpty()) {
                return emptyList()
            }
            val iconFaces: MutableList<IconFace> = ArrayList(
                iconPaths.size
            )
            for (iconPath in iconPaths) {
                val filePath = iconPath?.path
                if (filePath!!.endsWith(".xml")) {
                    // adaptive icon?
                    val data = getFileData(filePath) ?: continue
                    parseResourceTable()
                    val iconParser = AdaptiveIconParser()
                    transBinaryXml(data, iconParser)
                    var backgroundIcon: Icon? = null
                    if (iconParser.background != null) {
                        backgroundIcon = newFileIcon(iconParser.background, iconPath.density)
                    }
                    var foregroundIcon: Icon? = null
                    if (iconParser.foreground != null) {
                        foregroundIcon = newFileIcon(iconParser.foreground, iconPath.density)
                    }
                    val icon = AdaptiveIcon(foregroundIcon, backgroundIcon)
                    iconFaces.add(icon)
                } else {
                    val icon = iconPath.let { newFileIcon(filePath, it.density) }
                    iconFaces.add(icon)
                }
            }
            return iconFaces
        }

    @Throws(IOException::class)
    private fun newFileIcon(filePath: String?, density: Int): Icon {
        return Icon(filePath, density, getFileData(filePath))
    }

    @get:Throws(IOException::class)
    @get:Deprecated("use {@link #getAllIcons()}")
    val iconFile: Icon?
        /**
         * Get the default apk icon file.
         *
         */
        get() {
            val iconPath = apkMeta?.icon ?: return null
            return Icon(iconPath, Densities.DEFAULT, getFileData(iconPath))
        }

    /**
     * Get all the icon paths, for different densities.
     *
     */
    @Deprecated("using {@link #getAllIcons()} instead")
    @Throws(IOException::class)
    fun getIconPaths(): List<IconPath?>? {
        parseManifest()
        return iconPaths
    }

    @get:Throws(IOException::class)
    @get:Deprecated("using {@link #getAllIcons()} instead")
    val iconFiles: List<Icon>
        /**
         * Get all the icons, for different densities.
         *
         */
        get() {
            val iconPaths = getIconPaths()
            val icons: MutableList<Icon> = ArrayList(
                iconPaths!!.size
            )
            for (iconPath in iconPaths) {
                val icon = iconPath?.let { newFileIcon(iconPath.path, it.density) }
                if (icon != null) {
                    icons.add(icon)
                }
            }
            return icons
        }

    /**
     * get class infos form dex file. currently only class name
     */
    @Throws(IOException::class)
    fun getDexClasses(): Array<DexClass?>? {
        if (dexClasses == null) {
            parseDexFiles()
        }
        return dexClasses
    }

    private fun mergeDexClasses(
        first: Array<DexClass?>?,
        second: Array<DexClass?>?
    ): Array<DexClass?> {
        val result = arrayOfNulls<DexClass>(
            first!!.size + second!!.size
        )
        System.arraycopy(first, 0, result, 0, first.size)
        System.arraycopy(second, 0, result, first.size, second.size)
        return result
    }

    @Throws(IOException::class)
    private fun parseDexFile(path: String?): Array<DexClass?>? {
        val data = getFileData(path)
        if (data == null) {
            val msg = String.format("Dex file %s not found", path)
            throw ParserException(msg)
        }
        val buffer = ByteBuffer.wrap(data)
        val dexParser = DexParser(buffer)
        return dexParser.parse()
    }

    @Throws(IOException::class)
    private fun parseDexFiles() {
        dexClasses = parseDexFile(AndroidConstants.DEX_FILE)
        for (i in 2..999) {
            val path = String.format(AndroidConstants.DEX_ADDITIONAL, i)
            try {
                val classes = parseDexFile(path)
                dexClasses = mergeDexClasses(dexClasses, classes)
            } catch (e: ParserException) {
                break
            }
        }
    }

    /**
     * parse resource table.
     */
    @Throws(IOException::class)
    private fun parseResourceTable() {
        if (resourceTableParsed) {
            return
        }
        resourceTableParsed = true
        val data = getFileData(AndroidConstants.RESOURCE_FILE)
        if (data == null) {
            // if no resource entry has been found, we assume it is not needed by this APK
            resourceTable = ResourceTable()
            locales = emptySet<Locale>()
            return
        }
        val buffer = ByteBuffer.wrap(data)
        val resourceTableParser = ResourceTableParser(buffer)
        resourceTableParser.parse()
        resourceTable = resourceTableParser.resourceTable
        locales = resourceTableParser.locales
    }

    /**
     * Check apk sign. This method only use apk v1 scheme verifier
     *
     */
    @Deprecated("using google official ApkVerifier of apksig lib instead.")
    @Throws(
        IOException::class
    )
    abstract fun verifyApk(): ApkSignStatus?

    @Throws(IOException::class)
    override fun close() {
        apkSigners = null
        resourceTable = null
        iconPaths = null
    }

    /**
     * Create ApkSignBlockParser for this apk file.
     *
     * @return null if do not have sign block
     */
    @Throws(IOException::class)
    protected fun findApkSignBlock(): ByteBuffer? {
        val buffer = fileData().order(ByteOrder.LITTLE_ENDIAN)
        val len = buffer.limit()

        // first find zip end of central directory entry
        if (len < 22) {
            // should not happen
            throw RuntimeException("Not zip file")
        }
        val maxEOCDSize = 1024 * 100
        var eocd: EOCD? = null
        for (i in len - 22 downTo 0.coerceAtLeast(len - maxEOCDSize) + 1) {
            val v = buffer.getInt(i)
            if (v == EOCD.SIGNATURE) {
                Buffers.positionInt(buffer, i + 4)
                eocd = EOCD()
                eocd.diskNum = Buffers.readUShort(buffer)
                eocd.cdStartDisk = Buffers.readUShort(buffer).toShort()
                eocd.cdRecordNum = Buffers.readUShort(buffer).toShort()
                eocd.totalCDRecordNum = Buffers.readUShort(buffer).toShort()
                eocd.cdSize = Buffers.readUInt(buffer)
                eocd.cdStart = Buffers.readUInt(buffer)
                eocd.commentLen = Buffers.readUShort(buffer).toShort()
            }
        }
        if (eocd == null) {
            return null
        }
        val magicStrLen = 16
        val cdStart = eocd.cdStart
        // find apk sign block
        Buffers.position(buffer, cdStart - magicStrLen)
        val magic = Buffers.readAsciiString(buffer, magicStrLen)
        if (magic != ApkSigningBlock.MAGIC) {
            return null
        }
        Buffers.position(buffer, cdStart - 24)
        val blockSize = Unsigned.ensureUInt(buffer.long)
        Buffers.position(buffer, cdStart - blockSize - 8)
        val size2 = Unsigned.ensureULong(buffer.long)
        return if (blockSize.toLong() != size2) {
            null
        } else Buffers.sliceAndSkip(buffer, blockSize - magicStrLen)
        // now at the start of signing block
    }

    companion object {
        private val DEFAULT_LOCALE = Locale.US
    }
}