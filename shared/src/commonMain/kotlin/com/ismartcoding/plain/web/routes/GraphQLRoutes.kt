package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.web.graphql.MainGraphQLService
import com.ismartcoding.plain.web.graphql.PeerGraphQLService
import com.ismartcoding.plain.web.http.HttpRouter

/**
 * Register the GraphQL routes (`/graphql` and `/peer_graphql`) against
 * [router]. The actual handlers live in [MainGraphQLService] and
 * [PeerGraphQLService] in commonMain, so the platform layer only needs to
 * build the services and pass them through.
 */
fun HttpRouter.addGraphQLRoutes(
    mainService: MainGraphQLService,
    peerService: PeerGraphQLService,
) {
    post("/graphql") { call -> mainService.handle(call) }
    post("/peer_graphql") { call -> peerService.handle(call) }
}
