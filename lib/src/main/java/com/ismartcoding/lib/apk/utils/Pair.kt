package com.ismartcoding.lib.apk.utils

class Pair<K, V> {
    var left: K? = null
        private set
    var right: V? = null
        private set

    constructor()
    constructor(left: K, right: V) {
        this.left = left
        this.right = right
    }

    fun setLeft(left: K) {
        this.left = left
    }

    fun setRight(right: V) {
        this.right = right
    }
}