package com.ismartcoding.lib.kgraphql.schema.structure

import com.ismartcoding.lib.kgraphql.ValidationException
import com.ismartcoding.lib.kgraphql.schema.SchemaException
import com.ismartcoding.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.lib.kgraphql.schema.model.ast.ArgumentNode
import com.ismartcoding.lib.kgraphql.schema.model.ast.SelectionNode.FieldNode
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


fun validatePropertyArguments(parentType: Type, field: Field, requestNode: FieldNode) {
    val argumentValidationExceptions = field.validateArguments(requestNode.arguments, parentType.name)

    if (argumentValidationExceptions.isNotEmpty()) {
        throw ValidationException(argumentValidationExceptions.fold("") { sum, exc ->
            "$sum${exc.message}"
        }, nodes = argumentValidationExceptions.flatMap { it.nodes ?: listOf() })
    }
}

fun Field.validateArguments(selectionArgs: List<ArgumentNode>?, parentTypeName: String?): List<ValidationException> {
    if (!(this.arguments.isNotEmpty() || selectionArgs?.isNotEmpty() != true)) {
        return listOf(ValidationException(
            message = "Property $name on type $parentTypeName has no arguments, found: ${selectionArgs.map { it.name.value }}",
            nodes = selectionArgs
        ))
    }

    val exceptions = mutableListOf<ValidationException>()

    val parameterNames = arguments.map { it.name }
    val invalidArguments = selectionArgs?.filter { it.name.value !in parameterNames }

    if (invalidArguments != null && invalidArguments.isNotEmpty()) {
        exceptions.add(
            ValidationException(
                message = "$name does support arguments ${arguments.map { it.name }}. " +
                          "Found arguments ${selectionArgs.map { it.name.value }}"
            )
        )
    }

    arguments.forEach { arg ->
        val value = selectionArgs?.firstOrNull { arg.name == it.name.value }
        if (value == null && arg.type.kind == TypeKind.NON_NULL && arg.defaultValue == null) {
            exceptions.add(
                ValidationException(
                    message = "Missing value for non-nullable argument ${arg.name} on the field '$name'"
                )
            )
        } // else is valid
    }

    return exceptions
}


/**
 * validate that only typed fragments or __typename are present
 */
fun validateUnionRequest(field: Field.Union<*>, selectionNode: FieldNode) {
    val illegalChildren = selectionNode.selectionSet?.selections?.filterNot {
        !(it is FieldNode && it.name.value != "__typename")
    }

    if (illegalChildren?.any() == true) {
        throw ValidationException(
            message = "Invalid selection set with properties: ${illegalChildren.joinToString(prefix = "[", postfix = "]") {
                (it as FieldNode).aliasOrName.value
            }} on union type property ${field.name} : ${field.returnType.possibleTypes.map { it.name }}",
            nodes = illegalChildren
        )
    }
}

fun validateName(name : String) {
    if(name.startsWith("__")){
        throw SchemaException("Illegal name '$name'. " +
                "Names starting with '__' are reserved for introspection system"
        )
    }
}

//function before generic, because it is its subset
fun assertValidObjectType(kClass: KClass<*>) = when {
    kClass.isSubclassOf(Function::class) -> throw SchemaException("Cannot handle function $kClass as Object type")
    kClass.isSubclassOf(Enum::class) -> throw SchemaException("Cannot handle enum class $kClass as Object type")
    else -> Unit
}
