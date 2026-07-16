package com.ismartcoding.plain.web.http

/**
 * Request-scoped context exposed to GraphQL resolvers via KGraphQL's
 * `Context` mechanism. The platform layer constructs an instance for each
 * request and injects it into the KGraphQL `Context` so resolvers can read
 * headers and per-request attributes (e.g. the peer signature and timestamp
 * set during request decryption) without depending on `ApplicationCall`.
 *
 * The implementation deliberately holds an [HttpCall] reference rather than
 * platform-specific types, keeping resolvers in commonMain. Resolvers fetch
 * it via `context.get<GraphqlRequestContext>()` (which uses the reified
 * class as the lookup key, matching how KGraphQL stores components).
 */
class GraphqlRequestContext(
    val call: HttpCall,
) {
    /** Mutable per-request attributes, used by the peer-graphql route to
     *  carry the verified signature and timestamp to the resolver. */
    val attributes: MutableMap<String, Any?> = mutableMapOf()

    fun header(name: String): String? = call.header(name)

    fun setAttribute(name: String, value: Any?) {
        attributes[name] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(name: String): T? = attributes[name] as T?
}
