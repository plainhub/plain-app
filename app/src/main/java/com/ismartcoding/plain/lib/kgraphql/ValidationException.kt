package com.ismartcoding.plain.lib.kgraphql

import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ASTNode

class ValidationException(message: String, nodes: List<ASTNode>? = null): GraphQLError(message, nodes = nodes)
