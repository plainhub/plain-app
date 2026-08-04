import SwiftUI
import PlainShared
import Photos
import Contacts
import CoreLocation
import AVFoundation
import UserNotifications
import Network
import Darwin

@main
struct PlainApp: SwiftUI.App {
    private let httpServer = PlainHttpServer()
    private let networkInfo = NetworkInfoProvider()
    private let permissionChecker = PermissionChecker()
    private let filePicker = FilePickerController()
    private let shareController = ShareController()
    private let sslCertManager = SslCertManager()

    init() {
        IosPlatformRegistry.shared.setNetworkInfoProvider(provider: networkInfo)
        IosPlatformRegistry.shared.setHttpServerBridge(bridge: httpServer)
        IosPlatformRegistry.shared.setPermissionChecker(checker: permissionChecker)
        IosPlatformRegistry.shared.setFilePicker(picker: filePicker)
        IosPlatformRegistry.shared.setShareController(controller: shareController)
        IosPlatformRegistry.shared.setSslCertProvider(provider: sslCertManager)
#if DEBUG
        let provider = networkInfo
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            provider.debugDumpDeviceIP4s(label: "startup+1s")
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            provider.debugDumpDeviceIP4s(label: "startup+5s")
        }
#endif
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}

final class NetworkInfoProvider: NSObject, IosNetworkInfoProvider {
    func getDeviceIP4s() -> [String] {
        collectDeviceIP4s(debugLabel: nil)
    }

#if DEBUG
    func debugDumpDeviceIP4s(label: String) {
        _ = collectDeviceIP4s(debugLabel: label)
    }
#endif

    private func collectDeviceIP4s(debugLabel: String?) -> [String] {
        debugLog(debugLabel, "begin")
        var entries: [(name: String, ip: String)] = []
        var seen = Set<String>()
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else {
            debugLog(debugLabel, "getifaddrs failed errno=\(errno)")
            return []
        }
        defer { freeifaddrs(ifaddr) }
        var ptr = ifaddr
        while let current = ptr {
            defer { ptr = current.pointee.ifa_next }
            let name = current.pointee.ifa_name.map { String(cString: $0) } ?? ""
            guard let addr = current.pointee.ifa_addr else {
                debugLog(debugLabel, "\(name): skip missing address")
                continue
            }
            let family = Int32(addr.pointee.sa_family)
            guard family == AF_INET else {
                debugLog(debugLabel, "\(name): skip family=\(family)")
                continue
            }

            let flags = Int32(current.pointee.ifa_flags)
            let isUp = (flags & IFF_UP) != 0
            let isLoopbackInterface = (flags & IFF_LOOPBACK) != 0
            guard isUp && !isLoopbackInterface else {
                debugLog(debugLabel, "\(name): skip flags=\(flags) isUp=\(isUp) loopback=\(isLoopbackInterface)")
                continue
            }

            guard let ip = ipv4String(from: addr) else {
                debugLog(debugLabel, "\(name): skip could not format IPv4")
                continue
            }
            guard !ip.hasPrefix("127.") else {
                debugLog(debugLabel, "\(name): skip loopback ip=\(ip)")
                continue
            }
            guard seen.insert(ip).inserted else {
                debugLog(debugLabel, "\(name): skip duplicate ip=\(ip)")
                continue
            }

            debugLog(debugLabel, "\(name): accept ip=\(ip) flags=\(flags)")
            entries.append((name: name, ip: ip))
        }

        if entries.isEmpty {
            let routedIp = routedIPv4Address()
            debugLog(debugLabel, "route fallback ip=\(routedIp ?? "<nil>")")
            if let routedIp, !routedIp.hasPrefix("127.") {
                entries.append((name: "route", ip: routedIp))
            }
        }

        let result = entries
            .sorted { lhs, rhs in priority(lhs) < priority(rhs) }
            .map(\.ip)
        debugLog(debugLabel, "result=\(result)")
        return result
    }

    private func ipv4String(from addr: UnsafePointer<sockaddr>) -> String? {
        var host = [CChar](repeating: 0, count: Int(NI_MAXHOST))
        let length = addr.pointee.sa_len > 0
            ? socklen_t(addr.pointee.sa_len)
            : socklen_t(MemoryLayout<sockaddr_in>.size)
        let result = getnameinfo(
            addr,
            length,
            &host,
            socklen_t(host.count),
            nil,
            0,
            NI_NUMERICHOST
        )
        if result == 0 {
            return String(cString: host)
        }

        var addrIn = addr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee }
        var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
        guard inet_ntop(AF_INET, &addrIn.sin_addr, &buffer, socklen_t(INET_ADDRSTRLEN)) != nil else {
            return nil
        }
        return String(cString: buffer)
    }

    private func routedIPv4Address() -> String? {
        let fd = socket(AF_INET, SOCK_DGRAM, 0)
        guard fd >= 0 else { return nil }
        defer { close(fd) }

        var remote = sockaddr_in()
        remote.sin_len = __uint8_t(MemoryLayout<sockaddr_in>.size)
        remote.sin_family = sa_family_t(AF_INET)
        remote.sin_port = in_port_t(53).bigEndian
        guard inet_pton(AF_INET, "8.8.8.8", &remote.sin_addr) == 1 else { return nil }

        let connectResult = withUnsafePointer(to: &remote) { pointer in
            pointer.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                Darwin.connect(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }
        guard connectResult == 0 else { return nil }

        var local = sockaddr_in()
        var localLength = socklen_t(MemoryLayout<sockaddr_in>.size)
        let nameResult = withUnsafeMutablePointer(to: &local) { pointer in
            pointer.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                getsockname(fd, $0, &localLength)
            }
        }
        guard nameResult == 0 else { return nil }

        var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
        var localAddr = local.sin_addr
        guard inet_ntop(AF_INET, &localAddr, &buffer, socklen_t(INET_ADDRSTRLEN)) != nil else {
            return nil
        }
        return String(cString: buffer)
    }

    private func priority(_ entry: (name: String, ip: String)) -> Int {
        if entry.name == "en0" && isPrivateIPv4(entry.ip) { return 0 }
        if entry.name.hasPrefix("en") && isPrivateIPv4(entry.ip) { return 1 }
        if isPrivateIPv4(entry.ip) { return 2 }
        if entry.name == "en0" { return 3 }
        if entry.name.hasPrefix("en") { return 4 }
        if isVpnInterface(entry.name) { return 6 }
        return 5
    }

    private func isPrivateIPv4(_ ip: String) -> Bool {
        let parts = ip.split(separator: ".").compactMap { Int($0) }
        guard parts.count == 4 else { return false }
        if parts[0] == 10 { return true }
        if parts[0] == 192 && parts[1] == 168 { return true }
        if parts[0] == 172 && (16...31).contains(parts[1]) { return true }
        return false
    }

    private func isVpnInterface(_ name: String) -> Bool {
        name.hasPrefix("utun") ||
            name.hasPrefix("tun") ||
            name.hasPrefix("ppp") ||
            name.hasPrefix("ipsec") ||
            name.hasPrefix("tap")
    }

    private func debugLog(_ label: String?, _ message: String) {
#if DEBUG
        guard let label else { return }
        print("[PlainApp][NetworkInfoProvider][\(label)] \(message)")
#endif
    }
}

final class PermissionChecker: NSObject, IosPermissionChecker {
    private var locationDelegate: LocationPermissionDelegate?
    private var cachedNotificationStatus: UNAuthorizationStatus = .notDetermined

    override init() {
        super.init()
        refreshNotificationStatus()
    }

    func refreshNotificationStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            self.cachedNotificationStatus = settings.authorizationStatus
        }
    }

    func isGranted(permission: String) -> Bool {
        switch permission {
        case "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEOS", "READ_MEDIA_AUDIO":
            let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
            return status == .authorized || status == .limited
        case "READ_CONTACTS", "WRITE_CONTACTS":
            return CNContactStore.authorizationStatus(for: .contacts) == .authorized
        case "ACCESS_FINE_LOCATION":
            let status = CLLocationManager().authorizationStatus
            return status == .authorizedAlways || status == .authorizedWhenInUse
        case "CAMERA":
            return AVCaptureDevice.authorizationStatus(for: .video) == .authorized
        case "RECORD_AUDIO":
            return AVCaptureDevice.authorizationStatus(for: .audio) == .authorized
        case "POST_NOTIFICATIONS":
            return cachedNotificationStatus == .authorized
        default:
            return true
        }
    }

    func requestPermission(permission: String) {
        DispatchQueue.main.async {
            switch permission {
            case "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEOS", "READ_MEDIA_AUDIO":
                PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
                    let granted = status == .authorized || status == .limited
                    IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: granted)
                }
            case "READ_CONTACTS", "WRITE_CONTACTS":
                CNContactStore().requestAccess(for: .contacts) { granted, _ in
                    IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: granted)
                }
            case "ACCESS_FINE_LOCATION":
                self.locationDelegate = LocationPermissionDelegate(permissionName: permission)
                self.locationDelegate?.request()
            case "CAMERA":
                AVCaptureDevice.requestAccess(for: .video) { granted in
                    IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: granted)
                }
            case "RECORD_AUDIO":
                AVCaptureDevice.requestAccess(for: .audio) { granted in
                    IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: granted)
                }
            case "POST_NOTIFICATIONS":
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
                    self.refreshNotificationStatus()
                    IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: granted)
                }
            default:
                IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: true)
            }
        }
    }

    func openAppSettings() {
        DispatchQueue.main.async {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        }
    }
}

/// CLLocationManager requires a strong delegate reference and a delegate
/// callback to learn the result of `requestWhenInUseAuthorization()`.
/// This class holds the manager alive until the user responds, then reports
/// the result via `IosPermissionCallback` and releases itself.
final class LocationPermissionDelegate: NSObject, CLLocationManagerDelegate {
    private let permissionName: String
    private let manager = CLLocationManager()
    private var previousStatus: CLAuthorizationStatus

    init(permissionName: String) {
        self.permissionName = permissionName
        self.previousStatus = CLLocationManager().authorizationStatus
        super.init()
        manager.delegate = self
    }

    func request() {
        manager.requestWhenInUseAuthorization()
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        if status == previousStatus { return }
        previousStatus = status
        let granted = status == .authorizedAlways || status == .authorizedWhenInUse
        IosPermissionCallback.shared.onPermissionResult(permissionName: permissionName, granted: granted)
    }
}

/// Wraps the Compose Multiplatform `MainViewController` in a SwiftUI view.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeView>) -> UIViewController {
        let vc = MainViewControllerKt.MainViewController()
        // Prevent the green flash from the Metal surface before the first
        // Compose frame is rendered. Use white to match the app's background.
        vc.view.backgroundColor = .white
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<ComposeView>) {}
}
