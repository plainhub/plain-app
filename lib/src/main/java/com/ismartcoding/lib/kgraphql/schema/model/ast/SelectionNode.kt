package com.ismartcoding.lib.kgraphql.schema.model.ast

import com.ismartcoding.lib.kgraphql.schema.model.ast.TypeNode.NamedTypeNode

sealed class SelectionNode(val parent: SelectionNode?): ASTNode() {

    abstract val fullPath: String

    class FieldNode(
        parent: SelectionNode?,
        val alias: NameNode?,
        val name: NameNode,
        val arguments: List<ArgumentNode>?,
        val directives: List<DirectiveNode>?
    ): SelectionNode(parent) {
        private var _selectionSet: SelectionSetNode? = null
        private var _loc: Location? = null

        override val loc get() = _loc
        val selectionSet get() = _selectionSet

        internal fun finalize(selectionSet: SelectionSetNode?, loc: Location?): FieldNode {
            _selectionSet = selectionSet
            _loc = loc
            return this
        }

        val aliasOrName get() = alias ?: name

        override val fullPath get() = (parent?.fullPath?.let { "$it." } ?: "") + aliasOrName.value

    }

    sealed class FragmentNode(parent: SelectionNode?, val directives: List<DirectiveNode>?): SelectionNode(parent) {
        override val fullPath get () = parent?.fullPath?.let {"$it."} ?: ""

        /**
         * ...FragmentName
         */
        class FragmentSpreadNode(
            parent: SelectionNode?,
            override val loc: Location?,
            val name: NameNode,
            directives: List<DirectiveNode>?
        ): FragmentNode(parent, directives)

        /**
         * ... on Type {
         *   [...]
         *   [...]
         * }
         */
        class InlineFragmentNode(
            parent: SelectionNode?,
            val typeCondition: NamedTypeNode?,
            directives: List<DirectiveNode>?
        ): FragmentNode(parent, directives) {
            private var _selectionSet: SelectionSetNode? = null
            private var _loc: Location? = null

            override val loc get() = _loc
            val selectionSet get() = _selectionSet!!

            internal fun finalize(selectionSet: SelectionSetNode?, loc: Location?): InlineFragmentNode {
                _selectionSet = selectionSet
                _loc = loc
                return this
            }
        }
    }

}
