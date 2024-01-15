package com.ismartcoding.lib.apk.struct.signingv2

/**
 * For read apk signing block
 *
 * @see [apksigning v2 scheme](https://source.android.com/security/apksigning/v2)
 */
class ApkSigningBlock(val signerBlocks: List<SignerBlock>) {

    companion object {
        const val SIGNING_V2_ID = 0x7109871a
        const val MAGIC = "APK Sig Block 42"
    }
}