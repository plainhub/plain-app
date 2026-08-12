import Foundation
import Network
import NIOCore
import NIOPosix
import NIOHTTP1
import NIOWebSocket
import NIOConcurrencyHelpers
import PlainShared

// MARK: - SwiftNIO HTTP server implementing the Kotlin IosHttpServerBridge protocol

/// SwiftNIO-backed HTTP + WebSocket server that bridges every request to
/// Kotlin's `IosRequestProcessor` (via the PlainShared framework).
///
/// Lifecycle:
/// 1. `iosApp.swift` creates `PlainHttpServer()` and registers it with
///    `IosPlatformRegistry` at app launch.
/// 2. Kotlin's `startHttpServerService()` calls `start(httpPort:httpsPort:)`
///    through the `IosHttpServerBridge` protocol.
/// 3. For each HTTP request, the `HTTPHandler` creates a Kotlin
///    `IosRequestContext`, populates it, calls `IosRequestProcessor.processHttpRequest`
///    (a suspend fun exposed with a completion handler), then writes the
///    response back to the SwiftNIO channel.
/// 4. WebSocket upgrades are handled by `NIOWebSocketServerUpgrader`; the
///    `WebSocketHandler` creates a Kotlin `NioWsSession` and calls
///    `IosRequestProcessor.processWebSocket` to run the route handler.
public final class PlainHttpServer: NSObject, IosHttpServerBridge {

    private let group: MultiThreadedEventLoopGroup
    private var serverChannel: NIOCore.Channel?
    private let isStarted: NIOAtomic<Bool> = .makeAtomic(value: false)

    public override init() {
        self.group = MultiThreadedEventLoopGroup(numberOfThreads: System.coreCount)
        super.init()
    }

    @objc public func start(httpPort: Int32, httpsPort: Int32) -> Bool {
        if isStarted.load() { return true }
        // iOS 14+ Local Network Privacy: bind 之前主动向本地链路本地地址
        // 发送一个 UDP 包触发"本地网络访问"权限弹窗。仅用 127.0.0.1
        // 健康检查不会触发弹窗，此时外部设备通过 Wi-Fi 接口的入站
        // 连接会被 iOS 沙盒静默丢弃，表现为连接超时/被拒。
        Self.triggerLocalNetworkPrivacyPrompt()
        do {
            let upgrader = NIOWebSocketServerUpgrader(
                maxFrameSize: Int(UInt32.max),
                automaticErrorHandling: true,
                shouldUpgrade: { channel, _ in
                    channel.eventLoop.makeSucceededFuture(HTTPHeaders())
                },
                upgradePipelineHandler: { channel, requestHead in
                    self.handleWebSocketUpgrade(channel: channel, requestHead: requestHead)
                }
            )

            let bootstrap = ServerBootstrap(group: group)
                .serverChannelOption(ChannelOptions.backlog, value: 256)
                .serverChannelOption(ChannelOptions.socketOption(.so_reuseaddr), value: 1)
                .childChannelInitializer { channel in
                    let handler = HTTPHandler(server: self)
                    return channel.pipeline.configureHTTPServerPipeline(
                        withServerUpgrade: (
                            upgraders: [upgrader],
                            completionHandler: { _ in
                                // Upgrade succeeded — remove the HTTP handler since
                                // the pipeline now carries WebSocket frames, not
                                // HTTPServerRequestPart.
                                handler.removeFromPipeline()
                            }
                        )
                    ).flatMap {
                        channel.pipeline.addHandler(handler)
                    }
                }
                .childChannelOption(ChannelOptions.socketOption(.so_reuseaddr), value: 1)
                .childChannelOption(ChannelOptions.maxMessagesPerRead, value: 16)
                .childChannelOption(ChannelOptions.recvAllocator, value: AdaptiveRecvByteBufferAllocator())

            serverChannel = try bootstrap.bind(host: "0.0.0.0", port: Int(httpPort)).wait()
            isStarted.store(true)
            return true
        } catch {
            NSLog("PlainHttpServer: failed to start on port \(httpPort): \(error)")
            isStarted.store(false)
            return false
        }
    }

    @objc public func stop() {
        guard isStarted.load() else { return }
        isStarted.store(false)
        do {
            try serverChannel?.close().wait()
        } catch {
            NSLog("PlainHttpServer: error stopping server: \(error)")
        }
        serverChannel = nil
    }

    @objc public func isRunning() -> Bool {
        return isStarted.load()
    }

    // MARK: - Local Network Privacy trigger

    /// 在 HTTP 服务器 bind 之前调用，主动触发 iOS 14+ 的「本地网络访问」
    /// 权限弹窗。机制：通过 Network.framework 的 NWConnection 向一个链路
    /// 本地多播地址（224.0.0.1:5353，mDNS 标准多播组）发送一个空 UDP
    /// 包。系统检测到 app 尝试访问本地网络地址时就会弹出授权对话框；
    /// 用户授予权限后，外部设备通过 Wi‑Fi 接口访问 HTTP 服务器才不会
    /// 被 iOS 沙盒静默丢弃。UDP 包本身发送成败无关紧要。
    private static func triggerLocalNetworkPrivacyPrompt() {
        // 模拟器共享 Mac 网络栈，没有 iOS 沙盒限制，无需触发。
        #if targetEnvironment(simulator)
        return
        #else
        let host = NWEndpoint.Host("224.0.0.1")
        let port = NWEndpoint.Port(integerLiteral: 5353)
        let conn = NWConnection(host: host, port: port, using: .udp)
        let empty = Data()
        conn.send(content: empty, completion: .contentProcessed { _ in
            conn.cancel()
        })
        conn.start(queue: .global())
        // 100ms 后强制清理，避免 NWConnection 长期存活。
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
            conn.cancel()
        }
        #endif
    }

    // MARK: - WebSocket upgrade

    private func handleWebSocketUpgrade(channel: NIOCore.Channel, requestHead: HTTPRequestHead) -> EventLoopFuture<Void> {
        let path = requestHead.uri
        let headers = requestHead.headers.map { ($0.name, $0.value) }
        let queryParams = parseQueryParams(from: requestHead.uri)
        let remoteHost = channel.remoteAddress?.ipAddress ?? ""

        let reader = WebSocketFrameReader()
        return channel.pipeline.addHandler(reader).map {
            self.processWebSocket(
                path: path,
                channel: channel,
                requestHeaders: headers,
                queryParams: queryParams,
                remoteHost: remoteHost,
                frameReader: reader
            )
        }
    }

    // MARK: - Request processing (called from HTTPHandler / WebSocketHandler)

    /// Process a single HTTP request through Kotlin's HttpRouteRegistry.
    /// Runs on a background thread; the completion handler is called when the
    /// Kotlin route handler finishes writing the response.
    func processRequest(
        method: String,
        path: String,
        headers: [(String, String)],
        queryParams: [(String, String)],
        body: Data,
        remoteHost: String,
        completion: @escaping (IosRequestContext) -> Void
    ) {
        let ctx = IosRequestContext(method: method, path: path, remoteHost: remoteHost)

        // Populate headers
        for (name, value) in headers {
            ctx.setRequestHeader(name: name, value: value)
        }
        // Populate query parameters
        for (name, value) in queryParams {
            ctx.addQueryParam(name: name, value: value)
        }
        // Set body as KotlinByteArray
        ctx.setRequestBody(data: body.toKotlinByteArray())

        // Call the Kotlin suspend function. Kotlin/Native exposes suspend funs
        // to Swift as functions with a trailing completion handler.
        IosRequestProcessor.shared.processHttpRequest(ctx: ctx) { _ in
            // The completion handler is called on a Kotlin coroutine thread.
            // Hop to the SwiftNIO event loop if needed (the HTTPHandler does
            // this in its own callback).
            completion(ctx)
        }
    }

    /// Process a WebSocket upgrade through Kotlin's WebSocket route registry.
    fileprivate func processWebSocket(
        path: String,
        channel: NIOCore.Channel,
        requestHeaders: [(String, String)],
        queryParams: [(String, String)],
        remoteHost: String,
        frameReader: WebSocketFrameReader
    ) {
        let ctx = IosRequestContext(method: "GET", path: path, remoteHost: remoteHost)
        for (name, value) in requestHeaders {
            ctx.setRequestHeader(name: name, value: value)
        }
        for (name, value) in queryParams {
            ctx.addQueryParam(name: name, value: value)
        }

        // Create the Kotlin WsSession and the Swift transport that backs it.
        let transport = WsChannelTransport(channel: channel)
        let session = NioWsSession(remoteHost: remoteHost, transport: transport)
        transport.session = session
        frameReader.session = session

        // Launch the Kotlin WebSocket route handler.
        IosRequestProcessor.shared.processWebSocket(
            path: path,
            session: session,
            ctx: ctx
        )
    }
}

// MARK: - HTTP request handler

private final class HTTPHandler: ChannelInboundHandler, RemovableChannelHandler {
    typealias InboundIn = HTTPServerRequestPart
    typealias OutboundOut = HTTPServerResponsePart

    private let server: PlainHttpServer
    private var pendingHead: HTTPRequestHead?
    private var bodyAccumulator: Data = Data()
    private weak var channel: NIOCore.Channel?

    init(server: PlainHttpServer) {
        self.server = server
    }

    func handlerAdded(context: ChannelHandlerContext) {
        channel = context.channel
    }

    func removeFromPipeline() {
        if let channel = channel {
            channel.pipeline.removeHandler(self, promise: nil)
        }
    }

    func channelRead(context: ChannelHandlerContext, data: NIOAny) {
        let part = unwrapInboundIn(data)
        switch part {
        case .head(let head):
            pendingHead = head

        case .body(let buffer):
            var buf = buffer
            bodyAccumulator.append(contentsOf: buf.readableBytesView)

        case .end:
            guard let head = pendingHead else { return }
            let method = head.method.rawValue
            let path = head.uri
            let headers = head.headers.map { ($0.name, $0.value) }
            let queryParams = parseQueryParams(from: head.uri)
            let body = bodyAccumulator
            let remoteHost = context.remoteAddress?.ipAddress ?? ""

            // Reset state for keep-alive
            pendingHead = nil
            bodyAccumulator = Data()

            // Process the request through Kotlin
            server.processRequest(
                method: method,
                path: path,
                headers: headers,
                queryParams: queryParams,
                body: body,
                remoteHost: remoteHost
            ) { [weak self] ctx in
                guard let self = self else { return }
                self.writeResponse(context: context, ctx: ctx)
            }
        }
    }

    private func writeResponse(context: ChannelHandlerContext, ctx: IosRequestContext) {
        // Hop back to the channel's event loop before writing.
        let eventLoop = context.eventLoop
        eventLoop.execute {
            let status = HTTPResponseStatus(statusCode: Int(ctx.getResponseStatus()))
            var headers = HTTPHeaders()
            let responseHeaders = ctx.getResponseHeaders() as? [String: String] ?? [:]
            for (name, value) in responseHeaders {
                headers.add(name: name, value: value)
            }

            // Check if this is a file response (respondFile)
            if let filePath = ctx.getResponseFilePath() {
                self.serveFile(context: context, path: filePath, status: status, headers: headers, ctx: ctx)
                return
            }

            // Inline body response
            let bodyData = ctx.getResponseBody().toNSData() as Data
            headers.add(name: "Content-Length", value: "\(bodyData.count)")

            context.write(self.wrapOutboundOut(HTTPServerResponsePart.head(HTTPResponseHead(version: .http1_1, status: status, headers: headers))), promise: nil)
            if bodyData.isEmpty {
                context.writeAndFlush(self.wrapOutboundOut(.end(nil)), promise: nil)
            } else {
                var buffer = context.channel.allocator.buffer(capacity: bodyData.count)
                buffer.writeBytes(bodyData)
                context.write(self.wrapOutboundOut(.body(IOData.byteBuffer(buffer))), promise: nil)
                context.writeAndFlush(self.wrapOutboundOut(.end(nil)), promise: nil)
            }
        }
    }

    private func serveFile(
        context: ChannelHandlerContext,
        path: String,
        status: HTTPResponseStatus,
        headers: HTTPHeaders,
        ctx: IosRequestContext
    ) {
        let eventLoop = context.eventLoop
        eventLoop.execute {
            do {
                // ⚠️ iOS 沙盒禁止直接调用 sendfile() 系统调用（SIGSYS），
                // 因此在 iOS 上绝对不能使用 FileRegion（底层会走 sendfile）。
                // 这里把文件内容读入 Data，然后通过普通 ByteBuffer 写路径
                // 走 writev()，完全避开 sendfile。
                let fileData = try Data(contentsOf: URL(fileURLWithPath: path))

                var responseHeaders = headers
                if let contentType = ctx.getResponseFileContentType() {
                    responseHeaders.add(name: "Content-Type", value: contentType)
                }
                if let disposition = ctx.getResponseFileContentDisposition() {
                    responseHeaders.add(name: "Content-Disposition", value: disposition)
                }
                responseHeaders.add(name: "Content-Length", value: "\(fileData.count)")
                responseHeaders.add(name: "Accept-Ranges", value: "bytes")

                let head = HTTPResponseHead(version: .http1_1, status: status, headers: responseHeaders)
                context.write(self.wrapOutboundOut(HTTPServerResponsePart.head(head)), promise: nil)
                if !fileData.isEmpty {
                    var buffer = context.channel.allocator.buffer(capacity: fileData.count)
                    fileData.withUnsafeBytes { raw in
                        buffer.writeBytes(raw.bindMemory(to: UInt8.self))
                    }
                    context.write(self.wrapOutboundOut(.body(.byteBuffer(buffer))), promise: nil)
                }
                context.writeAndFlush(self.wrapOutboundOut(.end(nil)), promise: nil)
            } catch {
                NSLog("HTTPHandler: failed to serve file \(path): \(error)")
                var errHeaders = HTTPHeaders()
                errHeaders.add(name: "Content-Type", value: "text/plain")
                let body = "Internal Server Error"
                errHeaders.add(name: "Content-Length", value: "\(body.utf8.count)")
                let head = HTTPResponseHead(version: .http1_1, status: .internalServerError, headers: errHeaders)
                context.write(self.wrapOutboundOut(.head(head)), promise: nil)
                var buf = context.channel.allocator.buffer(capacity: body.utf8.count)
                buf.writeString(body)
                context.write(self.wrapOutboundOut(.body(IOData.byteBuffer(buf))), promise: nil)
                context.writeAndFlush(self.wrapOutboundOut(.end(nil)), promise: nil)
            }
        }
    }
}

// MARK: - WebSocket handling

private final class WebSocketFrameReader: ChannelInboundHandler {
    typealias InboundIn = WebSocketFrame

    weak var session: NioWsSession?

    init() {}

    func channelRead(context: ChannelHandlerContext, data: NIOAny) {
        let frame = unwrapInboundIn(data)
        switch frame.opcode {
        case .binary:
            var buf = frame.unmaskedData
            let bytes = buf.readBytes(length: buf.readableBytes) ?? []
            session?.onBinaryFrame(data: bytes.toKotlinByteArray())

        case .text:
            var buf = frame.unmaskedData
            let text = buf.readString(length: buf.readableBytes) ?? ""
            session?.onTextFrame(text: text)

        case .ping:
            // SwiftNIO auto-responds to pings with pongs; nothing to do here.
            break

        case .pong:
            break

        case .connectionClose:
            session?.onClose()
            context.close(promise: nil)

        default:
            break
        }
    }

    func handlerRemoved(context: ChannelHandlerContext) {
        session?.onClose()
    }
}

/// Transport that sends WebSocket frames back to the client through SwiftNIO.
/// Implements Kotlin's `IosWsTransport` protocol.
private final class WsChannelTransport: NSObject, IosWsTransport {
    private let channel: NIOCore.Channel
    weak var session: NioWsSession?

    init(channel: NIOCore.Channel) {
        self.channel = channel
        super.init()
    }

    @objc func sendBinary(data: KotlinByteArray) {
        let bytes = data.toNSData() as Data
        var buffer = channel.allocator.buffer(capacity: bytes.count)
        buffer.writeBytes(bytes)
        _ = channel.writeAndFlush(WebSocketFrame(fin: true, opcode: .binary, data: buffer))
    }

    @objc func sendText(text: String) {
        var buffer = channel.allocator.buffer(capacity: text.utf8.count)
        buffer.writeString(text)
        _ = channel.writeAndFlush(WebSocketFrame(fin: true, opcode: .text, data: buffer))
    }

    @objc func close(code: Int32, reason: String) {
        var buffer = channel.allocator.buffer(capacity: 2 + reason.utf8.count)
        buffer.writeInteger(UInt16(truncatingIfNeeded: code))
        buffer.writeString(reason)
        _ = channel.writeAndFlush(WebSocketFrame(fin: true, opcode: .connectionClose, data: buffer))
        _ = channel.close()
    }
}

// MARK: - Helpers

private func parseQueryParams(from uri: String) -> [(String, String)] {
    guard let questionIdx = uri.firstIndex(of: "?") else { return [] }
    let queryString = String(uri[uri.index(after: questionIdx)...])
    var result: [(String, String)] = []
    for pair in queryString.split(separator: "&") {
        let kv = pair.split(separator: "=", maxSplits: 1)
        if kv.count == 2 {
            let key = String(kv[0]).removingPercentEncoding ?? String(kv[0])
            let value = String(kv[1]).removingPercentEncoding ?? String(kv[1])
            result.append((key, value))
        } else if kv.count == 1 {
            let key = String(kv[0]).removingPercentEncoding ?? String(kv[0])
            result.append((key, ""))
        }
    }
    return result
}

// MARK: - Data → KotlinByteArray conversion

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(self.count))
        self.withUnsafeBytes { ptr in
            if let base = ptr.baseAddress {
                for i in 0..<self.count {
                    array.set(index: Int32(i), value: Int8(bitPattern: base.load(fromByteOffset: i, as: UInt8.self)))
                }
            }
        }
        return array
    }
}

private extension Array where Element == UInt8 {
    func toKotlinByteArray() -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(self.count))
        for (i, byte) in self.enumerated() {
            array.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return array
    }
}
