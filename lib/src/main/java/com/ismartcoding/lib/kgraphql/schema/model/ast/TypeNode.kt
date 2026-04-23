package com.ismartcoding.lib.kgraphql.schema.model.ast

sealed class TypeNode(override val loc: Location?): ASTNode() {

    class NamedTypeNode(loc: Location?, val name: NameNode): TypeNode(loc)
    class ListTypeNode(loc: Location?, val type: TypeNode): TypeNode(loc) {
        val isElementNullable get() = type !is NonNullTypeNode
    }
    class NonNullTypeNode(loc: Location?, val type: TypeNode): TypeNode(loc)


    val isNullable get() = this !is NonNullTypeNode

    val isList get() = this is ListTypeNode || (this is NonNullTypeNode && type is ListTypeNode)

    val nameNode get(): NameNode = when (this) {
        is NonNullTypeNode -> type.nameNode
        is ListTypeNode -> type.nameNode
        is NamedTypeNode -> name
    }
}
