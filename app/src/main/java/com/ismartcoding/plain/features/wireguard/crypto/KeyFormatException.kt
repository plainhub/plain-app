package com.ismartcoding.plain.features.wireguard.crypto

class KeyFormatException(val format: Key.Format, val type: Type) : Exception() {
    enum class Type {
        CONTENTS,
        LENGTH,
    }
}
