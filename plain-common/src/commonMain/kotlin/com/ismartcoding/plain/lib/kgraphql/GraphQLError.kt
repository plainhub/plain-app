package com.ismartcoding.plain.lib.kgraphql

/**
 * Failure on a bridge-served route that must be rendered back to the caller as
 * a GraphQL `errors` payload.
 *
 * The public `/graphql` and the peer/guest GraphQL endpoints run in Rust and
 * render their own errors; this type only covers the Kotlin routes that throw
 * through `handleGraphQLError`. It carries a message and nothing else — there
 * is no request AST to point a location at, so no error location is reported.
 */
class GraphQLError(message: String) : Exception(message)