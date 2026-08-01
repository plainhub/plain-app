package com.ismartcoding.plain.platform

/**
 * Swift-implemented share controller. iOS share sheets
 * (`UIActivityViewController`) must be presented from a `UIViewController`
 * and use ObjC APIs, so the real UI lives in Swift and is bridged through
 * this interface.
 *
 * Kotlin calls [shareText] / [shareFile] / [shareFiles] when commonMain
 * invokes the `shareText` / `shareFile` / `shareFiles` expect fun. Swift
 * presents the share sheet and dismisses it on completion.
 */
interface IosShareController {
    fun shareText(text: String)
    fun shareFile(path: String, mimeType: String)
    fun shareFiles(paths: List<String>, mimeTypes: List<String>)
    fun openFileExternal(path: String)
}

/**
 * Whether [path] points to a remote URL (http/https). Used to decide whether
 * the share controller downloads the content first or hands the local file
 * directly to `UIActivityViewController`.
 */
fun isRemoteUrl(path: String): Boolean =
    path.startsWith("http://", ignoreCase = true) ||
        path.startsWith("https://", ignoreCase = true)
