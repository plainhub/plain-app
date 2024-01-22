package com.ismartcoding.lib.apk.parser

import android.util.Log
import com.ismartcoding.lib.apk.bean.ApkMeta
import com.ismartcoding.lib.apk.bean.GlEsVersion
import com.ismartcoding.lib.apk.bean.IconPath
import com.ismartcoding.lib.apk.bean.Permission
import com.ismartcoding.lib.apk.bean.UseFeature
import com.ismartcoding.lib.apk.struct.ResourceValue
import com.ismartcoding.lib.apk.struct.resource.Densities
import com.ismartcoding.lib.apk.struct.resource.ResourceTable
import com.ismartcoding.lib.apk.struct.xml.XmlCData
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceStartTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeStartTag
import com.ismartcoding.lib.logcat.LogCat
import java.util.Locale

class ApkMetaTranslator(resourceTable: ResourceTable, locale: Locale?) : XmlStreamer {
    private val tagStack = arrayOfNulls<String>(100)
    private var depth = 0
    private val apkMetaBuilder: ApkMeta.Builder = ApkMeta.newBuilder()
    var iconPaths = emptyList<IconPath>()
        private set
    private val resourceTable: ResourceTable
    private var locale: Locale? = null

    init {
        this.resourceTable = resourceTable
        this.locale = locale
    }

    override fun onStartTag(xmlNodeStartTag: XmlNodeStartTag) {
        val attributes = xmlNodeStartTag.attributes
        when (xmlNodeStartTag.name) {
            "application" -> {
                val debuggable = attributes?.getBoolean("debuggable", false)
                debuggable?.let { apkMetaBuilder.setDebuggable(it) }
                val label = attributes?.getString("label")
                if (label != null) {
                    apkMetaBuilder.setLabel(label)
                }
                val iconAttr = attributes?.get("icon")
                if (iconAttr != null) {
                    val resourceValue = iconAttr.typedValue
                    if (resourceValue is ResourceValue.ReferenceResourceValue) {
                        val resourceId = resourceValue.referenceResourceId
                        val resources = resourceTable.getResourcesById(resourceId)
                        if (resources.isNotEmpty()) {
                            val icons: MutableList<IconPath> = ArrayList()
                            var hasDefault = false
                            for (resource in resources) {
                                val type = resource.type
                                val resourceEntry = resource.resourceEntry
                                val path = resourceEntry.toStringValue(resourceTable, locale)
                                if (type.density == Densities.DEFAULT) {
                                    hasDefault = true
                                    apkMetaBuilder.setIcon(path)
                                }
                                val iconPath = IconPath(path, type.density)
                                icons.add(iconPath)
                            }
                            if (!hasDefault) {
                                apkMetaBuilder.setIcon(icons[0].path)
                            }
                            iconPaths = icons
                        }
                    } else {
                        val value = iconAttr.value
                        if (value != null) {
                            apkMetaBuilder.setIcon(value)
                            val iconPath = IconPath(value, Densities.DEFAULT)
                            iconPaths = listOf(iconPath)
                        }
                    }
                }
            }

            "manifest" -> {
                apkMetaBuilder.setPackageName(attributes?.getString("package"))
                apkMetaBuilder.setVersionName(attributes?.getString("versionName"))
                apkMetaBuilder.setRevisionCode(attributes?.getLong("revisionCode"))
                apkMetaBuilder.setSharedUserId(attributes?.getString("sharedUserId"))
                apkMetaBuilder.setSharedUserLabel(attributes?.getString("sharedUserLabel"))
                apkMetaBuilder.setSplit(attributes?.getString("split"))
                apkMetaBuilder.setConfigForSplit(attributes?.getString("configForSplit"))
                attributes?.let {
                    apkMetaBuilder.setIsFeatureSplit(it.getBoolean("isFeatureSplit", false))
                    apkMetaBuilder.setIsSplitRequired(
                        attributes.getBoolean(
                            "isSplitRequired",
                            false
                        )
                    )
                    apkMetaBuilder.setIsolatedSplits(attributes.getBoolean("isolatedSplits", false))
                }

                val majorVersionCode = attributes?.getLong("versionCodeMajor")
                var versionCode = attributes?.getLong("versionCode")
                if (majorVersionCode != null) {
                    if (versionCode == null) {
                        versionCode = 0L
                    }
                    versionCode = majorVersionCode shl 32 or (versionCode and 0xFFFFFFFFL)
                }
                apkMetaBuilder.setVersionCode(versionCode)
                val installLocation = attributes?.getString("installLocation")
                if (installLocation != null) {
                    apkMetaBuilder.setInstallLocation(installLocation)
                }
                apkMetaBuilder.setCompileSdkVersion(attributes?.getString("compileSdkVersion"))
                apkMetaBuilder.setCompileSdkVersionCodename(attributes?.getString("compileSdkVersionCodename"))
                apkMetaBuilder.setPlatformBuildVersionCode(attributes?.getString("platformBuildVersionCode"))
                apkMetaBuilder.setPlatformBuildVersionName(attributes?.getString("platformBuildVersionName"))
            }

            "uses-sdk" -> {
                val minSdkVersion = attributes!!.getString("minSdkVersion")
                if (minSdkVersion != null) {
                    apkMetaBuilder.setMinSdkVersion(minSdkVersion)
                }
                val targetSdkVersion = attributes.getString("targetSdkVersion")
                if (targetSdkVersion != null) {
                    apkMetaBuilder.setTargetSdkVersion(targetSdkVersion)
                }
                val maxSdkVersion = attributes.getString("maxSdkVersion")
                if (maxSdkVersion != null) {
                    apkMetaBuilder.setMaxSdkVersion(maxSdkVersion)
                }
            }

            "supports-screens" -> {
                attributes?.let {
                    apkMetaBuilder.setAnyDensity(it.getBoolean("anyDensity", false))
                    apkMetaBuilder.setSmallScreens(attributes.getBoolean("smallScreens", false))
                    apkMetaBuilder.setNormalScreens(attributes.getBoolean("normalScreens", false))
                    apkMetaBuilder.setLargeScreens(attributes.getBoolean("largeScreens", false))
                }

            }

            "uses-feature" -> {
                val name = attributes!!.getString("name")
                val required = attributes.getBoolean("required", false)
                if (name != null) {
                    val useFeature = UseFeature(name, required)
                    apkMetaBuilder.addUsesFeature(useFeature)
                } else {
                    val gl = attributes.getInt("glEsVersion")
                    if (gl != null) {
                        val v: Int = gl
                        val glEsVersion = GlEsVersion(v shr 16, v and 0xffff, required)
                        apkMetaBuilder.setGlEsVersion(glEsVersion)
                    }
                }
            }

            "uses-permission" -> apkMetaBuilder.addUsesPermission(attributes?.getString("name"))
            "permission" -> {
                val permission = Permission(
                    attributes?.getString("name"),
                    attributes?.getString("label"),
                    attributes?.getString("icon"),
                    attributes?.getString("description"),
                    attributes?.getString("group"),
                    attributes?.getString("android:protectionLevel")
                )
                apkMetaBuilder.addPermissions(permission)
            }

            else -> xmlNodeStartTag.name?.let { LogCat.d("Unexpected value: $it") }
        }
        tagStack[depth++] = xmlNodeStartTag.name
    }

    override fun onEndTag(xmlNodeEndTag: XmlNodeEndTag) {
        depth--
    }

    override fun onCData(xmlCData: XmlCData) {}
    override fun onNamespaceStart(tag: XmlNamespaceStartTag) {}
    override fun onNamespaceEnd(tag: XmlNamespaceEndTag) {}
    val apkMeta: ApkMeta
        get() = apkMetaBuilder.build()

    private fun matchTagPath(vararg tags: String): Boolean {
        // the root should always be "manifest"
        if (depth != tags.size + 1) {
            return false
        }
        for (i in 1 until depth) {
            if (tagStack[i] != tags[i - 1]) {
                return false
            }
        }
        return true
    }

    private fun matchLastTag(tag: String): Boolean {
        // the root should always be "manifest"
        return tagStack[depth - 1]!!.endsWith(tag)
    }
}