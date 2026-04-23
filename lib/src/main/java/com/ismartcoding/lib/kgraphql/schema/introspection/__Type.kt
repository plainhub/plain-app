
package com.ismartcoding.lib.kgraphql.schema.introspection

/**
 * GraphQL introspection system defines __Type to represent all of TypeKinds
 * If some field does not apply to given type, it returns null
 */
interface __Type {
    val kind: TypeKind
    val name : String?
    val description : String
    //OBJECT and INTERFACE only
    val fields : List<__Field>?
    //OBJECT only
    val interfaces : List<__Type>?
    //INTERFACE and UNION only
    val possibleTypes : List<__Type>?
    //ENUM only
    val enumValues : List<__EnumValue>?
    //INPUT_OBJECT only
    val inputFields : List<__InputValue>?
    //NON_NULL and LIST only
    val ofType : __Type?
}

fun __Type.asString() = buildString {
    append(kind)
    append(" : ")
    append(name)
    append(" ")

    if(fields != null){
        append("[")
        fields?.forEach { field ->
            append(field.name).append(" : ").append(field.type.name ?: field.type.kind).append(" ")
        }
        append("]")
    }

    if(ofType != null){
        append(" => ").append(ofType?.name)
    }
}