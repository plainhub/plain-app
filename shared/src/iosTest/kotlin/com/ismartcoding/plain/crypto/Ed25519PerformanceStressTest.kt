package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.signEd25519
import com.ismartcoding.plain.platform.verifyEd25519
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * Performance & stress tests for the pure-Kotlin Ed25519 implementation.
 *
 * Two classes of regression are caught here:
 *
 *   1. **Memory leaks / stability** — if any internal array is aliased
 *      instead of copied, running 1000 iterations will eventually
 *      corrupt intermediate state in a way that makes sign → verify
 *      fail for a later iteration (e.g. when the GC happens to reuse
 *      a buffer, or when the same Fe array is double-freed in Native).
 *      Running sign→verify for 1000 iterations with the SAME key also
 *      exercises the SHA-512 hot path: if any state buffer is reused
 *      without resetting, iteration N will produce a wrong signature.
 *
 *   2. **Latency regression** — we print sign/verify per-op latency
 *      and also assert a generous soft ceiling so obvious performance
 *      bugs (e.g. accidentally quadratic feCarry) are caught at test
 *      time.  The limits are intentionally loose because the iOS
 *      simulator on CI may run 10× slower than native hardware.
 *
 * NOTE: pure timing assertions are flaky by nature — the test uses
 * `assertTrue` + generous thresholds so it won't break on slow CI
 * runners, but the printed values give humans a comparison baseline.
 */
@OptIn(ExperimentalTime::class)
class Ed25519PerformanceStressTest {

    @Test
    fun `1000 sign-verify iterations with same keypair - no state corruption`() {
        val (sk, pk) = generateEd25519KeyPair()
        val base = "stress iteration #".encodeToByteArray()
        for (i in 0 until 1000) {
            val msg = base + i.toString().encodeToByteArray()
            val sig = signEd25519(sk, msg)
            assertEquals(64, sig.size, "iter $i: signature must be 64 bytes")
            assertTrue(verifyEd25519(pk, msg, sig),
                "iter $i: sign→verify must succeed (state corruption suspected)")
        }
    }

    @Test
    fun `1000 sign-verify iterations generating fresh keypair each time`() {
        // Exercises the SecRandomCopyBytes → SHA-512 → scalarBase hot path.
        val msg = "fixed stress message".encodeToByteArray()
        for (i in 0 until 1000) {
            val (sk, pk) = generateEd25519KeyPair()
            val sig = signEd25519(sk, msg)
            assertTrue(verifyEd25519(pk, msg, sig),
                "iter $i with fresh keypair failed")
        }
    }

    @Test
    fun `1000 verify-only iterations - no sign - no pointDecode state leak`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = "verify stress".encodeToByteArray()
        val sig = signEd25519(sk, msg)
        for (i in 0 until 1000) {
            assertTrue(verifyEd25519(pk, msg, sig),
                "verify iteration $i failed — pointDecode state corruption?")
        }
    }

    @Test
    fun `sign latency over 100 iterations - sanity soft ceiling`() {
        val (sk, _) = generateEd25519KeyPair()
        val msg = ByteArray(256) { 0x42 }
        // Warm-up (JIT / interpreter stabilisation)
        repeat(5) { signEd25519(sk, msg) }

        val duration = measureTime {
            repeat(100) { signEd25519(sk, msg) }
        }
        val perOpUs = duration.inWholeMicroseconds / 100
        println("Ed25519 sign latency (Kotlin/Native iOS): $perOpUs µs/op (100 ops, ${duration.inWholeMilliseconds} ms total)")
        // Generous soft ceiling: 2M µs/op = 2 seconds per sign would be so
        // slow something is catastrophically wrong (e.g. infinite loop in
        // feInv).  In practice a correct port runs < 5 ms/op on simulator.
        assertTrue(perOpUs < 2_000_000,
            "sign latency $perOpUs µs/op > 2 s/op — catastrophic regression suspected")
    }

    @Test
    fun `verify latency over 100 iterations - sanity soft ceiling`() {
        val (sk, pk) = generateEd25519KeyPair()
        val msg = ByteArray(256) { 0x17 }
        val sig = signEd25519(sk, msg)
        repeat(5) { verifyEd25519(pk, msg, sig) }

        val duration = measureTime {
            repeat(100) { verifyEd25519(pk, msg, sig) }
        }
        val perOpUs = duration.inWholeMicroseconds / 100
        println("Ed25519 verify latency (Kotlin/Native iOS): $perOpUs µs/op (100 ops, ${duration.inWholeMilliseconds} ms total)")
        assertTrue(perOpUs < 2_000_000,
            "verify latency $perOpUs µs/op > 2 s/op — catastrophic regression suspected")
    }

    @Test
    fun `feMul stress 100_000 iterations - no carry state leaks`() {
        // Repeatedly multiply and verify the result is the same as a
        // freshly computed product.  Catches: carry chain corruption,
        // output aliasing input, and any long-to-wide overflow bug.
        val a = longArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val b = longArrayOf(17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
        val golden = LongArray(16); feMul(golden, a, b)
        for (i in 0 until 100_000) {
            val r = LongArray(16)
            feMul(r, a, b)
            assertTrue(feEqual(r, golden), "feMul mismatch at iteration $i")
        }
    }

    @Test
    fun `scalarBase for same scalar 1000 times - deterministic output`() {
        // If any internal buffer in scalarBase/scalarMult is aliased to the
        // output point, the next iteration will see a corrupted accumulator
        // and produce a different result even for the same scalar.
        val scalar = byteArrayOf(
            0x42, 0x11, 0x77, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x40, 0,
        )
        val out = Array<LongArray>(4) { LongArray(16) }
        scalarBase(out, scalar)
        val golden = pointEncode(out)
        for (i in 0 until 1000) {
            val p = Array<LongArray>(4) { LongArray(16) }
            scalarBase(p, scalar)
            val enc = pointEncode(p)
            assertTrue(golden.contentEquals(enc),
                "scalarBase non-deterministic at iter $i — aliasing bug?")
        }
    }
}
