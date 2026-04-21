package com.ismartcoding.plain.debug

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import leakcanary.LeakCanary
import shark.AndroidReferenceMatchers
import shark.LibraryLeakReferenceMatcher
import shark.ReferencePattern

/**
 * Configures LeakCanary's known library leaks before [com.ismartcoding.plain.MainApp.onCreate]
 * runs. ContentProviders are started by the framework before the Application, so this is safe.
 *
 * Why this exists
 * ---------------
 * [android.service.notification.NotificationListenerService] creates an inner Binder stub called
 * `NotificationListenerWrapper` that holds an implicit `this$0` reference back to the outer
 * service instance. The Android system keeps that Binder stub alive as a global JNI reference
 * even after `Service#onDestroy()`, so the service cannot be GC-ed until the OS eventually drops
 * the Binder connection. There is no application-level fix for this; it is an Android framework
 * design limitation.
 *
 * This provider adds the reference to LeakCanary's "library leak" list so it is correctly
 * categorised and does not show up as an application regression in heap reports.
 */
internal class LeakCanaryKnownLeaks : ContentProvider() {

    override fun onCreate(): Boolean {
        LeakCanary.config = LeakCanary.config.copy(
            referenceMatchers = AndroidReferenceMatchers.appDefaults + knownLeaks
        )
        return true
    }

    // ---- unused ContentProvider stubs ----

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    companion object {
        /**
         * `NotificationListenerService$NotificationListenerWrapper` is a non-static inner class.
         * Its implicit `this$0` field holds a strong reference to the outer service. The Android
         * Binder system keeps a global native reference to the wrapper after `onDestroy()`,
         * preventing the service from being garbage-collected until the system releases the
         * Binder connection. This is an Android OS limitation, not an application bug.
         *
         * See: https://cs.android.com/android/platform/superproject/+/main:frameworks/base/core/java/android/service/notification/NotificationListenerService.java
         */
        private val knownLeaks = listOf(
            LibraryLeakReferenceMatcher(
                pattern = ReferencePattern.InstanceFieldPattern(
                    className = "android.service.notification.NotificationListenerService\$NotificationListenerWrapper",
                    fieldName = "this\$0"
                ),
                description = "NotificationListenerWrapper is a non-static inner class of " +
                    "NotificationListenerService. The Android Binder system keeps the wrapper " +
                    "alive as a global JNI reference after onDestroy(), preventing GC of the " +
                    "outer service. This is an Android framework limitation."
            )
        )
    }
}
