package com.ismartcoding.lib.apk.bean

import com.ismartcoding.lib.apk.struct.dex.DexClassStruct
import com.ismartcoding.lib.apk.struct.dex.DexHeader

class DexClass(
    val dexHeader: DexHeader,
    /**
     * the class name
     */
    private val classType: String, val superClass: String?, private val accessFlags: Int
) {
    val packageName: String
        get() {
            var packageName = classType
            if (packageName.isNotEmpty()) {
                if (packageName[0] == 'L') {
                    packageName = packageName.substring(1)
                }
            }
            if (packageName.isNotEmpty()) {
                val idx = classType.lastIndexOf('/')
                if (idx > 0) {
                    packageName = packageName.substring(0, classType.lastIndexOf('/') - 1)
                } else if (packageName[packageName.length - 1] == ';') {
                    packageName = packageName.substring(0, packageName.length - 1)
                }
            }
            return packageName.replace('/', '.')
        }
    val isInterface: Boolean
        get() = accessFlags and DexClassStruct.ACC_INTERFACE != 0
    val isEnum: Boolean
        get() = accessFlags and DexClassStruct.ACC_ENUM != 0
    val isAnnotation: Boolean
        get() = accessFlags and DexClassStruct.ACC_ANNOTATION != 0
    val isPublic: Boolean
        get() = accessFlags and DexClassStruct.ACC_PUBLIC != 0
    val isProtected: Boolean
        get() = accessFlags and DexClassStruct.ACC_PROTECTED != 0
    val isStatic: Boolean
        get() = accessFlags and DexClassStruct.ACC_STATIC != 0

    override fun toString(): String {
        return classType
    }
}