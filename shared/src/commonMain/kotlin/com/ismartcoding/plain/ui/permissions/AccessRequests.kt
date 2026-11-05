package com.ismartcoding.plain.ui.permissions

import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.ui.components.DialogHelper
import org.jetbrains.compose.resources.getString

internal suspend fun requestAccess(api: GraphqlClient, feature: AccessFeature) {
    if (!api.execute(Mutation.requestAccess(feature))) {
        DialogHelper.showMessage(getString(Res.string.not_supported_error))
    }
}
