package com.ismartcoding.lib.extensions

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<List<T>>.addAll(values: List<T>) {
    val value = this.value?.toMutableList() ?: arrayListOf()
    value.addAll(values)
    this.value = value
}

fun <T> MutableLiveData<List<T>>.add(value: T) {
    val values = this.value?.toMutableList() ?: arrayListOf()
    values.add(value)
    this.value = values
}
