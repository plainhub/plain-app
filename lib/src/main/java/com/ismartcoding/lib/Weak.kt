package com.ismartcoding.lib

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

class Weak<T : Any>(initializer: () -> T?) {
    var weakReference = WeakReference<T?>(initializer())

    constructor() : this({ null })

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T? {
        return weakReference.get()
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?,
    ) {
        weakReference = WeakReference(value)
    }
}
