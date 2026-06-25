package com.ismartcoding.plain.lib.kgraphql.schema.model.ast

import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ValueNode.StringValueNode

data class EnumValueDefinitionNode(
    override val loc: Location?,
    val name: NameNode,
    val description: ValueNode.StringValueNode?,
    val directives: List<DirectiveNode>?
): ASTNode()
