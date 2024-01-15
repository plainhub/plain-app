package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.StringPool

class ResourcePackage(@JvmField val header: PackageHeader) {
    // the packageName
    var name: String? = header.name
    var id: Short = header.id.toShort()

    // contains the names of the types of the Resources defined in the ResourcePackage
    var typeStringPool: StringPool? = null

    //  contains the names (keys) of the Resources defined in the ResourcePackage.
    var keyStringPool: StringPool? = null
    private var typeSpecMap: MutableMap<Short, TypeSpec> = HashMap()
    private var typesMap: MutableMap<Short, MutableList<Type>> = HashMap()

    fun addTypeSpec(typeSpec: TypeSpec) {
        typeSpecMap[typeSpec.id] = typeSpec
    }

    fun getTypeSpec(id: Short): TypeSpec? {
        return typeSpecMap[id]
    }

    fun addType(type: Type) {
        var types = typesMap[type.id]
        if (types == null) {
            types = ArrayList()
            typesMap[type.id] = types
        }
        types.add(type)
    }

    fun getTypes(id: Short): List<Type>? {
        return typesMap[id]
    }
}