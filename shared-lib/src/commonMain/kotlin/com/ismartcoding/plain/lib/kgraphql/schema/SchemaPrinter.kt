package com.ismartcoding.plain.lib.kgraphql.schema

import com.ismartcoding.plain.lib.kgraphql.schema.introspection.*

/**
 * Generates a GraphQL Schema Definition Language (SDL) string from the introspection data.
 */
fun __Schema.toSDL(): String = buildString {
    val printedTypes = mutableSetOf<String>()
    val builtInScalars = setOf("Int", "Float", "String", "Boolean", "ID")

    fun typeRef(type: __Type): String {
        return when (type.kind) {
            TypeKind.NON_NULL -> "${typeRef(type.ofType!!)}!"
            TypeKind.LIST -> "[${typeRef(type.ofType!!)}]"
            else -> type.name ?: "Unknown"
        }
    }

    fun printDescription(desc: String?, indent: String = "") {
        if (!desc.isNullOrBlank()) {
            val lines = desc.trim().lines()
            if (lines.size == 1) {
                appendLine("$indent\"\"\"${lines.first()}\"\"\"")
            } else {
                appendLine("${indent}\"\"\"")
                lines.forEach { appendLine("${indent}${it}") }
                appendLine("${indent}\"\"\"")
            }
        }
    }

    fun printType(type: __Type) {
        val name = type.name ?: return
        if (name in printedTypes) return
        if (name in builtInScalars) return

        when (type.kind) {
            TypeKind.OBJECT -> {
                printedTypes.add(name)
                printDescription(type.description)
                append("type $name")
                if (!type.interfaces.isNullOrEmpty()) {
                    append(" implements ")
                    append(type.interfaces!!.joinToString(", ") { it.name!! })
                }
                appendLine(" {")
                type.fields?.forEach { field ->
                    printDescription(field.description, "  ")
                    append("  ${field.name}")
                    if (field.args.isNotEmpty()) {
                        append("(")
                        field.args.forEachIndexed { index, arg ->
                            if (index > 0) append(", ")
                            if (arg.defaultValue != null) {
                                append("${arg.name}: ${typeRef(arg.type)} = ${arg.defaultValue}")
                            } else {
                                append("${arg.name}: ${typeRef(arg.type)}")
                            }
                        }
                        append(")")
                    }
                    append(": ${typeRef(field.type)}")
                    if (field.isDeprecated) {
                        append(" @deprecated(reason: \"${field.deprecationReason}\")")
                    }
                    appendLine()
                }
                appendLine("}")
                appendLine()
            }
            TypeKind.INTERFACE -> {
                printedTypes.add(name)
                printDescription(type.description)
                append("interface $name")
                if (!type.possibleTypes.isNullOrEmpty()) {
                    append(" implements ")
                    append(type.possibleTypes!!.joinToString(", ") { it.name!! })
                }
                appendLine(" {")
                type.fields?.forEach { field ->
                    printDescription(field.description, "  ")
                    append("  ${field.name}")
                    if (field.args.isNotEmpty()) {
                        append("(")
                        field.args.forEachIndexed { index, arg ->
                            if (index > 0) append(", ")
                            if (arg.defaultValue != null) {
                                append("${arg.name}: ${typeRef(arg.type)} = ${arg.defaultValue}")
                            } else {
                                append("${arg.name}: ${typeRef(arg.type)}")
                            }
                        }
                        append(")")
                    }
                    append(": ${typeRef(field.type)}")
                    appendLine()
                }
                appendLine("}")
                appendLine()
            }
            TypeKind.INPUT_OBJECT -> {
                printedTypes.add(name)
                printDescription(type.description)
                appendLine("input $name {")
                type.inputFields?.forEach { field ->
                    printDescription(field.description, "  ")
                    append("  ${field.name}: ${typeRef(field.type)}")
                    if (field.defaultValue != null) {
                        append(" = ${field.defaultValue}")
                    }
                    appendLine()
                }
                appendLine("}")
                appendLine()
            }
            TypeKind.ENUM -> {
                printedTypes.add(name)
                printDescription(type.description)
                appendLine("enum $name {")
                type.enumValues?.forEach { value ->
                    printDescription(value.description, "  ")
                    append("  ${value.name}")
                    if (value.isDeprecated) {
                        append(" @deprecated(reason: \"${value.deprecationReason}\")")
                    }
                    appendLine()
                }
                appendLine("}")
                appendLine()
            }
            TypeKind.UNION -> {
                printedTypes.add(name)
                printDescription(type.description)
                appendLine("union $name = ${type.possibleTypes?.joinToString(" | ") { it.name!! } ?: ""}")
                appendLine()
            }
            TypeKind.SCALAR -> {
                printedTypes.add(name)
                printDescription(type.description)
                appendLine("scalar $name")
                appendLine()
            }
            else -> {}
        }
    }

    // Print Query type first
    val query = queryType
    if (query.name != null) {
        printDescription(query.description)
        appendLine("type ${query.name} {")
        query.fields?.forEach { field ->
            printDescription(field.description, "  ")
            append("  ${field.name}")
            if (field.args.isNotEmpty()) {
                append("(")
                field.args.forEachIndexed { index, arg ->
                    if (index > 0) append(", ")
                    if (arg.defaultValue != null) {
                        append("${arg.name}: ${typeRef(arg.type)} = ${arg.defaultValue}")
                    } else {
                        append("${arg.name}: ${typeRef(arg.type)}")
                    }
                }
                append(")")
            }
            append(": ${typeRef(field.type)}")
            appendLine()
        }
        appendLine("}")
        appendLine()
        printedTypes.add(query.name!!)
    }

    // Print Mutation type
    mutationType?.let { mutation ->
        if (mutation.name != null) {
            printDescription(mutation.description)
            appendLine("type ${mutation.name} {")
            mutation.fields?.forEach { field ->
                printDescription(field.description, "  ")
                append("  ${field.name}")
                if (field.args.isNotEmpty()) {
                    append("(")
                    field.args.forEachIndexed { index, arg ->
                        if (index > 0) append(", ")
                        if (arg.defaultValue != null) {
                            append("${arg.name}: ${typeRef(arg.type)} = ${arg.defaultValue}")
                        } else {
                            append("${arg.name}: ${typeRef(arg.type)}")
                        }
                    }
                    append(")")
                }
                append(": ${typeRef(field.type)}")
                appendLine()
            }
            appendLine("}")
            appendLine()
            printedTypes.add(mutation.name!!)
        }
    }

    // Print Subscription type
    subscriptionType?.let { subscription ->
        if (subscription.name != null) {
            printDescription(subscription.description)
            appendLine("type ${subscription.name} {")
            subscription.fields?.forEach { field ->
                printDescription(field.description, "  ")
                append("  ${field.name}")
                if (field.args.isNotEmpty()) {
                    append("(")
                    field.args.forEachIndexed { index, arg ->
                        if (index > 0) append(", ")
                        if (arg.defaultValue != null) {
                            append("${arg.name}: ${typeRef(arg.type)} = ${arg.defaultValue}")
                        } else {
                            append("${arg.name}: ${typeRef(arg.type)}")
                        }
                    }
                    append(")")
                }
                append(": ${typeRef(field.type)}")
                appendLine()
            }
            appendLine("}")
            appendLine()
            printedTypes.add(subscription.name!!)
        }
    }

    // Print all remaining types
    types.sortedBy { it.name ?: "" }.forEach { type ->
        printType(type)
    }

    // Remove trailing blank line
    if (endsWith("\n")) {
        deleteAt(length - 1)
    }
}