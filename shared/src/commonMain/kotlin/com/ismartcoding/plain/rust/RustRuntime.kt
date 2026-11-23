package com.ismartcoding.plain.rust

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.SubscriptionObservers

import com.ismartcoding.plain.platform.system.*
import com.ismartcoding.plain.rust.graphql.RustGraphqlSubscriptions
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.host.RustHostApiHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.Volatile

object RustRuntime {
    private val lock = PlatformLock()
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var isCoreInitialized: Boolean = false
        private set

    val dataDir: String by lazy { dataDirPath() }
    @Volatile private var logPath: String? = null
    val logFile: String get() = checkNotNull(logPath) { "Rust log path is not initialized" }
    @Volatile private var sharedPreviewPath: String? = null
    val sharedPreviewDir: String get() = checkNotNull(sharedPreviewPath) { "Rust shared preview cache is not initialized" }

    private val legacyUpgradePrompt = MutableStateFlow(false)
    internal val showLegacyUpgradePrompt = legacyUpgradePrompt.asStateFlow()

    internal fun dismissLegacyUpgradePrompt() {
        legacyUpgradePrompt.value = false
    }

    private var sessionToken: String? = null
    private var collectorsStarted = false

    fun start() = lock.withLock {
        val token = sessionToken ?: generateChaCha20Key().also { sessionToken = it }
        val config = rustStartupConfig
        val result = JsonHelper.jsonDecode<RustStartupResult>(RustNativeBindings.startCore(
            dataDir, token, JsonHelper.jsonEncode(config)))
        check(result.errorCode.isEmpty()) { result.error }
        require(result.logFile.startsWith('/')) { "Rust log file path required" }
        require(result.sharedPreviewDir.startsWith('/')) { "Rust shared preview cache path required" }
        logPath = result.logFile
        sharedPreviewPath = result.sharedPreviewDir
        if (!collectorsStarted) {
            Prefs.load()
            legacyUpgradePrompt.value = result.offerLegacyUpgrade
            isCoreInitialized = true
            RustHostApiHandler.start(scope)
            RustGraphqlSubscriptions.start(scope)
            SubscriptionObservers.start(scope)
            collectorsStarted = true
        }
    }
}
