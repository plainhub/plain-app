package com.ismartcoding.lib.kgraphql.schema.model.ast

data class ArgumentNode(
    override val loc: Location?,
    val name: NameNode,
    val value: ValueNode
): ASTNode()

fun List<ArgumentNode>.toArguments() =
    ArgumentNodes(this)


class ArgumentNodes(): HashMap<String, ValueNode>() {
    constructor(argumentNodes: List<ArgumentNode>): this() {
        argumentNodes.forEach {
            put(it.name.value, it.value)
        }
    }
}
