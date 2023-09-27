package com.ismartcoding.plain.api

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.network.okHttpClient
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.features.BoxConnectivityStateChangedEvent
import com.ismartcoding.plain.features.bluetooth.BluetoothUtil
import com.ismartcoding.plain.features.bluetooth.SmartBTDevice
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BoxApi {
    private var clients = mutableMapOf<String, ApolloClient>()
    private val mutex = Mutex()

    suspend fun <D : Mutation.Data> mixMutateAsync(
        mutation: Mutation<D>,
        box: DBox? = UIDataCache.current().box,
        timeout: Int = HttpApiTimeout.DEFAULT_SECONDS,
    ): GraphqlApiResult<D> {
        if (box == null) {
            return GraphqlApiResult(null, BoxUnreachableException())
        }

        val r = mutateAsync(mutation, box, timeout)
        if (r.response != null) {
            return GraphqlApiResult(r.response, null)
        }

        val r2 = mutex.withLock { callBluetoothApiAsync(mutation, box) }
        if (r2 != null) {
            LogCat.d(r2.data)
            return GraphqlApiResult(r2, null)
        }

        return r
    }

    suspend fun <D : Query.Data> mixQueryAsync(
        query: Query<D>,
        box: DBox? = UIDataCache.current().box,
        timeout: Int = HttpApiTimeout.DEFAULT_SECONDS,
    ): GraphqlApiResult<D> {
        if (box == null) {
            return GraphqlApiResult(null, BoxUnreachableException())
        }

        val r = queryAsync(query, box, timeout)
        if (r.response != null && !r.response.isFromCache) {
            return GraphqlApiResult(r.response, null)
        }

        val r2 = mutex.withLock { callBluetoothApiAsync(query, box) }
        if (r2 != null) {
            LogCat.d(r2.data)
            if (UIDataCache.current().boxBluetoothReachable != true) {
                UIDataCache.current().boxBluetoothReachable = true
                sendEvent(BoxConnectivityStateChangedEvent())
            }
            return GraphqlApiResult(r2, null)
        }

        if (UIDataCache.current().boxBluetoothReachable != false) {
            UIDataCache.current().boxBluetoothReachable = false
            sendEvent(BoxConnectivityStateChangedEvent())
        }

        return r
    }

    private suspend fun <D : Query.Data> queryAsync(
        query: Query<D>,
        box: DBox,
        timeout: Int,
    ): GraphqlApiResult<D> {
        val boxIP = box.getBoxIP()
        if (boxIP.isEmpty()) {
            UIDataCache.current().boxNetworkReachable = false
            return GraphqlApiResult(null, BoxUnreachableException())
        }

        val response =
            try {
                getOrCreateClient(box.id, boxIP, box.token, timeout).query(query).fetchPolicy(FetchPolicy.NetworkFirst).execute()
            } catch (e: ApolloException) {
                e.printStackTrace()
                return GraphqlApiResult(null, e)
            }

        if (response.isFromCache) {
            if (UIDataCache.current().boxNetworkReachable != false) {
                UIDataCache.current().boxNetworkReachable = false
                sendEvent(BoxConnectivityStateChangedEvent())
            }
        } else {
            if (UIDataCache.current().boxNetworkReachable != true) {
                UIDataCache.current().boxNetworkReachable = true
                sendEvent(BoxConnectivityStateChangedEvent())
            }
        }

        return GraphqlApiResult(response, null)
    }

    fun disposeApolloClients(boxId: String) {
        clients.toMap().forEach {
            if (it.key.startsWith(boxId)) {
                it.value.dispose()
                clients.remove(it.key)
            }
        }
    }

    private suspend fun <D : Operation.Data> callBluetoothApiAsync(
        operation: Operation<D>,
        box: DBox,
    ): ApolloResponse<D>? {
        if (!BluetoothUtil.ensurePermissionAsync()) {
            return null
        }

        val device = BluetoothUtil.getCurrentBTDeviceAsync(box.bluetoothMac)
        if (device != null) {
            val smartDevice = SmartBTDevice(device)
            smartDevice.ensureConnectedAsync()
            if (smartDevice.isConnected()) {
                val r = smartDevice.graphqlAsync(MainApp.instance, operation, box.token)
                smartDevice.disconnect()
                return r
            }
        }

        return null
    }

    fun getBoxFileUrl(): String {
        val box = UIDataCache.current().box ?: return ""
        val boxIP = box.getBoxIP()
        if (boxIP.isEmpty()) {
            return ""
        }
        return "https://$boxIP:8443/public"
    }

    private suspend fun <D : Mutation.Data> mutateAsync(
        mutation: Mutation<D>,
        box: DBox,
        timeout: Int,
    ): GraphqlApiResult<D> {
        val boxIP = box.getBoxIP()
        if (boxIP.isEmpty()) {
            return GraphqlApiResult(null, BoxUnreachableException())
        }
        val response =
            try {
                getOrCreateClient(box.id, boxIP, box.token, timeout).mutation(mutation).execute()
            } catch (e: ApolloException) {
                e.printStackTrace()
                return GraphqlApiResult(null, e)
            }

        return GraphqlApiResult(response, null)
    }

    private fun getOrCreateClient(
        boxId: String,
        ip: String,
        token: String,
        timeout: Int,
    ): ApolloClient {
        val key = "$boxId:$ip:$timeout"
        var apolloClient = clients[key]
        if (apolloClient != null) {
            return apolloClient
        }

        apolloClient =
            ApolloClient.Builder()
                .serverUrl("https://$ip:8443/graphql")
                .okHttpClient(HttpClientManager.createCryptoHttpClient(token, timeout))
                .normalizedCache(SqlNormalizedCacheFactory(MainApp.instance, "apollo_$boxId.db"))
                .addCustomScalarAdapter(com.ismartcoding.plain.type.Time.type, com.apollographql.apollo3.adapter.DateAdapter)
                .build()
        clients[key] = apolloClient
        return apolloClient
    }
}
