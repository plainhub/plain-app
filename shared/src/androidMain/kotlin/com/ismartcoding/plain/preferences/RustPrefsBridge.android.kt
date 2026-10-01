package com.ismartcoding.plain.preferences

internal actual object RustPrefsBridge {
    init {
        System.loadLibrary("plain_rust")
    }

    private external fun openNative(systemPath: String, userPath: String)
    private external fun systemSnapshotNative(): String
    private external fun userSnapshotNative(): String
    private external fun setSystemNative(key: String, valueJson: String)
    private external fun setUserNative(key: String, valueJson: String)
    private external fun removeSystemNative(key: String)
    private external fun removeUserNative(key: String)

    actual fun open(systemPath: String, userPath: String) = openNative(systemPath, userPath)
    actual fun systemSnapshot(): String = systemSnapshotNative()
    actual fun userSnapshot(): String = userSnapshotNative()
    actual fun setSystem(key: String, valueJson: String) = setSystemNative(key, valueJson)
    actual fun setUser(key: String, valueJson: String) = setUserNative(key, valueJson)
    actual fun removeSystem(key: String) = removeSystemNative(key)
    actual fun removeUser(key: String) = removeUserNative(key)
}
