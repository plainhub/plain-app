@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, kotlin.uuid.ExperimentalUuidApi::class)

package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.platform.system.cacheDirPath
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resumeWithException
import kotlin.uuid.Uuid

internal actual suspend fun downloadHttpFile(
    url: String,
    name: String,
    tlsCertificateSha256: String?,
    onProgress: (Long, Long?) -> Unit,
): String = suspendCancellableCoroutine { continuation ->
    val request = NSMutableURLRequest.requestWithURL(checkNotNull(NSURL.URLWithString(url)))
    if (tlsCertificateSha256 != null) require(request.URL?.scheme == "https") { "Peer resources require HTTPS" }
    val manager = NSFileManager.defaultManager
    val directory = cacheDirPath() + "/resource_downloads"
    val extension = name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 16 && it.all(Char::isLetterOrDigit) }
    val destination = directory + "/resource-" + Uuid.random().toString() + (extension?.let { ".$it" } ?: ".tmp")
    val delegate = object : NSObject(), NSURLSessionDownloadDelegateProtocol {
        override fun URLSession(session: NSURLSession, downloadTask: NSURLSessionDownloadTask, didWriteData: Long, totalBytesWritten: Long, totalBytesExpectedToWrite: Long) {
            onProgress(totalBytesWritten, totalBytesExpectedToWrite.takeIf { it >= 0 })
        }
        override fun URLSession(session: NSURLSession, downloadTask: NSURLSessionDownloadTask, didFinishDownloadingToURL: NSURL) {
            try {
                val response = downloadTask.response as NSHTTPURLResponse
                check(response.statusCode in 200..299) { "Download HTTP ${response.statusCode}" }
                check(manager.createDirectoryAtPath(directory, true, null, null)) { "Unable to create resource download directory" }
                check(manager.moveItemAtURL(didFinishDownloadingToURL, checkNotNull(NSURL.fileURLWithPath(destination)), null)) { "Unable to finish resource download" }
                if (continuation.isActive) continuation.resume(destination, onCancellation = { _, path, _ -> manager.removeItemAtPath(path, null) })
                else manager.removeItemAtPath(destination, null)
            } catch (error: Throwable) {
                manager.removeItemAtPath(destination, null)
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
            if (didCompleteWithError != null && continuation.isActive) {
                manager.removeItemAtPath(destination, null)
                continuation.resumeWithException(IllegalStateException(didCompleteWithError.localizedDescription))
            }
            session.finishTasksAndInvalidate()
        }
        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didReceiveChallenge: NSURLAuthenticationChallenge, completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit) {
            if (tlsCertificateSha256 == null) {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
                return
            }
            val credential = com.ismartcoding.plain.rust.graphql.peerResourceCredential(didReceiveChallenge, request.URL?.host, tlsCertificateSha256)
            if (credential != null) completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
            else completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        }
        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, willPerformHTTPRedirection: NSHTTPURLResponse, newRequest: NSURLRequest, completionHandler: (NSURLRequest?) -> Unit) {
            completionHandler(if (tlsCertificateSha256 == null) newRequest else null)
        }
    }
    val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration.apply { HTTPCookieStorage = null; URLCache = null }
    val session = NSURLSession.sessionWithConfiguration(configuration, delegate, null)
    val task = session.downloadTaskWithRequest(request)
    continuation.invokeOnCancellation { session.invalidateAndCancel() }
    onProgress(0, null)
    task.resume()
}
