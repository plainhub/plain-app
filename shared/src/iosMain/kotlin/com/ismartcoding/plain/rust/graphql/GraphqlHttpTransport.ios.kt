@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun createGraphqlHttpTransport(): GraphqlHttpTransport = object : GraphqlHttpTransport {
    override suspend fun post(url: String, clientId: String, body: ByteArray): GraphqlHttpResponse = suspendCancellableCoroutine { continuation ->
        val request = NSMutableURLRequest.requestWithURL(NSURL(string = url)).apply {
            HTTPMethod = "POST"
            HTTPBody = body.toNSData()
            setTimeoutInterval(30.0)
            setValue("application/octet-stream", forHTTPHeaderField = "Content-Type")
            setValue(clientId, forHTTPHeaderField = "c-id")
        }
        val chunks = mutableListOf<ByteArray>()
        var length = 0
        var status = 0
        val delegate = object : NSObject(), NSURLSessionDataDelegateProtocol {
            override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveResponse: NSURLResponse, completionHandler: (NSURLSessionResponseDisposition) -> Unit) {
                status = (didReceiveResponse as NSHTTPURLResponse).statusCode.toInt()
                completionHandler(NSURLSessionResponseAllow)
            }
            override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
                if (length.toLong() + didReceiveData.length.toLong() > 32L * 1024 * 1024) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("GraphQL response exceeds 32 MiB"))
                    session.invalidateAndCancel()
                } else {
                    val bytes = didReceiveData.toByteArray()
                    chunks.add(bytes)
                    length += bytes.size
                }
            }
            override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
                if (continuation.isActive) {
                    if (didCompleteWithError != null) continuation.resumeWithException(IllegalStateException(didCompleteWithError.localizedDescription))
                    else {
                        val bytes = ByteArray(length)
                        var offset = 0
                        chunks.forEach { it.copyInto(bytes, offset); offset += it.size }
                        continuation.resume(GraphqlHttpResponse(status, bytes))
                    }
                }
                session.finishTasksAndInvalidate()
            }
            override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didReceiveChallenge: NSURLAuthenticationChallenge, completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit) {
                val trust = didReceiveChallenge.protectionSpace.serverTrust
                if (didReceiveChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust && trust != null && didReceiveChallenge.protectionSpace.host == request.URL?.host) {
                    completionHandler(NSURLSessionAuthChallengeUseCredential, NSURLCredential.credentialForTrust(trust))
                } else completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            }
            override fun URLSession(session: NSURLSession, task: NSURLSessionTask, willPerformHTTPRedirection: NSHTTPURLResponse, newRequest: NSURLRequest, completionHandler: (NSURLRequest?) -> Unit) {
                completionHandler(null)
            }
        }
        val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration.apply {
            HTTPCookieStorage = null
            URLCache = null
        }
        val session = NSURLSession.sessionWithConfiguration(configuration, delegate, null)
        val task = session.dataTaskWithRequest(request)
        continuation.invokeOnCancellation { session.invalidateAndCancel() }
        task.resume()
    }
}
