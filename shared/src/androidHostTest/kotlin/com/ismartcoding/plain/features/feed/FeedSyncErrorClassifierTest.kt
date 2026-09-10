package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.platform.NetworkType
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedSyncErrorClassifierTest {
    private fun classify(ex: Throwable, networkType: NetworkType = NetworkType.WIFI) =
        FeedSyncErrorClassifier.classify(ex, networkType)

    @Test
    fun offlineNetworkShortCircuits() {
        assertEquals(FeedSyncErrorCode.NO_NETWORK, classify(Exception("anything"), NetworkType.NONE))
    }

    @Test
    fun httpStatusMessageIsServer() {
        assertEquals(FeedSyncErrorCode.SERVER, classify(Exception("HTTP 404")))
        assertEquals(FeedSyncErrorCode.SERVER, classify(Exception("HTTP 503")))
    }

    @Test
    fun androidNetworkExceptions() {
        assertEquals(FeedSyncErrorCode.DNS, classify(UnknownHostException("api.example.com")))
        assertEquals(FeedSyncErrorCode.TIMEOUT, classify(SocketTimeoutException("Read timed out")))
        assertEquals(FeedSyncErrorCode.TIMEOUT, classify(ConnectException("refused")))
        assertEquals(FeedSyncErrorCode.SSL, classify(SSLHandshakeException("cert mismatch")))
    }

    @Test
    fun causeChainIsWalked() {
        val wrapped = RuntimeException(IllegalStateException(UnknownHostException("host")))
        assertEquals(FeedSyncErrorCode.DNS, classify(wrapped))
    }

    @Test
    fun iosNsUrlErrorMessages() {
        assertEquals(FeedSyncErrorCode.NO_NETWORK, classify(Exception("NSURLErrorDomain -1009 offline")))
        assertEquals(FeedSyncErrorCode.DNS, classify(Exception("NSURLErrorDomain -1003 A server with the specified hostname could not be found")))
        assertEquals(FeedSyncErrorCode.TIMEOUT, classify(Exception("NSURLErrorDomain -1001 timed out")))
        assertEquals(FeedSyncErrorCode.TIMEOUT, classify(Exception("NSURLErrorDomain -1004 cannot connect")))
        assertEquals(FeedSyncErrorCode.SSL, classify(Exception("NSURLErrorDomain -1200 An SSL error has occurred")))
        assertEquals(FeedSyncErrorCode.UNKNOWN, classify(Exception("NSURLErrorDomain -999 cancelled")))
    }

    @Test
    fun parseFailures() {
        assertEquals(FeedSyncErrorCode.PARSE, classify(IllegalArgumentException("The provided XML is not supported")))
    }

    @Test
    fun fallbackAndTimeoutByMessage() {
        assertEquals(FeedSyncErrorCode.UNKNOWN, classify(RuntimeException("boom")))
        assertEquals(FeedSyncErrorCode.TIMEOUT, classify(Exception("operation timed out")))
    }
}
