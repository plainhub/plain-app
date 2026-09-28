package com.ismartcoding.plain.lib.kgraphql.schema.model.ast

import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ValueNode.StringValueNode

data class InputValueDefinitionNode(
    override val loc: Location?,
    val name: NameNode,
    val description: ValueNode.StringValueNode?,
    val type: TypeNode,
    val defaultValue: ValueNode?,
    val directives: List<DirectiveNode>?
): ASTNode()
