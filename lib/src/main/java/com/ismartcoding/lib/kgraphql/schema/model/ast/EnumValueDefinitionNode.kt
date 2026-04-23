package com.ismartcoding.lib.kgraphql.schema.model.ast

import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode.StringValueNode

data class EnumValueDefinitionNode(
    override val loc: Location?,
    val name: NameNode,
    val description: StringValueNode?,
    val directives: List<DirectiveNode>?
): ASTNode()
