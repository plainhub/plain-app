package com.ismartcoding.plain.crypto

data class ECDHKeyPair(
    val privateKeyEncoded: ByteArray,
    val publicKeyEncoded: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ECDHKeyPair) return false
        return privateKeyEncoded.contentEquals(other.privateKeyEncoded) &&
            publicKeyEncoded.contentEquals(other.publicKeyEncoded)
    }

    override fun hashCode(): Int = privateKeyEncoded.contentHashCode() * 31 + publicKeyEncoded.contentHashCode()
}
