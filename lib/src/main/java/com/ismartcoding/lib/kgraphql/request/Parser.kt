package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.schema.directive.DirectiveLocation
import com.ismartcoding.lib.kgraphql.schema.model.ast.*
import com.ismartcoding.lib.kgraphql.schema.model.ast.OperationTypeNode.*
import com.ismartcoding.lib.kgraphql.schema.model.ast.TokenKindEnum.*

open class Parser {
    private val options: Options
    private val lexer: Lexer

    constructor(source: Source, options: Options? = null) {
        this.options = options ?: Options()
        lexer = Lexer(source)
    }

    constructor(source: String, options: Options? = null) : this(Source(source), options)

    constructor(lexer: Lexer, options: Options? = null) {
        this.options = options ?: Options()
        this.lexer = lexer
    }

    /**
     * Converts a name lex token into a name parse node.
     */
    private fun parseName(): NameNode {
        val token = expectToken(NAME)
        return NameNode(
            value = token.value!!,
            loc = loc(token)
        )
    }

    // Implements the parsing rules in the Document section.

    /**
     * Document : Definition+
     */
    fun parseDocument(): DocumentNode {
        val start = lexer.token

        return DocumentNode(
            definitions = many(
                SOF,
                ::parseDefinition,
                EOF
            ),
            loc = loc(start)
        )
    }

    /**
     * Definition :
     *   - ExecutableDefinition
     *   - TypeSystemDefinition
     *   - TypeSystemExtension
     *
     * ExecutableDefinition :
     *   - OperationDefinition
     *   - FragmentDefinition
     */
    private fun parseDefinition(): DefinitionNode {
        return when {
            peek(NAME) -> when (lexer.token.value) {
                "query", "mutation", "subscription" -> parseOperationDefinition()
                "fragment" -> parseFragmentDefinition()
                "schema", "scalar", "type", "interface", "union", "enum", "input", "directive" -> parseTypeSystemDefinition()
                "extend" -> throw NotImplementedError("Extend is not supported")
                else -> throw unexpected()
            }
            peek(BRACE_L) -> parseOperationDefinition()
            peekDescription() -> parseTypeSystemDefinition()
            else -> throw unexpected()
        }
    }



    /**
     * OperationDefinition :
     *  - SelectionSet
     *  - OperationType Name? VariableDefinitions? Directives? SelectionSet
     */
    private fun parseOperationDefinition(): DefinitionNode.ExecutableDefinitionNode.OperationDefinitionNode {
        val start = lexer.token
        if (peek(BRACE_L)) {
            return DefinitionNode.ExecutableDefinitionNode.OperationDefinitionNode(
                operation = QUERY,
                name = null,
                variableDefinitions = listOf(),
                directives = listOf(),
                selectionSet = parseSelectionSet(null),
                loc = loc(start)
            )
        }
        val operation = this.parseOperationType()
        var name: NameNode? = null
        if (peek(NAME)) {
            name = this.parseName()
        }
        return DefinitionNode.ExecutableDefinitionNode.OperationDefinitionNode(
            operation = operation,
            name = name,
            variableDefinitions = parseVariableDefinitions(),
            directives = parseDirectives(false),
            selectionSet = parseSelectionSet(null),
            loc = loc(start)
        )
    }

    /**
     * OperationType : one of query mutation subscription
     */
    private fun parseOperationType(): OperationTypeNode {
        val operationToken = expectToken(NAME)
        return when (operationToken.value) {
            "query" -> QUERY
            "mutation" -> MUTATION
            "subscription" -> SUBSCRIPTION
            else -> throw unexpected(operationToken)
        }
    }

    /**
     * VariableDefinitions : ( VariableDefinition+ )
     */
    private fun parseVariableDefinitions() = optionalMany(
        PAREN_L,
        ::parseVariableDefinition,
        PAREN_R
    )

    /**
     * VariableDefinition : Variable : Type DefaultValue? Directives{Const}?
     */
    @Suppress("ComplexRedundantLet")
    private fun parseVariableDefinition(): VariableDefinitionNode {
        val start = lexer.token
        return VariableDefinitionNode(
            variable = parseVariable(),
            type = expectToken(COLON).let { parseTypeReference() },
            defaultValue = if (expectOptionalToken(EQUALS) != null) parseValueLiteral(true) else null,
            directives = parseDirectives(true),
            loc = loc(start)
        )
    }

    /**
     * Variable : $ Name
     */
    private fun parseVariable(): ValueNode.VariableNode {
        val start = lexer.token
        expectToken(DOLLAR)
        return ValueNode.VariableNode(
            name = parseName(),
            loc = loc(start)
        )
    }

    /**
     * SelectionSet : { Selection+ }
     */
    private fun parseSelectionSet(parent: SelectionNode?): SelectionSetNode {
        val start = lexer.token
        return SelectionSetNode(
            selections = many(
                BRACE_L,
                { parseSelection(parent) },
                BRACE_R
            ),
            loc = loc(start)
        )
    }

    /**
     * Selection :
     *   - Field
     *   - FragmentSpread
     *   - InlineFragment
     */
    private fun parseSelection(parent: SelectionNode?): SelectionNode {
        return if (peek(SPREAD)) parseFragment(parent) else parseField(parent)
    }

    /**
     * Field : Alias? Name Arguments? Directives? SelectionSet?
     *
     * Alias : Name :
     */
    private fun parseField(parent: SelectionNode?): SelectionNode.FieldNode {
        val start = lexer.token

        val nameOrAlias = parseName()
        val alias: NameNode?
        val name: NameNode

        if (expectOptionalToken(COLON) != null) {
            alias = nameOrAlias
            name = parseName()
        } else {
            alias = null
            name = nameOrAlias
        }

        val newNode = SelectionNode.FieldNode(
            parent = parent,
            alias = alias,
            name = name,
            arguments = parseArguments(false),
            directives = parseDirectives(false)
        )
        return newNode.finalize(
            selectionSet = if (peek(BRACE_L)) parseSelectionSet(newNode) else null,
            loc = loc(start)
        )
    }

    /**
     * Arguments{Const} : ( Argument[?Const]+ )
     */
    private fun parseArguments(isConst: Boolean): MutableList<ArgumentNode> {
        val item = if (isConst) ::parseConstArgument else ::parseArgument
        return optionalMany(PAREN_L, item, PAREN_R)
    }

    /**
     * Argument{Const} : Name : Value[?Const]
     */
    private fun parseArgument(): ArgumentNode {
        val start = lexer.token
        val name = parseName()

        expectToken(COLON)
        return ArgumentNode(
            name = name,
            value = parseValueLiteral(false),
            loc = loc(start)
        )
    }

    @Suppress("ComplexRedundantLet")
    private fun parseConstArgument(): ArgumentNode {
        val start = lexer.token
        return ArgumentNode(
            name = parseName(),
            value = expectToken(COLON).let { parseValueLiteral(true) },
            loc = loc(start)
        )
    }

    /**
     * Corresponds to both FragmentSpread and InlineFragment in the spec.
     *
     * FragmentSpread : ... FragmentName Directives?
     *
     * InlineFragment : ... TypeCondition? Directives? SelectionSet
     */
    private fun parseFragment(parent: SelectionNode?): SelectionNode.FragmentNode {
        val start = lexer.token
        this.expectToken(SPREAD)

        val hasTypeCondition = expectOptionalKeyword("on")
        if (!hasTypeCondition && this.peek(NAME)) {
            return SelectionNode.FragmentNode.FragmentSpreadNode(
                parent = parent,
                name = parseFragmentName(),
                directives = parseDirectives(false),
                loc = loc(start)
            )
        }
        val newNode = SelectionNode.FragmentNode.InlineFragmentNode(
            parent = parent,
            typeCondition = if (hasTypeCondition) parseNamedType() else null,
            directives = parseDirectives(false)
        )

        return newNode.finalize(
            selectionSet = parseSelectionSet(newNode),
            loc = loc(start)
        )
    }

    /**
     * FragmentDefinition :
     *   - fragment FragmentName on TypeCondition Directives? SelectionSet
     *
     * TypeCondition : NamedType
     */
    @Suppress("ComplexRedundantLet")
    private fun parseFragmentDefinition(): DefinitionNode.ExecutableDefinitionNode.FragmentDefinitionNode {
        val start = lexer.token
        expectKeyword("fragment")
        return DefinitionNode.ExecutableDefinitionNode.FragmentDefinitionNode(
            name = parseFragmentName(),
            typeCondition = expectKeyword("on").let { parseNamedType() },
            directives = parseDirectives(false),
            selectionSet = parseSelectionSet(null),
            loc = loc(start)
        )
    }

    /**
     * FragmentName : Name but not `on`
     */
    private fun parseFragmentName(): NameNode {
        if (lexer.token.value == "on") {
            throw unexpected()
        }
        return parseName()
    }

    /**
     * Value{Const} :
     *   - [~Const] Variable
     *   - IntValue
     *   - FloatValue
     *   - StringValue
     *   - BooleanValue
     *   - NullValue
     *   - EnumValue
     *   - ListValue[?Const]
     *   - ObjectValue[?Const]
     *
     * BooleanValue : one of `true` `false`
     *
     * NullValue : `null`
     *
     * EnumValue : Name but not `true`, `false` or `null`
     */
    fun parseValueLiteral(isConst: Boolean): ValueNode {
        val token = lexer.token

        return when (token.kind) {
            BRACKET_L -> parseList(isConst)
            BRACE_L -> parseObject(isConst)
            INT -> {
                lexer.advance()
                ValueNode.NumberValueNode(
                    value = token.value!!,
                    loc = loc(token)
                )
            }
            FLOAT -> {
                lexer.advance()
                ValueNode.DoubleValueNode(
                    value = token.value!!,
                    loc = loc(token)
                )
            }
            STRING, BLOCK_STRING -> parseStringLiteral()
            NAME -> {
                if (token.value == "true" || token.value == "false") {
                    lexer.advance()
                    ValueNode.BooleanValueNode(
                        value = token.value == "true",
                        loc = loc(token)
                    )
                } else if (token.value == "null") {
                    lexer.advance()
                    ValueNode.NullValueNode(loc(token))
                } else {
                    lexer.advance()
                    ValueNode.EnumValueNode(
                        value = token.value!!,
                        loc = loc(token)
                    )
                }
            }
            DOLLAR -> if (!isConst) parseVariable() else throw unexpected()
            else -> throw unexpected()
        }
    }

    private fun parseStringLiteral(): ValueNode.StringValueNode {
        val token = lexer.token
        lexer.advance()
        return ValueNode.StringValueNode(
            value = token.value!!,
            block = token.kind == BLOCK_STRING,
            loc = loc(token)
        )
    }

    /**
     * ListValue{Const} :
     *   - [ ]
     *   - [ Value[?Const]+ ]
     */
    private fun parseList(isConst: Boolean): ValueNode.ListValueNode {
        val start = lexer.token
        val item = { this.parseValueLiteral(isConst) }

        return ValueNode.ListValueNode(
            values = any(BRACKET_L, item, BRACKET_R),
            loc = loc(start)
        )
    }

    /**
     * ObjectValue{Const} :
     *   - { }
     *   - { ObjectField[?Const]+ }
     */
    private fun parseObject(isConst: Boolean): ValueNode.ObjectValueNode {
        val start = lexer.token
        val item = { parseObjectField(isConst) }
        return ValueNode.ObjectValueNode(
            fields = any(BRACE_L, item, BRACE_R),
            loc = loc(start)
        )
    }

    /**
     * ObjectField{Const} : Name : Value[?Const]
     */
    private fun parseObjectField(isConst: Boolean): ValueNode.ObjectValueNode.ObjectFieldNode {
        val start = lexer.token
        val name = parseName()
        expectToken(COLON)

        return ValueNode.ObjectValueNode.ObjectFieldNode(
            name = name,
            value = parseValueLiteral(isConst),
            loc = loc(start)
        )
    }

    /**
     * Directives{Const} : Directive[?Const]+
     */
    private fun parseDirectives(isConst: Boolean): MutableList<DirectiveNode> {
        val directives = mutableListOf<DirectiveNode>()
        while (peek(AT)) {
            directives.add(parseDirective(isConst))
        }
        return directives
    }

    /**
     * Directive{Const} : @ Name Arguments[?Const]?
     */
    private fun parseDirective(isConst: Boolean): DirectiveNode {
        val start = lexer.token
        expectToken(AT)
        return DirectiveNode(
            name = parseName(),
            arguments = parseArguments(isConst),
            loc = loc(start)
        )
    }

    /**
     * Type :
     *   - NamedType
     *   - ListType
     *   - NonNullType
     */
    internal fun parseTypeReference(): TypeNode {
        val start = lexer.token
        var type: TypeNode?
        if (expectOptionalToken(BRACKET_L) != null) {
            type = parseTypeReference()
            expectToken(BRACKET_R)
            type = TypeNode.ListTypeNode(
                type = type,
                loc = loc(start)
            )
        } else {
            type = this.parseNamedType()
        }

        if (expectOptionalToken(BANG) != null) {
            return TypeNode.NonNullTypeNode(
                type = type,
                loc = loc(start)
            )
        }
        return type
    }

    /**
     * NamedType : Name
     */
    private fun parseNamedType(): TypeNode.NamedTypeNode {
        val start = lexer.token
        return TypeNode.NamedTypeNode(
            name = parseName(),
            loc = loc(start)
        )
    }

    /**
     * TypeSystemDefinition :
     *   - SchemaDefinition
     *   - TypeDefinition
     *   - DirectiveDefinition
     *
     * TypeDefinition :
     *   - ScalarTypeDefinition
     *   - ObjectTypeDefinition
     *   - InterfaceTypeDefinition
     *   - UnionTypeDefinition
     *   - EnumTypeDefinition
     *   - InputObjectTypeDefinition
     */
    private fun parseTypeSystemDefinition(): DefinitionNode.TypeSystemDefinitionNode {
        // Many definitions begin with a description and require a lookahead.
        val keywordToken = if (peekDescription()) lexer.lookahead() else lexer.token

        if (keywordToken.kind == NAME) {
            return when (keywordToken.value) {
                "schema" -> parseSchemaDefinition()
                "scalar" -> parseScalarTypeDefinition()
                "type" -> parseObjectTypeDefinition()
                "interface" -> parseInterfaceTypeDefinition()
                "union" -> parseUnionTypeDefinition()
                "enum" -> parseEnumTypeDefinition()
                "input" -> parseInputObjectTypeDefinition()
                "directive" -> parseDirectiveDefinition()
                else -> throw unexpected(keywordToken)
            }
        }
        throw unexpected(keywordToken)
    }

    private fun peekDescription() = peek(STRING) || peek(BLOCK_STRING)

    /**
     * Description : StringValue
     */
    private fun parseDescription() = if (peekDescription()) {
        parseStringLiteral()
    } else null

    /**
     * SchemaDefinition : schema Directives{Const}? { OperationTypeDefinition+ }
     */
    private fun parseSchemaDefinition(): DefinitionNode.TypeSystemDefinitionNode.SchemaDefinitionNode {
        val start = lexer.token
        expectKeyword("schema")
        val directives = parseDirectives(true)
        val operationTypes = many(
            BRACE_L,
            ::parseOperationTypeDefinition,
            BRACE_R
        )
        return DefinitionNode.TypeSystemDefinitionNode.SchemaDefinitionNode(
            directives = directives,
            operationTypes = operationTypes,
            loc = loc(start)
        )
    }

    /**
     * OperationTypeDefinition : OperationType : NamedType
     */
    private fun parseOperationTypeDefinition(): OperationTypeDefinitionNode {
        val start = lexer.token
        val operation = parseOperationType()
        expectToken(COLON)
        val type = parseNamedType()
        return OperationTypeDefinitionNode(
            operation = operation,
            type = type,
            loc = loc(start)
        )
    }

    /**
     * ScalarTypeDefinition : Description? scalar Name Directives{Const}?
     */
    private fun parseScalarTypeDefinition(): DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.ScalarTypeDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("scalar")
        val name = parseName()
        val directives = parseDirectives(true)
        return DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.ScalarTypeDefinitionNode(
            description = description,
            name = name,
            directives = directives,
            loc = loc(start)
        )
    }

    /**
     * ObjectTypeDefinition :
     *   Description?
     *   type Name ImplementsInterfaces? Directives{Const}? FieldsDefinition?
     */
    private fun parseObjectTypeDefinition(): DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.ObjectTypeDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("type")
        val name = parseName()
        val interfaces = parseImplementsInterfaces()
        val directives = parseDirectives(true)
        val fields = parseFieldsDefinition()
        return DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.ObjectTypeDefinitionNode(
            description = description,
            name = name,
            interfaces = interfaces,
            directives = directives,
            fields = fields,
            loc = loc(start)
        )
    }

    /**
     * ImplementsInterfaces :
     *   - implements `&`? NamedType
     *   - ImplementsInterfaces & NamedType
     */
    private fun parseImplementsInterfaces(): MutableList<TypeNode.NamedTypeNode> {
        val types = mutableListOf<TypeNode.NamedTypeNode>()
        if (this.expectOptionalKeyword("implements")) {
            // Optional leading ampersand
            expectOptionalToken(AMP)
            do {
                types.add(parseNamedType())
            } while (
                expectOptionalToken(AMP) != null ||
                // Legacy support for the SDL?
                (options.allowLegacySDLImplementsInterfaces && peek(NAME))
            )
        }
        return types
    }

    /**
     * FieldsDefinition : { FieldDefinition+ }
     */
    private fun parseFieldsDefinition(): MutableList<FieldDefinitionNode> {
        // Legacy support for the SDL?
        if (
            options.allowLegacySDLEmptyFields &&
            peek(BRACE_L) &&
            lexer.lookahead().kind == BRACE_R
        ) {
            lexer.advance()
            lexer.advance()
            return mutableListOf()
        }
        return optionalMany(
            BRACE_L,
            ::parseFieldDefinition,
            BRACE_R
        )
    }

    /**
     * FieldDefinition :
     *   - Description? Name ArgumentsDefinition? : Type Directives{Const}?
     */
    private fun parseFieldDefinition(): FieldDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        val name = parseName()
        val args = parseArgumentDefs()
        expectToken(COLON)
        val type = parseTypeReference()
        val directives = parseDirectives(true)

        return FieldDefinitionNode(
            description = description,
            name = name,
            arguments = args,
            type = type,
            directives = directives,
            loc = loc(start)
        )
    }

    /**
     * ArgumentsDefinition : ( InputValueDefinition+ )
     */
    private fun parseArgumentDefs() = optionalMany(
        PAREN_L,
        ::parseInputValueDef,
        PAREN_R
    )

    /**
     * InputValueDefinition :
     *   - Description? Name : Type DefaultValue? Directives{Const}?
     */
    private fun parseInputValueDef(): InputValueDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        val name = parseName()
        expectToken(COLON)
        val type = parseTypeReference()
        val defaultValue = expectOptionalToken(EQUALS)?.let { parseValueLiteral(true) }
        val directives = parseDirectives(true)

        return InputValueDefinitionNode(
            description = description,
            name = name,
            type = type,
            defaultValue = defaultValue,
            directives = directives,
            loc = loc(start)
        )
    }

    /**
     * InterfaceTypeDefinition :
     *   - Description? interface Name Directives{Const}? FieldsDefinition?
     */
    private fun parseInterfaceTypeDefinition(): DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.InterfaceTypeDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("interface")
        val name = parseName()
        val directives = parseDirectives(true)
        val fields = parseFieldsDefinition()

        return DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.InterfaceTypeDefinitionNode(
            description = description,
            name = name,
            directives = directives,
            fields = fields,
            loc = loc(start)
        )
    }

    /**
     * UnionTypeDefinition :
     *   - Description? union Name Directives{Const}? UnionMemberTypes?
     */
    private fun parseUnionTypeDefinition(): DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.UnionTypeDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("union")
        val name = parseName()
        val directives = parseDirectives(true)
        val types = parseUnionMemberTypes()

        return DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.UnionTypeDefinitionNode(
            description = description,
            name = name,
            directives = directives,
            types = types,
            loc = loc(start)
        )
    }

    /**
     * UnionMemberTypes :
     *   - = `|`? NamedType
     *   - UnionMemberTypes | NamedType
     */
    private fun parseUnionMemberTypes(): MutableList<TypeNode.NamedTypeNode> {
        val types = mutableListOf<TypeNode.NamedTypeNode>()
        if (expectOptionalToken(EQUALS) != null) {
            // Optional leading pipe
            expectOptionalToken(PIPE)
            do {
                types.add(parseNamedType())
            } while (expectOptionalToken(PIPE) != null)
        }
        return types
    }

    /**
     * EnumTypeDefinition :
     *   - Description? enum Name Directives{Const}? EnumValuesDefinition?
     */
    private fun parseEnumTypeDefinition(): DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.EnumTypeDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("enum")
        val name = parseName()
        val directives = parseDirectives(true)
        val values = parseEnumValuesDefinition()
        return DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.EnumTypeDefinitionNode(
            description = description,
            name = name,
            directives = directives,
            values = values,
            loc = loc(start)
        )
    }

    /**
     * EnumValuesDefinition : { EnumValueDefinition+ }
     */
    private fun parseEnumValuesDefinition() = optionalMany(
        BRACE_L,
        ::parseEnumValueDefinition,
        BRACE_R
    )

    /**
     * EnumValueDefinition : Description? EnumValue Directives{Const}?
     *
     * EnumValue : Name
     */
    private fun parseEnumValueDefinition(): EnumValueDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        val name = parseName()
        val directives = parseDirectives(true)

        return EnumValueDefinitionNode(
            description = description,
            name = name,
            directives = directives,
            loc = loc(start)
        )
    }

    /**
     * InputObjectTypeDefinition :
     *   - Description? input Name Directives{Const}? InputFieldsDefinition?
     */
    private fun parseInputObjectTypeDefinition(): DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.InputObjectTypeDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("input")
        val name = parseName()
        val directives = parseDirectives(true)
        val fields = parseInputFieldsDefinition()

        return DefinitionNode.TypeSystemDefinitionNode.TypeDefinitionNode.InputObjectTypeDefinitionNode(
            description = description,
            name = name,
            directives = directives,
            fields = fields,
            loc = loc(start)
        )
    }

    /**
     * InputFieldsDefinition : { InputValueDefinition+ }
     */
    private fun parseInputFieldsDefinition() = optionalMany(
        BRACE_L,
        ::parseInputValueDef,
        BRACE_R
    )

    /**
     * DirectiveDefinition :
     *   - Description? directive @ Name ArgumentsDefinition? `repeatable`? on DirectiveLocations
     */
    private fun parseDirectiveDefinition(): DefinitionNode.TypeSystemDefinitionNode.DirectiveDefinitionNode {
        val start = lexer.token
        val description = parseDescription()
        expectKeyword("directive")
        expectToken(AT)
        val name = parseName()
        val args = parseArgumentDefs()
        val repeatable = expectOptionalKeyword("repeatable")
        expectKeyword("on")
        val locations = parseDirectiveLocations()

        return DefinitionNode.TypeSystemDefinitionNode.DirectiveDefinitionNode(
            description = description,
            name = name,
            arguments = args,
            repeatable = repeatable,
            locations = locations,
            loc = loc(start)
        )
    }

    /**
     * DirectiveLocations :
     *   - `|`? DirectiveLocation
     *   - DirectiveLocations | DirectiveLocation
     */
    private fun parseDirectiveLocations(): MutableList<NameNode> {
        // Optional leading pipe
        expectOptionalToken(PIPE)
        val locations = mutableListOf<NameNode>()
        do {
            locations.add(parseDirectiveLocation())
        } while (expectOptionalToken(PIPE) != null)
        return locations
    }

    /*
     * DirectiveLocation :
     *   - ExecutableDirectiveLocation
     *   - TypeSystemDirectiveLocation
     *
     * ExecutableDirectiveLocation : one of
     *   `QUERY`
     *   `MUTATION`
     *   `SUBSCRIPTION`
     *   `FIELD`
     *   `FRAGMENT_DEFINITION`
     *   `FRAGMENT_SPREAD`
     *   `INLINE_FRAGMENT`
     *
     * TypeSystemDirectiveLocation : one of
     *   `SCHEMA`
     *   `SCALAR`
     *   `OBJECT`
     *   `FIELD_DEFINITION`
     *   `ARGUMENT_DEFINITION`
     *   `INTERFACE`
     *   `UNION`
     *   `ENUM`
     *   `ENUM_VALUE`
     *   `INPUT_OBJECT`
     *   `INPUT_FIELD_DEFINITION`
     */
    private fun parseDirectiveLocation(): NameNode {
        val start = lexer.token
        val name = parseName()
        if (DirectiveLocation.from(name.value) != null) {
            return name
        }
        throw unexpected(start)
    }

    /**
     * Returns a location object, used to identify the place in
     * the source that created a given parsed object.
     */
    private fun loc(startToken: Token): Location? {
        if (options.noLocation != true) {
            return Location(startToken, lexer.lastToken, lexer.source)
        }
        return null
    }

    /**
     * Determines if the next token is of a given kind
     */
    private fun peek(kind: TokenKindEnum) = lexer.token.kind == kind

    /**
     * If the next token is of the given kind, return that token after advancing
     * the lexer. Otherwise, do not change the parser state and throw an error.
     */
    internal fun expectToken(kind: TokenKindEnum): Token {
        val token = lexer.token
        if (token.kind == kind) {
            lexer.advance()
            return token
        }

        throw syntaxError(
            lexer.source,
            token.start,
            "Expected ${getTokenKindDesc(kind)}, found ${getTokenDesc(
                token
            )}."
        )
    }

    /**
     * If the next token is of the given kind, return that token after advancing
     * the lexer. Otherwise, do not change the parser state and return undefined.
     */
    private fun expectOptionalToken(kind: TokenKindEnum): Token? {
        val token = lexer.token
        if (token.kind == kind) {
            lexer.advance()
            return token
        }
        return null
    }

    /**
     * If the next token is a given keyword, advance the lexer.
     * Otherwise, do not change the parser state and throw an error.
     */
    private fun expectKeyword(value: String) {
        val token = lexer.token
        if (token.kind == NAME && token.value == value) {
            lexer.advance()
        } else {
            throw syntaxError(
                lexer.source,
                token.start,
                "Expected \"${value}\", found ${getTokenDesc(token)}."
            )
        }
    }

    /**
     * If the next token is a given keyword, return "true" after advancing
     * the lexer. Otherwise, do not change the parser state and return "false".
     */
    private fun expectOptionalKeyword(value: String): Boolean {
        val token = lexer.token
        if (token.kind == NAME && token.value == value) {
            lexer.advance()
            return true
        }
        return false
    }

    /**
     * Helper function for creating an error when an unexpected lexed token
     * is encountered.
     */
    private fun unexpected(token: Token = lexer.token) = syntaxError(
        lexer.source,
        token.start,
        "Unexpected ${getTokenDesc(token)}."
    )

    /**
     * Returns a possibly empty list of parse nodes, determined by
     * the parseFn. This list begins with a lex token of openKind
     * and ends with a lex token of closeKind. Advances the parser
     * to the next lex token after the closing token.
     */
    private fun <T> any(
        openKind: TokenKindEnum,
        parseFn: () -> T,
        closeKind: TokenKindEnum
    ): MutableList<T> {
        expectToken(openKind)
        val nodes = mutableListOf<T>()
        while (expectOptionalToken(closeKind) == null) {
            nodes.add(parseFn())
        }
        return nodes
    }

    /**
     * Returns a list of parse nodes, determined by the parseFn.
     * It can be empty only if open token is missing otherwise it will always
     * return non-empty list that begins with a lex token of openKind and ends
     * with a lex token of closeKind. Advances the parser to the next lex token
     * after the closing token.
     */
    private fun <T> optionalMany(
        openKind: TokenKindEnum,
        parseFn: () -> T,
        closeKind: TokenKindEnum
    ): MutableList<T> {
        if (expectOptionalToken(openKind) != null) {
            val nodes = mutableListOf<T>()
            do {
                nodes.add(parseFn())
            } while (this.expectOptionalToken(closeKind) == null)
            return nodes
        }
        return mutableListOf()
    }

    /**
     * Returns a non-empty list of parse nodes, determined by
     * the parseFn. This list begins with a lex token of openKind
     * and ends with a lex token of closeKind. Advances the parser
     * to the next lex token after the closing token.
     */
    private fun <T> many(
        openKind: TokenKindEnum,
        parseFn: () -> T,
        closeKind: TokenKindEnum
    ): MutableList<T> {
        expectToken(openKind)
        val nodes = mutableListOf<T>()
        do {
            nodes.add(parseFn())
        } while (expectOptionalToken(closeKind) == null)
        return nodes
    }

    /**
     * Configuration options to control parser behavior
     */
    data class Options(
        /**
         * By default, the parser creates AST nodes that know the location
         * in the source that they correspond to. This configuration flag
         * disables that behavior for performance or testing.
         */
        val noLocation: Boolean? = null,

        /**
         * If enabled, the parser will parse empty fields sets in the Schema
         * Definition Language. Otherwise, the parser will follow the current
         * specification.
         *
         * This option is provided to ease adoption of the final SDL specification
         * and will be removed in v16.
         */
        val allowLegacySDLEmptyFields: Boolean = false,

        /**
         * If enabled, the parser will parse implemented interfaces with no `&`
         * character between each interface. Otherwise, the parser will follow the
         * current specification.
         *
         * This option is provided to ease adoption of the final SDL specification
         * and will be removed in v16.
         */
        val allowLegacySDLImplementsInterfaces: Boolean = false
    )

    companion object {

        /**
         * A helper function to describe a token as a string for debugging
         */
        fun getTokenDesc(token: Token): String {
            val value = token.value
            return getTokenKindDesc(token.kind) + (if (value != null) " \"$value\"" else "")
        }

        /**
         * A helper function to describe a token kind as a string for debugging
         */
        fun getTokenKindDesc(kind: TokenKindEnum): String {
            return if (kind.isPunctuatorTokenKind) "\"$kind\"" else kind.str
        }
    }
}
