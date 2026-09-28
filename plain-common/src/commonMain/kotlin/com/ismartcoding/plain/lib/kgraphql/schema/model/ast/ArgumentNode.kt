package com.ismartcoding.plain.lib.kgraphql.schema.model.ast

data class ArgumentNode(
    override val loc: Location?,
    val name: NameNode,
    val value: ValueNode
): ASTNode()

fun List<ArgumentNode>.toArguments() =
    ArgumentNodes(this)


class ArgumentNodes() : MutableMap<String, ValueNode> by mutableMapOf() {
    constructor(argumentNodes: List<ArgumentNode>): this() {
        argumentNodes.forEach {
            put(it.name.value, it.value)
        }
    }
}
