package com.ismartcoding.lib.kgraphql.schema.model.ast

import com.ismartcoding.lib.kgraphql.schema.model.ast.TypeNode.NamedTypeNode
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode.*


sealed class DefinitionNode(override val loc: Location?): ASTNode() {

    sealed class ExecutableDefinitionNode(
        loc: Location?,
        val name: NameNode?,
        val variableDefinitions: List<VariableDefinitionNode>?,
        val directives: List<DirectiveNode>?,
        val selectionSet: SelectionSetNode
    ): DefinitionNode(loc) {
        class FragmentDefinitionNode(
            loc: Location?,
            name: NameNode,
            directives: List<DirectiveNode> = listOf(),
            selectionSet: SelectionSetNode,
            val typeCondition: NamedTypeNode
        ) : ExecutableDefinitionNode(loc, name, emptyList(), directives, selectionSet)

        class OperationDefinitionNode(
            loc: Location?,
            name: NameNode? = null,
            variableDefinitions: List<VariableDefinitionNode>?,
            directives: List<DirectiveNode>?,
            selectionSet: SelectionSetNode,
            val operation: OperationTypeNode
        ): ExecutableDefinitionNode(loc, name, variableDefinitions, directives, selectionSet)
    }

    /**
     * [TypeSystemDefinitionNode] is currently not used for anything
     */
    sealed class TypeSystemDefinitionNode(loc: Location?): DefinitionNode(loc) {
        class SchemaDefinitionNode(
            val directives: List<DirectiveNode> = listOf(),
            val operationTypes: List<OperationTypeDefinitionNode>,
            loc: Location?
        ): TypeSystemDefinitionNode(loc)

        sealed class TypeDefinitionNode(
            loc: Location?,
            val name: NameNode,
            val description: StringValueNode?,
            val directives: List<DirectiveNode>?
        ): TypeSystemDefinitionNode(loc) {

            class EnumTypeDefinitionNode(
                loc: Location?,
                name: NameNode,
                description: StringValueNode?,
                directives: List<DirectiveNode>?,
                val values: List<EnumValueDefinitionNode>?
            ): TypeDefinitionNode(loc, name, description, directives)

            class InputObjectTypeDefinitionNode(
                loc: Location?,
                name: NameNode,
                description: StringValueNode?,
                directives: List<DirectiveNode>?,
                val fields: List<InputValueDefinitionNode>?
            ): TypeDefinitionNode(loc, name, description, directives)

            class InterfaceTypeDefinitionNode(
                loc: Location?,
                name: NameNode,
                description: StringValueNode?,
                directives: List<DirectiveNode>?,
                val fields: List<FieldDefinitionNode>?
            ): TypeDefinitionNode(loc, name, description, directives)

            class ObjectTypeDefinitionNode(
                loc: Location?,
                name: NameNode,
                description: StringValueNode?,
                directives: List<DirectiveNode>?,
                val interfaces: List<NamedTypeNode>?,
                val fields: List<FieldDefinitionNode>?
            ): TypeDefinitionNode(loc, name, description, directives)

            class ScalarTypeDefinitionNode(
                loc: Location?,
                name: NameNode,
                description: StringValueNode?,
                directives: List<DirectiveNode>?
            ): TypeDefinitionNode(loc, name, description, directives)

            class UnionTypeDefinitionNode(
                loc: Location?,
                name: NameNode,
                description: StringValueNode?,
                directives: List<DirectiveNode>?,
                val types: List<NamedTypeNode>?
            ): TypeDefinitionNode(loc, name, description, directives)
        }

        class DirectiveDefinitionNode(
            val description: StringValueNode?,
            val name: NameNode,
            val arguments: List<InputValueDefinitionNode> = listOf(),
            val repeatable: Boolean,
            val locations: List<NameNode>,
            loc: Location?
        ): TypeSystemDefinitionNode(loc)
    }

    sealed class TypeSystemExtensionNode(loc: Location): DefinitionNode(loc) {
        // Nothing at the moment. [ast.js:203]
    }
}
