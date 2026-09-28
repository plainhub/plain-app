package com.ismartcoding.plain.lib.kgraphql.annotations

/**
 * Marks a data class as a GraphQL Object type. KSP scans the class at compile
 * time and generates a [com.ismartcoding.plain.lib.kgraphql.generated.TypeDescriptor]
 * that enumerates every constructor-declared property as a GraphQL field.
 *
 * No runtime reflection is used — the generated descriptor references each
 * property via `::foo` (KMP-compatible) and reads values via `KProperty1.get()`.
 *
 * Example:
 * ```
 * @GraphQLType
 * @Serializable
 * data class ChatItem(val id: ID, val content: String, ...)
 * ```
 *
 * @param name Optional GraphQL type name. Defaults to the simple class name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLType(val name: String = "")

/**
 * Marks a data class as a GraphQL Input type. KSP generates an
 * [com.ismartcoding.plain.lib.kgraphql.generated.InputDescriptor] and binds the
 * kotlinx.serialization [KSerializer] so GraphQL input objects are deserialized
 * without runtime reflection.
 *
 * The class MUST also be `@Serializable`.
 *
 * @param name Optional GraphQL type name. Defaults to the simple class name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLInput(val name: String = "")

/**
 * Marks an interface as a GraphQL Interface type. KSP scans all classes in the
 * same compilation that implement the interface and registers them as
 * `possibleTypes`, eliminating the need for `isKotlinSubclassOf` reflection.
 *
 * @param name Optional GraphQL type name. Defaults to the simple class name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLInterface(val name: String = "")

/**
 * Marks a sealed class as a GraphQL Union type. KSP reads
 * `KSClassDeclaration.getSealedSubclasses()` at compile time and generates a
 * [com.ismartcoding.plain.lib.kgraphql.generated.UnionDescriptor] with all
 * member classes — no `sealedSubclasses` reflection at runtime.
 *
 * @param name Optional GraphQL type name. Defaults to the simple class name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLUnion(val name: String = "")

/**
 * Excludes a property from the GraphQL schema. Use on fields that should not
 * be exposed (caches, internal state, etc.) — equivalent to the legacy
 * `@NotIntrospected` annotation but evaluated at compile time by KSP.
 *
 * Example:
 * ```
 * @GraphQLType
 * @Serializable
 * data class ChatItem(
 *     val id: ID,
 *     @GraphQLIgnore val internalCache: String = "",
 * )
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLIgnore

/**
 * Overrides the GraphQL field name and/or attaches a description for a
 * property. Use when the Kotlin property name should differ from the exposed
 * GraphQL field name.
 *
 * Example:
 * ```
 * @GraphQLType
 * data class User(
 *     @GraphQLField(name = "displayName", description = "User-facing name")
 *     val name: String,
 * )
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLField(
    val name: String = "",
    val description: String = "",
)

enum class GraphQLSchemaTarget { MAIN, PEER, GUEST }

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLQuery(
    val name: String = "",
    val description: String = "",
    val target: GraphQLSchemaTarget = GraphQLSchemaTarget.MAIN,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLMutation(
    val name: String = "",
    val description: String = "",
    val target: GraphQLSchemaTarget = GraphQLSchemaTarget.MAIN,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphQLSubscription(
    val name: String = "",
    val description: String = "",
    val target: GraphQLSchemaTarget = GraphQLSchemaTarget.MAIN,
)
