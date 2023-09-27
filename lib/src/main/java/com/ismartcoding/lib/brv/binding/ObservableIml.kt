package com.ismartcoding.lib.brv.binding

import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry

/**
 * A convenience class that implements [android.databinding.Observable] interface and provides
 * [.notifyPropertyChanged] and [.notifyChange] methods.
 */

interface ObservableIml : Observable {
    val registry: PropertyChangeRegistry

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        registry.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        registry.remove(callback)
    }

    /**
     * Notifies listeners that all properties of this instance have changed.
     */
    fun notifyChange() {
        registry.notifyCallbacks(this, 0, null)
    }

    /**
     * Notifies listeners that a specific property has changed. The getter for the property
     * that changes should be marked with [Bindable] to generate a field in
     * `BR` to be used as `fieldId`.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    fun notifyPropertyChanged(fieldId: Int) {
        registry.notifyCallbacks(this, fieldId, null)
    }
}
