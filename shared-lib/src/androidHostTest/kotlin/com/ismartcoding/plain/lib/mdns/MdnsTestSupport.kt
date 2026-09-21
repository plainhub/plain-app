package com.ismartcoding.plain.lib.mdns

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before

/** One datagram queued for delivery into a [FakeMdnsSocket]. */
internal class FakeDatagram(val bytes: ByteArray, val senderIp: String, val senderPort: Int)

/** One datagram the responder sent through a [FakeMdnsSocket]. */
internal class SentDatagram(val bytes: ByteArray, val destIp: String, val destPort: Int)

/**
 * In-memory [MdnsSocket]. Tests push inbound datagrams into [inbox] (the
 * responder's receive loop drains it) and inspect [sent] for transmissions.
 */
internal class FakeMdnsSocket : MdnsSocket {
    val binds = mutableListOf<Int>()
    val joins = mutableListOf<Pair<String, String?>>()
    val outgoingIfaces = mutableListOf<String>()
    val sent = ConcurrentLinkedQueue<SentDatagram>()
    val inbox = LinkedBlockingQueue<FakeDatagram>()

    /** When set, [bind] throws — simulates a port conflict. */
    var bindFailure: Exception? = null

    /** When set, the next [receive] throws it — simulates an OOM Error killing the receive thread. */
    @Volatile var failReceive: Throwable? = null

    @Volatile private var closed = false
    override val isClosed: Boolean get() = closed

    override fun bind(port: Int, timeoutMs: Int) {
        bindFailure?.let { throw it }
        binds += port
    }

    override fun joinGroup(groupIp: String, ifaceName: String?) {
        joins += groupIp to ifaceName
    }

    override fun setOutgoingInterface(ifaceName: String) {
        outgoingIfaces += ifaceName
    }

    override fun receive(buf: ByteArray): ReceiveResult? {
        failReceive?.let { throw it }
        val d = inbox.poll(50, TimeUnit.MILLISECONDS) ?: return null
        System.arraycopy(d.bytes, 0, buf, 0, d.bytes.size)
        return ReceiveResult(d.bytes.size, d.senderIp, d.senderPort)
    }

    override fun send(bytes: ByteArray, destIp: String, destPort: Int) {
        sent += SentDatagram(bytes.copyOf(), destIp, destPort)
    }

    override fun close() {
        closed = true
    }

    fun deliver(bytes: ByteArray, senderIp: String, senderPort: Int = 5353) {
        inbox += FakeDatagram(bytes.copyOf(), senderIp, senderPort)
    }
}

/** Worker handle whose liveness tests control — simulates a dead receive thread. */
internal class StaticWorkerHandle(@Volatile override var isAlive: Boolean) : MdnsWorkerHandle {
    override fun join(timeoutMs: Long) {}
}

/** Polls [condition] until true or the deadline passes; returns the last result. */
internal fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return true
        Thread.sleep(10)
    }
    return condition()
}

/** Builds a one-section mDNS response packet from answer records, all in the answer section. */
internal fun mdnsResponseOf(build: TestRecordWriter.() -> Unit): ByteArray {
    val writer = TestRecordWriter().apply(build)
    val out = mutableListOf<Byte>()
    MdnsPacketCodec.writeHeader(out, answers = writer.count, additional = 0)
    out.addAll(writer.bytes.toList())
    return out.toByteArray()
}

/** Accumulates wire-format answer records for [mdnsResponseOf]. */
internal class TestRecordWriter {
    val bytes = mutableListOf<Byte>()
    var count = 0
        private set

    fun ptr(name: String, target: String, ttl: Int = MdnsPacketCodec.TTL_SECONDS) {
        MdnsPacketCodec.writeRecord(
            bytes, MdnsPacketCodec.encodeName(name), MdnsPacketCodec.TYPE_PTR,
            MdnsPacketCodec.DNS_CLASS_IN, ttl, MdnsPacketCodec.encodeName(target),
        )
        count++
    }

    fun srv(instanceFqdn: String, port: Int, target: String, ttl: Int = MdnsPacketCodec.TTL_SECONDS) {
        val rdata = mutableListOf<Byte>()
        MdnsPacketCodec.writeU16(rdata, 0)
        MdnsPacketCodec.writeU16(rdata, 0)
        MdnsPacketCodec.writeU16(rdata, port)
        rdata.addAll(MdnsPacketCodec.encodeName(target).toList())
        MdnsPacketCodec.writeRecord(
            bytes, MdnsPacketCodec.encodeName(instanceFqdn), MdnsPacketCodec.TYPE_SRV,
            MdnsPacketCodec.DNS_CACHE_FLUSH_CLASS_IN, ttl, rdata.toByteArray(),
        )
        count++
    }

    fun txt(instanceFqdn: String, entries: List<String>, ttl: Int = MdnsPacketCodec.TTL_SECONDS) {
        val rdata = mutableListOf<Byte>()
        entries.forEach { value ->
            val b = value.encodeToByteArray()
            rdata.add(b.size.toByte())
            rdata.addAll(b.toList())
        }
        MdnsPacketCodec.writeRecord(
            bytes, MdnsPacketCodec.encodeName(instanceFqdn), MdnsPacketCodec.TYPE_TXT,
            MdnsPacketCodec.DNS_CACHE_FLUSH_CLASS_IN, ttl, rdata.toByteArray(),
        )
        count++
    }

    fun a(hostname: String, ips: List<String>, ttl: Int = MdnsPacketCodec.TTL_SECONDS) {
        ips.forEach { ip ->
            MdnsPacketCodec.writeRecord(
                bytes, MdnsPacketCodec.encodeName(hostname), MdnsPacketCodec.TYPE_A,
                MdnsPacketCodec.DNS_CACHE_FLUSH_CLASS_IN, ttl, ipToBytes(ip),
            )
            count++
        }
    }
}

/**
 * Base for responder tests: resets both singletons, installs a fake socket
 * factory and a fixed interface set, silences the log. Restores production
 * seams afterwards.
 */
internal open class MdnsResponderTestBase {
    protected val sockets = mutableListOf<FakeMdnsSocket>()
    protected val workers = mutableListOf<Pair<String, StaticWorkerHandle>>()

    /** Local interfaces the responder sees; override per test when needed. */
    protected var ifaces: List<Pair<MdnsIface, String>> =
        listOf(MdnsIface("wlan0", 24) to "192.168.1.10")

    @Before fun setUpMdnsSeams() {
        MdnsHostResponder.resetForTest()
        MdnsServiceBrowser.resetForTest()
        MdnsHostResponder.logSink = {}
        interfacesProvider = { ifaces }
        MdnsHostResponder.socketFactory = { FakeMdnsSocket().also { sockets += it } }
        installStaticWorkers()
    }

    /** Replaces worker threads with controllable handles so tests can kill them. */
    protected fun installStaticWorkers() {
        MdnsHostResponder.workerFactory = { name, _ ->
            StaticWorkerHandle(true).also { workers += name to it }
        }
    }

    /** Restores real receive threads — needed when a test drives the inbound path. */
    protected fun installRealWorkers() {
        MdnsHostResponder.workerFactory = { name, block -> startMdnsWorker(name, block) }
    }

    @After fun tearDownMdnsSeams() {
        MdnsHostResponder.resetForTest()
        MdnsServiceBrowser.resetForTest()
        MdnsHostResponder.logSink = { println("mDNS: $it") }
        MdnsHostResponder.socketFactory = { createMdnsSocket() }
        MdnsHostResponder.workerFactory = { name, block -> startMdnsWorker(name, block) }
        MdnsHostResponder.retryInitialDelayMs = 2_000L
        interfacesProvider = null
    }

    protected fun mainSocket(): FakeMdnsSocket = sockets[0]
    protected fun quSocket(): FakeMdnsSocket = sockets[1]
    protected fun workerNamed(name: String): StaticWorkerHandle =
        workers.first { it.first == name }.second
}
