package com.ismartcoding.lib.kgraphql

import com.ismartcoding.lib.kgraphql.schema.model.ast.ASTNode

class ValidationException(message: String, nodes: List<ASTNode>? = null): GraphQLError(message, nodes = nodes)
