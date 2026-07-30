package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.web.HttpRouteRegistry
import com.ismartcoding.plain.web.http.HttpRouter

/**
 * Register the GraphQL routes (`/graphql` and `/peer_graphql`) against
 * [router]. The actual handlers live in [MainGraphQLService] and
 * [PeerGraphQLService] in commonMain.
 *
 * The services are accessed lazily via [HttpRouteRegistry] inside the route
 * handlers so that router initialization does NOT trigger GraphQL schema
 * creation. This is important on iOS where KGraphQL reflection is not
 * supported — the router can still be built and serve non-GraphQL routes
 * (static files, system endpoints, etc.) without the schema throwing.
 */
fun HttpRouter.addGraphQLRoutes() {
    post("/graphql") { call -> HttpRouteRegistry.mainGraphQL.handle(call) }
    post("/peer_graphql") { call -> HttpRouteRegistry.peerGraphQL.handle(call) }
}
