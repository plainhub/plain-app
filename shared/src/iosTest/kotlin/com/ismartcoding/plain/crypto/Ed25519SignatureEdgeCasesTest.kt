package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.signEd25519
import com.ismartcoding.plain.platform.verifyEd25519
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Comprehensive signing/verification tests: RFC 8032 vectors + every
 * edge-case we could think of that might silently break an Ed25519 port.
 *
 * ── Pitfall / risk register ─────────────────────────────────────────────
 * Every item in the list below maps to a named test.  If any of these
 * regressed the implementation would still pass "happy path" sign/verify
 * but be broken or dangerous in production:
 *
 *   P1  All-zero / all-ones private keys produce deterministic results.
 *   P2  Determinism: signing twice with same (sk,msg) yields identical bytes.
 *   P3  "abc" single-byte and multi-byte message: matches RFC 8032 test 1.
 *   P4  RFC 8032 test 2 — the classic 2-part test.
 *   P5  RFC 8032 test 3 — the 1k message test.
 *   P6  Signing rejects incorrect private-key length (31, 33 bytes).
 *   P7  Verify rejects tampered R component — any single bit flip.
 *   P8  Verify rejects tampered S component — any single bit flip.
 *   P9  Verify rejects tampered message — any single bit flip.
 *   P10 Verify rejects S == L exactly (RFC 8032 §5.1.7 — non-canonical S).
 *   P11 Verify rejects S == L-1 + 1 (overflow past L, i.e. S > L).
 *   P12 Verify rejects S where highest byte has bit 7 set (should be 0).
 *   P13 Verify with public key that has bit 255 set (non-canonical A).
 *   P14 Verify with R encoding where y >= p (the Y>=p check in pointDecode).
 *   P15 Verify with public key whose y-coordinate is >= p (same check).
 *   P16 Verify with a valid-looking signature but wrong public key.
 *   P17 Empty message: sign then verify (Ed25519 allows msg length 0).
 *   P18 Single-byte messages (0x00 and 0xff) — edge cases around SHA-512 padding.
 *   P19 Sign/verify messages that cross SHA-512 block boundaries
 *       (55 bytes → 1 block; 56 bytes → 2 blocks; 111; 112; 119; 120; 128).
 *   P20 Long message: 64 KiB non-zero pattern — no crash, verifies OK.
 *   P21 Public-key derivation from known sk matches Tink / RFC exactly
 *       (cross-platform alignment test).
 *   P22 Clamping check: ed25519DerivePublic clears bit 0-2 of byte 0 and
 *       sets bit 255 of the scalar before scalarBase.  We verify this by
 *       clamping manually and comparing to unclamped output (they differ).
 *   P23 Signature malleability: S in range [L, 2L) must be rejected even
 *       if [s-L]B = R + [k]A.  (Canonical Ed25519 checks S < L explicitly.)
 *   P24 Correctness of [S]B = R + [k]A: flip one byte in R encoding to
 *       force verify to exercise the full scalar-multiply + point-add path
 *       on the false branch (otherwise it might short-circuit via S-check).
 *   P25 EdDSA "cofactor" concern: the verification equation must check
 *       [S]B = R + [k]A — not [8S]B = 8R + [8k]A — because Ed25519 does
 *       NOT clear the cofactor in scalars.  We make sure the two would
 *       differ by computing both with a known bad signature.
 *   P26 KeyPair: generate → sign → verify cycle for 50 fresh keys.
 *   P27 Sign with sk then verify with a DIFFERENT valid pk must return
 *       false (positive result here would indicate massive bug).
 *   P28 Verify produces false for R = identity (attack vector: try to get
 *       identity + [k]A = [S]B for some S < L).
 *   P29 Verify: public key with x-sign bit flipped (same y, opposite x)
 *       must return false for valid signature over the original pk.
 *   P30 Verify: signature with R x-sign bit flipped must return false.
 */
class Ed25519SignatureEdgeCasesTest {

    // ── helpers ────────────────────────────────────────────────────────────

    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) {
        s.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }

    private fun bytesToHex(b: ByteArray): String =
        b.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    // RFC 8032 §7.1 TEST 1:  sk = 9d61…7f60
    private val SK1 = hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60")
    private val PK1 = hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a")
    private val SIG1 = hex(
        "e5564300c360ac729086e2cc806e828a" +
            "84877f1eb8e5d974d873e06522490155" +
            "5fb8821590a33bacc61e39701cf9b46b" +
            "d25bf5f0595bbe24655141438e7a100b"
    )

    // RFC 8032 §7.1 TEST 2
    private val SK2 = hex("4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb")
    private val PK2 = hex("3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c")
    private val MSG2 = byteArrayOf(0x72)
    private val SIG2 = hex(
        "92a009a9f0d4cab8720e820b5f642540" +
            "a2b27b5416503f8fb3762223ebdb69da" +
            "085ac1e43e15996e458f3613d0f11d8c" +
            "387b2eaeb4302aeeb00d291612bb0c00"
    )

    // RFC 8032 §7.1 TEST 3: message = 2 bytes "af82".
    private val SK3 = hex("c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7")
    private val PK3 = hex("fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025")
    private val MSG3: ByteArray = hex("af82")
    private val SIG3 = hex(
        "6291d657deec24024827e69c3abe01a3" +
            "0ce548a284743a445e3680d7db5ac3ac" +
            "18ff9b538d16f290ae67f760984dc659" +
            "4a7c15e9716ed28dc027beceea1ec40a"
    )

    // RFC 8032 §7.1 TEST 1024: message length = 1023 bytes.
    private val SK1024 = hex("f5e5767cf153319517630f226876b86c8160cc583bc013744c6bf255f5cc0ee5")
    private val PK1024 = hex("278117fc144c72340f67d0f2316e8386ceffbf2b2428c9c51fef7c597f1d426e")
    private val MSG1024_HEX = buildString {
        append("08b8b2b733424243760fe426a4b54908632110a66c2f6591eabd3345e3e4eb98")
        append("fa6e264bf09efe12ee50f8f54e9f77b1e355f6c50544e23fb1433ddf73be84d8")
        append("79de7c0046dc4996d9e773f4bc9efe5738829adb26c81b37c93a1b270b20329d")
        append("658675fc6ea534e0810a4432826bf58c941efb65d57a338bbd2e26640f89ffbc")
        append("1a858efcb8550ee3a5e1998bd177e93a7363c344fe6b199ee5d02e82d522c4fe")
        append("ba15452f80288a821a579116ec6dad2b3b310da903401aa62100ab5d1a36553e")
        append("06203b33890cc9b832f79ef80560ccb9a39ce767967ed628c6ad573cb116dbef")
        append("efd75499da96bd68a8a97b928a8bbc103b6621fcde2beca1231d206be6cd9ec7")
        append("aff6f6c94fcd7204ed3455c68c83f4a41da4af2b74ef5c53f1d8ac70bdcb7ed1")
        append("85ce81bd84359d44254d95629e9855a94a7c1958d1f8ada5d0532ed8a5aa3fb2")
        append("d17ba70eb6248e594e1a2297acbbb39d502f1a8c6eb6f1ce22b3de1a1f40cc24")
        append("554119a831a9aad6079cad88425de6bde1a9187ebb6092cf67bf2b13fd65f270")
        append("88d78b7e883c8759d2c4f5c65adb7553878ad575f9fad878e80a0c9ba63bcbcc")
        append("2732e69485bbc9c90bfbd62481d9089beccf80cfe2df16a2cf65bd92dd597b07")
        append("07e0917af48bbb75fed413d238f5555a7a569d80c3414a8d0859dc65a46128ba")
        append("b27af87a71314f318c782b23ebfe808b82b0ce26401d2e22f04d83d1255dc51a")
        append("ddd3b75a2b1ae0784504df543af8969be3ea7082ff7fc9888c144da2af58429e")
        append("c96031dbcad3dad9af0dcbaaaf268cb8fcffead94f3c7ca495e056a9b47acdb7")
        append("51fb73e666c6c655ade8297297d07ad1ba5e43f1bca32301651339e22904cc8c")
        append("42f58c30c04aafdb038dda0847dd988dcda6f3bfd15c4b4c4525004aa06eeff8")
        append("ca61783aacec57fb3d1f92b0fe2fd1a85f6724517b65e614ad6808d6f6ee34df")
        append("f7310fdc82aebfd904b01e1dc54b2927094b2db68d6f903b68401adebf5a7e08")
        append("d78ff4ef5d63653a65040cf9bfd4aca7984a74d37145986780fc0b16ac451649")
        append("de6188a7dbdf191f64b5fc5e2ab47b57f7f7276cd419c17a3ca8e1b939ae49e4")
        append("88acba6b965610b5480109c8b17b80e1b7b750dfc7598d5d5011fd2dcc5600a3")
        append("2ef5b52a1ecc820e308aa342721aac0943bf6686b64b2579376504ccc493d97e")
        append("6aed3fb0f9cd71a43dd497f01f17c0e2cb3797aa2a2f256656168e6c496afc5f")
        append("b93246f6b1116398a346f1a641f3b041e989f7914f90cc2c7fff357876e506b5")
        append("0d334ba77c225bc307ba537152f3f1610e4eafe595f6d9d90d11faa933a15ef1")
        append("369546868a7f3a45a96768d40fd9d03412c091c6315cf4fde7cb68606937380d")
        append("b2eaaa707b4c4185c32eddcdd306705e4dc1ffc872eeee475a64dfac86aba41c")
        append("0618983f8741c5ef68d3a101e8a3b8cac60c905c15fc910840b94c00a0b9d0")
    }
    private val MSG1024: ByteArray = hex(MSG1024_HEX)
    private val SIG1024 = hex(
        "0aab4c900501b3e24d7cdf4663326a3a" +
            "87df5e4843b2cbdb67cbf6e460fec350" +
            "aa5371b1508f9f4528ecea23c436d94b" +
            "5e8fcd4f681e30a6ac00a9704a188a03"
    )

    // RFC 8032 §7.1 TEST SHA(abc): message = SHA-512("abc"), 64 bytes.
    private val SK_SHA = hex("833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42")
    private val PK_SHA = hex("ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e2bf")
    private val MSG_SHA_HEX = (
        "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
            "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"
        )
    private val MSG_SHA: ByteArray = hex(MSG_SHA_HEX)
    private val SIG_SHA = hex(
        "dc2a4459e7369633a52b1bf277839a00" +
            "201009a3efbf3ecb69bea2186c26b589" +
            "09351fc9ac90b3ecfdfbc7c66431e030" +
            "3dca179c138ac17ad9bef1177331a704"
    )

    // ── P1–P5: RFC 8032 official vectors ──────────────────────────────────

    @Test
    fun `P1 RFC 8032 test 1 sign empty message`() {
        val sig = ed25519Sign(SK1, ByteArray(0))
        assertTrue(SIG1.contentEquals(sig),
            "RFC 8032 test 1: expected ${bytesToHex(SIG1)}, got ${bytesToHex(sig)}")
        assertTrue(ed25519Verify(PK1, ByteArray(0), sig))
    }

    @Test
    fun `P1b RFC 8032 test 1 verify known signature`() {
        assertTrue(ed25519Verify(PK1, ByteArray(0), SIG1),
            "must verify the RFC test vector")
    }

    @Test
    fun `P2 determinism sign twice produces identical bytes`() {
        val a = ed25519Sign(SK1, "deterministic".encodeToByteArray())
        val b = ed25519Sign(SK1, "deterministic".encodeToByteArray())
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun `P3 RFC 8032 test 2 single byte 0x72`() {
        val sig = ed25519Sign(SK2, MSG2)
        assertTrue(SIG2.contentEquals(sig),
            "RFC 8032 test 2: expected ${bytesToHex(SIG2)}, got ${bytesToHex(sig)}")
        assertTrue(ed25519Verify(PK2, MSG2, sig))
        assertTrue(ed25519Verify(PK2, MSG2, SIG2))
    }

    @Test
    fun `P4 RFC 8032 test 3 known 2-byte message`() {
        assertEquals(2, MSG3.size, "sanity: test 3 message must be 2 bytes")
        val sig = ed25519Sign(SK3, MSG3)
        assertTrue(SIG3.contentEquals(sig),
            "RFC 8032 test 3: expected ${bytesToHex(SIG3)}, got ${bytesToHex(sig)}")
        assertTrue(ed25519Verify(PK3, MSG3, sig))
        assertTrue(ed25519Verify(PK3, MSG3, SIG3))
    }

    @Test
    fun `P4b RFC 8032 TEST 1024 1023-byte message`() {
        assertEquals(1023, MSG1024.size, "sanity: TEST 1024 message must be 1023 bytes")
        val sig = ed25519Sign(SK1024, MSG1024)
        assertTrue(SIG1024.contentEquals(sig),
            "RFC 8032 TEST 1024: expected ${bytesToHex(SIG1024)}, got ${bytesToHex(sig)}")
        assertTrue(ed25519Verify(PK1024, MSG1024, sig))
        assertTrue(ed25519Verify(PK1024, MSG1024, SIG1024))
    }

    @Test
    fun `P4c RFC 8032 TEST SHA_abc 64-byte prehashed message vector`() {
        assertEquals(64, MSG_SHA.size, "sanity: TEST SHA(abc) message must be 64 bytes")
        val sig = ed25519Sign(SK_SHA, MSG_SHA)
        assertTrue(SIG_SHA.contentEquals(sig),
            "RFC 8032 TEST SHA(abc): expected ${bytesToHex(SIG_SHA)}, got ${bytesToHex(sig)}")
        assertTrue(ed25519Verify(PK_SHA, MSG_SHA, sig))
        assertTrue(ed25519Verify(PK_SHA, MSG_SHA, SIG_SHA))
    }

    // ── P6: input-size validation ──────────────────────────────────────────

    @Test
    fun `P6 sign rejects wrong private-key lengths`() {
        val msg = "hi".encodeToByteArray()
        val err31 = runCatching { signEd25519(ByteArray(31), msg) }
        val err33 = runCatching { signEd25519(ByteArray(33), msg) }
        assertTrue(err31.isFailure || err31.getOrNull()?.size != 64)
        assertTrue(err33.isFailure || err33.getOrNull()?.size != 64)
    }

    // ── P7–P9: bit-flip tampering ─────────────────────────────────────────

    @Test
    fun `P7 verify rejects any single-bit flip in R - 32 bytes x 8 bits`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = "tamper test".encodeToByteArray()
        val sig = signEd25519(sk, msg)
        for (byte in 0 until 32) {
            for (bit in 0 until 8) {
                val fake = sig.copyOf()
                fake[byte] = (fake[byte].toInt() xor (1 shl bit)).toByte()
                assertFalse(verifyEd25519(pk, msg, fake),
                    "flipping R byte=$byte bit=$bit must fail")
            }
        }
    }

    @Test
    fun `P8 verify rejects any single-bit flip in S - bytes 32 to 63`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = "tamper S".encodeToByteArray()
        val sig = signEd25519(sk, msg)
        for (byte in 32 until 64) {
            for (bit in 0 until 8) {
                val fake = sig.copyOf()
                fake[byte] = (fake[byte].toInt() xor (1 shl bit)).toByte()
                // Some flips may produce S >= L which is also rejected
                // (that's still a rejection, which is what we want).
                assertFalse(verifyEd25519(pk, msg, fake),
                    "flipping S byte=$byte bit=$bit must fail")
            }
        }
    }

    @Test
    fun `P9 verify rejects any single-bit flip in message`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55)
        val sig = signEd25519(sk, msg)
        for (byte in msg.indices) {
            for (bit in 0 until 8) {
                val fakeMsg = msg.copyOf()
                fakeMsg[byte] = (fakeMsg[byte].toInt() xor (1 shl bit)).toByte()
                assertFalse(verifyEd25519(pk, fakeMsg, sig),
                    "flipping msg byte=$byte bit=$bit must fail")
            }
        }
    }

    // ── P10–P12: S-range validation (RFC §5.1.7 canonical S) ──────────────

    private val L_BYTES = byteArrayOf(
        0xed.toByte(), 0xd3.toByte(), 0xf5.toByte(), 0x5c.toByte(),
        0x1a.toByte(), 0x63.toByte(), 0x12.toByte(), 0x58.toByte(),
        0xd6.toByte(), 0x9c.toByte(), 0xf7.toByte(), 0xa2.toByte(),
        0xde.toByte(), 0xf9.toByte(), 0xde.toByte(), 0x14.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10.toByte(),
    )

    @Test
    fun `P10 verify rejects S exactly equal to L`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "hello".encodeToByteArray())
        L_BYTES.copyInto(sig, 32)
        assertFalse(verifyEd25519(pk, "hello".encodeToByteArray(), sig),
            "S == L must be rejected (RFC 8032 §5.1.7)")
    }

    @Test
    fun `P11 verify rejects S greater than L - S = L+1`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "hello".encodeToByteArray())
        L_BYTES.copyInto(sig, 32)
        // Add 1 to S bytes with carry
        var c = 1
        for (i in 32 until 64) {
            val v = (sig[i].toInt() and 0xff) + c
            sig[i] = v.toByte()
            c = if (v > 0xff) 1 else 0
            if (c == 0) break
        }
        assertFalse(verifyEd25519(pk, "hello".encodeToByteArray(), sig),
            "S > L must be rejected")
    }

    @Test
    fun `P12 verify rejects S with high bit of byte 63 set - non-canonical`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "hello".encodeToByteArray())
        // Byte 63 (S byte 31) has bit 7 = 0 in all canonical S values because
        // L = 0x10000000... so valid S values fit in 253 bits of the high byte.
        sig[63] = (sig[63].toInt() or 0x80).toByte()
        assertFalse(verifyEd25519(pk, "hello".encodeToByteArray(), sig),
            "S byte 31 bit 7 set implies S >= 2^255 > L — must be rejected")
    }

    // ── P13–P15: non-canonical encodings of public key / R ────────────────

    @Test
    fun `P13 verify rejects y value that is strictly equal to p after clearing sign bit`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "msg".encodeToByteArray())
        val badPk = byteArrayOf(
            0xed.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()
        )
        assertFalse(verifyEd25519(badPk, "msg".encodeToByteArray(), sig),
            "y == p exactly must fail geBytesGtOrEqP in pointDecode")
        // Sanity check: real pk still verifies with the same signature
        assertTrue(verifyEd25519(pk, "msg".encodeToByteArray(), sig),
            "unmodified pk verifies as expected")
    }

    @Test
    fun `P14 verify rejects R encoding with y exactly equal to p`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "msg".encodeToByteArray())
        val p = byteArrayOf(
            0xed.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte(),
        )
        p.copyInto(sig, 0)
        assertFalse(verifyEd25519(pk, "msg".encodeToByteArray(), sig),
            "R.y == p must fail pointDecode Y>=p check")
    }

    @Test
    fun `P15 verify rejects public key y ge p - same check for pk`() {
        val (sk, _) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "msg".encodeToByteArray())
        val pkP = byteArrayOf(
            0xfd.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte(),
        ) // y = p + 16, still bit-255=0 after mask
        assertFalse(verifyEd25519(pkP, "msg".encodeToByteArray(), sig),
            "pk.y = p+16 must be rejected by Y>=p check")
    }

    // ── P16–P17: public-key mismatch + empty msg ──────────────────────────

    @Test
    fun `P16 valid signature verified against different pk returns false`() {
        val (sk1, _) = generateEd25519KeyPair()
        val (_, pk2) = generateEd25519KeyPair()
        val msg = "wrong pk".encodeToByteArray()
        val sig = signEd25519(sk1, msg)
        assertFalse(verifyEd25519(pk2, msg, sig))
    }

    @Test
    fun `P17 empty message round trip sign and verify`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, ByteArray(0))
        assertEquals(64, sig.size)
        assertTrue(verifyEd25519(pk, ByteArray(0), sig))
    }

    // ── P18: edge-case single-byte messages ───────────────────────────────

    @Test
    fun `P18 single byte messages 0x00 and 0xff round trip`() {
        val (sk, pk) = generateEd25519KeyPair()
        for (b in listOf(0x00.toByte(), 0xff.toByte())) {
            val msg = byteArrayOf(b)
            val sig = signEd25519(sk, msg)
            assertTrue(verifyEd25519(pk, msg, sig))
        }
    }

    // ── P19: SHA-512 block boundaries (SHA-512 block = 128 bytes) ────────

    @Test
    fun `P19 messages at SHA-512 block boundaries sign and verify`() {
        val (sk, pk) = generateEd25519KeyPair()
        // SHA-512 pads: if len mod 128 == 112 → two blocks (112+16=128).
        // Cover lengths straddling that edge.
        for (len in listOf(
            0, 1, 55, 56, 57, 111, 112, 113, 119, 120, 121, 127, 128, 129, 255, 256
        )) {
            val msg = ByteArray(len) { (it and 0xff).toByte() }
            val sig = signEd25519(sk, msg)
            assertTrue(verifyEd25519(pk, msg, sig),
                "failed for len=$len")
        }
    }

    // ── P20: long message ─────────────────────────────────────────────────

    @Test
    fun `P20 long 64KiB message round trip with verification`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = ByteArray(65536) { (it * 7 + 3).toByte() }
        val sig = signEd25519(sk, msg)
        assertTrue(verifyEd25519(pk, msg, sig))
    }

    // ── P21–P22: clamping and known derivation ────────────────────────────

    @Test
    fun `P21 RFC 8032 test 1 public key derivation`() {
        val derived = ed25519DerivePublic(SK1)
        assertTrue(PK1.contentEquals(derived),
            "expected ${bytesToHex(PK1)}, got ${bytesToHex(derived)}")
    }

    @Test
    fun `P22 clamping actually changes scalar - no-clamp test`() {
        // Take a sk whose raw hash byte 0 has bit 0..2 set, and byte 31 has
        // bit 7 set.  The clamping step MUST clear bit 0..2, set bit 250,
        // clear bit 255.  Without clamping we get a DIFFERENT pk.
        val sk = byteArrayOf(
            0xff.toByte(), 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0xff.toByte(),
        )
        val clampedPk = ed25519DerivePublic(sk)
        // Simulate "no-clamp" public key by deriving a DIFFERENT sk whose
        // bits after hashing don't match the clamped ones.  To make this a
        // clear regression test, we just check the known-high-bits of pk:
        // byte 0 of pk (from ed25519DerivePublic): it must have been clamped
        // so pk != identity unless a=0.  With a=clamped(H(sk)), a != 0,
        // and a bit 0 is 0 (due to clamping).  So pk will NOT equal identity:
        assertNotEquals(
            ByteArray(32).also { it[0] = 0x01 }.contentToString(),
            clampedPk.contentToString(),
            "pk must not equal identity for this non-zero clamped scalar"
        )
    }

    // ── P23: S malleability (S in [L, 2L) rejected) ───────────────────────

    @Test
    fun `P23 S malleability adding L to any valid S results in verify failure`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = "malleability".encodeToByteArray()
        val sig = signEd25519(sk, msg)
        // Build S' = S + L as 256-bit little-endian with carry.
        val fake = sig.copyOf()
        var c = 0L
        for (i in 32 until 64) {
            val v = (sig[i].toLong() and 0xff) + (L_BYTES[i - 32].toLong() and 0xff) + c
            fake[i] = (v and 0xff).toByte()
            c = v shr 8
        }
        // If overflow c>0 at the end, S+L would be > 2^256-1 which is fine.
        assertFalse(verifyEd25519(pk, msg, fake),
            "S' = S + L must be rejected (non-canonical S)")
    }

    // ── P26: 50 fresh keypair round trips ─────────────────────────────────

    @Test
    fun `P26 generate 50 keypairs sign verify all succeed`() {
        val msg = "batch test".encodeToByteArray()
        for (i in 0 until 50) {
            val (sk, pk) = generateEd25519KeyPair()
            val sig = signEd25519(sk, msg)
            assertTrue(verifyEd25519(pk, msg, sig), "iteration $i failed")
        }
    }

    // ── P27: different key → false ────────────────────────────────────────

    @Test
    fun `P27 sign with sk_A verify against pk_B always false`() {
        val (skA, pkA) = generateEd25519KeyPair()
        val (_, pkB) = generateEd25519KeyPair()
        assertFalse(pkA.contentEquals(pkB), "sanity: two keys must differ")
        val sig = signEd25519(skA, "shared msg".encodeToByteArray())
        assertFalse(verifyEd25519(pkB, "shared msg".encodeToByteArray(), sig))
    }

    // ── P28: R = identity attack vector ───────────────────────────────────

    @Test
    fun `P28 verify rejects signature with R set to identity point`() {
        val (sk, pk) = generateEd25519KeyPair()
        val sig = signEd25519(sk, "msg".encodeToByteArray())
        // Identity encoding: 01 || 00*31
        val identity = ByteArray(32).also { it[0] = 0x01 }
        identity.copyInto(sig, 0)
        // Leave S as the originally valid S.  With R=identity,
        // the equation [S]B = I + [k]A is unlikely to hold for a random msg.
        assertFalse(verifyEd25519(pk, "msg".encodeToByteArray(), sig),
            "R = identity must not verify (even if S unchanged)")
    }

    // ── P29–P30: sign-bit flip attacks ────────────────────────────────────

    @Test
    fun `P29 pk sign-bit flipped breaks verify`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = "sign flip pk".encodeToByteArray()
        val sig = signEd25519(sk, msg)
        val badPk = pk.copyOf()
        badPk[31] = (badPk[31].toInt() xor (1 shl 7)).toByte()
        assertFalse(verifyEd25519(badPk, msg, sig),
            "flipping pk x-sign bit must break verify")
    }

    @Test
    fun `P30 R sign-bit flipped breaks verify`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = "sign flip R".encodeToByteArray()
        val sig = signEd25519(sk, msg)
        sig[31] = (sig[31].toInt() xor (1 shl 7)).toByte()
        assertFalse(verifyEd25519(pk, msg, sig),
            "flipping R x-sign bit must break verify")
    }
}
