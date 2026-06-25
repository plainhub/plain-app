package com.ismartcoding.plain.lib.kgraphql.schema.model.ast

import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ValueNode.StringValueNode

data class FieldDefinitionNode(
    override val loc: Location?,
    val name: NameNode,
    val description: ValueNode.StringValueNode?,
    val arguments: List<InputValueDefinitionNode>?,
    val type: TypeNode,
    val directives: List<DirectiveNode>?
): ASTNode()
