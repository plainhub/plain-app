package com.ismartcoding.lib.apk.bean

class ApkMeta private constructor(builder: Builder) {
    val packageName: String?
    /**
     * get the apk's title(name)
     */
    /**
     * alias for getLabel
     */
    val name: String?
        /**
         * alias for getLabel
         */
        get() = field

    /**
     * the icon file path in apk
     *
     * @return null if not found
     */
    @get:Deprecated("use {@link AbstractApkFile#getAllIcons()} instead.")
    val icon: String?
    val versionName: String?
    val versionCode: Long?
    val revisionCode: Long?
    val sharedUserId: String?
    val sharedUserLabel: String?
    val split: String?
    val configForSplit: String?
    val isFeatureSplit: Boolean
    val isSplitRequired: Boolean
    val isIsolatedSplits: Boolean
    val installLocation: String?
    val minSdkVersion: String?
    val targetSdkVersion: String?
    val maxSdkVersion: String?
    val compileSdkVersion: String?
    val compileSdkVersionCodename: String?
    val platformBuildVersionCode: String?
    val platformBuildVersionName: String?
    val glEsVersion: GlEsVersion?
    val isAnyDensity: Boolean
    val isSmallScreens: Boolean
    val isNormalScreens: Boolean
    val isLargeScreens: Boolean
    val isDebuggable: Boolean
    val usesPermissions: MutableList<String?>
    val usesFeatures: MutableList<UseFeature>
    val permissions: MutableList<Permission>

    init {
        packageName = builder.packageName
        name = builder.label
        icon = builder.icon
        versionName = builder.versionName
        versionCode = builder.versionCode
        revisionCode = builder.revisionCode
        sharedUserId = builder.sharedUserId
        sharedUserLabel = builder.sharedUserLabel
        split = builder.split
        configForSplit = builder.configForSplit
        isFeatureSplit = builder.isFeatureSplit
        isSplitRequired = builder.isSplitRequired
        isIsolatedSplits = builder.isolatedSplits
        installLocation = builder.installLocation
        minSdkVersion = builder.minSdkVersion
        targetSdkVersion = builder.targetSdkVersion
        maxSdkVersion = builder.maxSdkVersion
        compileSdkVersion = builder.compileSdkVersion
        compileSdkVersionCodename = builder.compileSdkVersionCodename
        platformBuildVersionCode = builder.platformBuildVersionCode
        platformBuildVersionName = builder.platformBuildVersionName
        glEsVersion = builder.glEsVersion
        isAnyDensity = builder.anyDensity
        isSmallScreens = builder.smallScreens
        isNormalScreens = builder.normalScreens
        isLargeScreens = builder.largeScreens
        isDebuggable = builder.debuggable
        usesPermissions = builder.usesPermissions
        usesFeatures = builder.usesFeatures
        permissions = builder.permissions
    }

    override fun toString(): String {
        return ("packageName: \t" + packageName + "\n"
                + "label: \t" + name + "\n"
                + "icon: \t" + icon + "\n"
                + "versionName: \t" + versionName + "\n"
                + "versionCode: \t" + versionCode + "\n"
                + "minSdkVersion: \t" + minSdkVersion + "\n"
                + "targetSdkVersion: \t" + targetSdkVersion + "\n"
                + "maxSdkVersion: \t" + maxSdkVersion)
    }

    class Builder {
        var packageName: String? = null
        var label: String? = null
        var icon: String? = null
        var versionName: String? = null
        var versionCode: Long? = null
        var revisionCode: Long? = null
        var sharedUserId: String? = null
        var sharedUserLabel: String? = null
        var split: String? = null
        var configForSplit: String? = null
        var isFeatureSplit = false
        var isSplitRequired = false
        var isolatedSplits = false
        var installLocation: String? = null
        var minSdkVersion: String? = null
        var targetSdkVersion: String? = null
        var maxSdkVersion: String? = null
        var compileSdkVersion: String? = null
        var compileSdkVersionCodename: String? = null
        var platformBuildVersionCode: String? = null
        var platformBuildVersionName: String? = null
        var glEsVersion: GlEsVersion? = null
        var anyDensity = false
        var smallScreens = false
        var normalScreens = false
        var largeScreens = false
        var debuggable = false
        val usesPermissions: MutableList<String?> = ArrayList()
        val usesFeatures: MutableList<UseFeature> = ArrayList()
        val permissions: MutableList<Permission> = ArrayList()
        fun setPackageName(packageName: String?): Builder {
            this.packageName = packageName
            return this
        }

        fun setLabel(label: String?): Builder {
            this.label = label
            return this
        }

        fun setIcon(icon: String?): Builder {
            this.icon = icon
            return this
        }

        fun setVersionName(versionName: String?): Builder {
            this.versionName = versionName
            return this
        }

        fun setVersionCode(versionCode: Long?): Builder {
            this.versionCode = versionCode
            return this
        }

        fun setRevisionCode(revisionCode: Long?): Builder {
            this.revisionCode = revisionCode
            return this
        }

        fun setSharedUserId(sharedUserId: String?): Builder {
            this.sharedUserId = sharedUserId
            return this
        }

        fun setSharedUserLabel(sharedUserLabel: String?): Builder {
            this.sharedUserLabel = sharedUserLabel
            return this
        }

        fun setSplit(split: String?): Builder {
            this.split = split
            return this
        }

        fun setConfigForSplit(configForSplit: String?): Builder {
            this.configForSplit = configForSplit
            return this
        }

        fun setIsFeatureSplit(isFeatureSplit: Boolean): Builder {
            this.isFeatureSplit = isFeatureSplit
            return this
        }

        fun setIsSplitRequired(isSplitRequired: Boolean): Builder {
            this.isSplitRequired = isSplitRequired
            return this
        }

        fun setIsolatedSplits(isolatedSplits: Boolean): Builder {
            this.isolatedSplits = isolatedSplits
            return this
        }

        fun setInstallLocation(installLocation: String?): Builder {
            this.installLocation = installLocation
            return this
        }

        fun setMinSdkVersion(minSdkVersion: String?): Builder {
            this.minSdkVersion = minSdkVersion
            return this
        }

        fun setTargetSdkVersion(targetSdkVersion: String?): Builder {
            this.targetSdkVersion = targetSdkVersion
            return this
        }

        fun setMaxSdkVersion(maxSdkVersion: String?): Builder {
            this.maxSdkVersion = maxSdkVersion
            return this
        }

        fun setCompileSdkVersion(compileSdkVersion: String?): Builder {
            this.compileSdkVersion = compileSdkVersion
            return this
        }

        fun setCompileSdkVersionCodename(compileSdkVersionCodename: String?): Builder {
            this.compileSdkVersionCodename = compileSdkVersionCodename
            return this
        }

        fun setPlatformBuildVersionCode(platformBuildVersionCode: String?): Builder {
            this.platformBuildVersionCode = platformBuildVersionCode
            return this
        }

        fun setPlatformBuildVersionName(platformBuildVersionName: String?): Builder {
            this.platformBuildVersionName = platformBuildVersionName
            return this
        }

        fun setGlEsVersion(glEsVersion: GlEsVersion?): Builder {
            this.glEsVersion = glEsVersion
            return this
        }

        fun setAnyDensity(anyDensity: Boolean): Builder {
            this.anyDensity = anyDensity
            return this
        }

        fun setSmallScreens(smallScreens: Boolean): Builder {
            this.smallScreens = smallScreens
            return this
        }

        fun setNormalScreens(normalScreens: Boolean): Builder {
            this.normalScreens = normalScreens
            return this
        }

        fun setLargeScreens(largeScreens: Boolean): Builder {
            this.largeScreens = largeScreens
            return this
        }

        fun setDebuggable(debuggable: Boolean): Builder {
            this.debuggable = debuggable
            return this
        }

        fun addUsesPermission(usesPermission: String?): Builder {
            usesPermissions.add(usesPermission)
            return this
        }

        fun addUsesFeature(usesFeature: UseFeature): Builder {
            usesFeatures.add(usesFeature)
            return this
        }

        fun addPermissions(permission: Permission): Builder {
            permissions.add(permission)
            return this
        }

        fun build(): ApkMeta {
            return ApkMeta(this)
        }
    }

    companion object {
        fun newBuilder(): Builder {
            return Builder()
        }
    }
}