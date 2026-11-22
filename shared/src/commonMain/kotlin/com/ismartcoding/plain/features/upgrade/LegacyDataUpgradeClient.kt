package com.ismartcoding.plain.features.upgrade

import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.variable
import kotlinx.serialization.Serializable

@Serializable
internal enum class LegacyDataUpgradeStatus {
    UNAVAILABLE, PENDING, SKIPPED, IMPORTED, CLEARED,
}

@Serializable
internal enum class LegacyDataUpgradeAction {
    SKIP, IMPORT, CLEAR,
}

internal object LegacyDataUpgradeClient {
    fun observe() = AppSubscriptions.subscription<LegacyDataUpgradeStatus>("legacyDataUpgradeChanged")

    suspend fun update(action: LegacyDataUpgradeAction): LegacyDataUpgradeStatus = GraphqlApi.execute(GraphqlRequest(
        "mutation UpgradeLegacyData(\$action: LegacyDataUpgradeAction!) { updateLegacyDataUpgrade(action: \$action) }",
        variables = mapOf(variable("action", action)),
        field = "updateLegacyDataUpgrade",
    ))
}
