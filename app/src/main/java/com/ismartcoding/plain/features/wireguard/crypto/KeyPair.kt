package com.ismartcoding.plain.features.wireguard.crypto

class KeyPair(
    val privateKey: Key = Key.generatePrivateKey(),
) {
    val publicKey: Key = Key.generatePublicKey(privateKey)
}
