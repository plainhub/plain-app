package com.ismartcoding.plain.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.jsonPrimitive

internal class ToolsViewModel(
    private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient,
    private val changes: Flow<Unit>,
) : ViewModel() {
    private val features = MutableStateFlow<List<String>>(emptyList())
    val enabledFeatures = features.asStateFlow()

    suspend fun observe() = coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) { changes.collect { load() } }
        load()
    }

    suspend fun load() {
        val values = api.execute(Query.prefs())
        features.value = Prefs.parseFeatures(values.getValue(PrefKeys.APP_HOME_FEATURES).jsonPrimitive.content)
    }

    suspend fun save(values: List<String>) {
        api.execute(Mutation.setPref(PrefKeys.APP_HOME_FEATURES, JsonHelper.jsonEncodeToElement(Prefs.formatFeatures(values))))
        features.value = values
    }
}

@Composable
internal fun rememberToolsViewModel(): ToolsViewModel {
    val source = LocalContentSource.current
    val changes = remember(source) {
        when (source) {
            ContentSource.Local -> Prefs.appHomeFeatures.drop(1).map { Unit }
            is ContentSource.Remote -> source.client.changes(listOf(GraphqlOperation("subscription { preferencesChanged }")))
        }
    }
    val model = viewModel(key = "tools:${source.key}") { ToolsViewModel(source.api, changes) }
    LaunchedEffect(model) { launchSafe { model.observe() } }
    return model
}
