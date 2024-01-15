package com.ismartcoding.lib.apk

import com.ismartcoding.lib.apk.bean.ApkMeta
import java.io.File
import java.util.Locale

object ApkParsers {
    private var useBouncyCastle = false
    fun useBouncyCastle(): Boolean {
        return useBouncyCastle
    }

    fun useBouncyCastle(useBouncyCastle: Boolean) {
        ApkParsers.useBouncyCastle = useBouncyCastle
    }

    fun getMetaInfo(apkFilePath: String): ApkMeta? {
        ApkFile(apkFilePath).use { apkFile -> return apkFile.apkMeta }
    }

    fun getMetaInfo(file: File): ApkMeta? {
        ApkFile(file).use { apkFile -> return apkFile.apkMeta }
    }

    fun getMetaInfo(apkData: ByteArray): ApkMeta? {
        ByteArrayApkFile(apkData).use { apkFile -> return apkFile.apkMeta }
    }

    fun getMetaInfo(apkFilePath: String, locale: Locale): ApkMeta? {
        ApkFile(apkFilePath).use { apkFile ->
            apkFile.preferredLocale = locale
            return apkFile.apkMeta
        }
    }

    fun getMetaInfo(file: File, locale: Locale): ApkMeta? {
        ApkFile(file).use { apkFile ->
            apkFile.preferredLocale = locale
            return apkFile.apkMeta
        }
    }

    fun getMetaInfo(apkData: ByteArray, locale: Locale): ApkMeta? {
        ByteArrayApkFile(apkData).use { apkFile ->
            apkFile.preferredLocale = locale
            return apkFile.apkMeta
        }
    }

    fun getManifestXml(apkFilePath: String): String {
        ApkFile(apkFilePath).use { apkFile -> return apkFile.manifestXml }
    }

    fun getManifestXml(file: File): String {
        ApkFile(file).use { apkFile -> return apkFile.manifestXml }
    }

    fun getManifestXml(apkData: ByteArray): String {
        ByteArrayApkFile(apkData).use { apkFile -> return apkFile.manifestXml }
    }

    fun getManifestXml(apkFilePath: String, locale: Locale): String {
        ApkFile(apkFilePath).use { apkFile ->
            apkFile.preferredLocale = locale
            return apkFile.manifestXml
        }
    }

    fun getManifestXml(file: File, locale: Locale): String {
        ApkFile(file).use { apkFile ->
            apkFile.preferredLocale = locale
            return apkFile.manifestXml
        }
    }

    fun getManifestXml(apkData: ByteArray, locale: Locale): String {
        ByteArrayApkFile(apkData).use { apkFile ->
            apkFile.preferredLocale = locale
            return apkFile.manifestXml
        }
    }
}