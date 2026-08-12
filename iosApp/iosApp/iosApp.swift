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
    private let soundMeter = SoundMeter()

    init() {
        IosPlatformRegistry.shared.setNetworkInfoProvider(provider: networkInfo)
        IosPlatformRegistry.shared.setHttpServerBridge(bridge: httpServer)
        IosPlatformRegistry.shared.setPermissionChecker(checker: permissionChecker)
        IosPlatformRegistry.shared.setFilePicker(picker: filePicker)
        IosPlatformRegistry.shared.setShareController(controller: shareController)
        IosPlatformRegistry.shared.setSslCertProvider(provider: sslCertManager)
        IosPlatformRegistry.shared.setSoundMeter(meter: soundMeter)
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                // System background fills the gap between the system
                // LaunchScreen disappearing and the first Compose frame
                // rendering. Matches the LaunchScreenBackground asset so
                // the transition is visually seamless (no more white flash).
                .background(Color(uiColor: .systemBackground))
        }
    }
}
final class NetworkInfoProvider: NSObject, IosNetworkInfoProvider {
    func getDeviceIP4s() -> [String] {
        collectDeviceIP4s(debugLabel: nil)
    }

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

/// A `UIWindow` that passes all touches through to windows below it.
/// Used for the immersive overlay so the Compose `Dialog` underneath still
/// receives tap events while the overlay window controls Status Bar visibility.
final class PassThroughWindow: UIWindow {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        nil
    }
}

/// Root VC for the immersive overlay window. Returns `true` for
/// `prefersStatusBarHidden` / `prefersHomeIndicatorAutoHidden` and defers
/// all edge system gestures, giving a clean pseudo-sleep black screen.
final class ImmersiveBarHidingVC: UIViewController {
    override var prefersStatusBarHidden: Bool { true }
    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation { .fade }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge { .all }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
    }
}

/// Wraps the Compose Multiplatform `MainViewController` in a container
/// `UIViewController`. When Kotlin requests immersive fullscreen (via
/// `IosSystemUiController`), the host creates a `PassThroughWindow` floating
/// above every other window (including the Compose `Dialog` window). That
/// window's root VC hides the Status Bar and Home Indicator, while its
/// `hitTest` returns `nil` so touches fall through to the Compose UI below.
final class ComposeHostingController: UIViewController, IosSystemUiController {
    private let composeVC: UIViewController
    private var immersiveWindow: UIWindow?
    private var previousKeyWindow: UIWindow?

    init(composeVC: UIViewController) {
        self.composeVC = composeVC
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(composeVC)
        view.addSubview(composeVC.view)
        composeVC.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            composeVC.view.topAnchor.constraint(equalTo: view.topAnchor),
            composeVC.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            composeVC.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            composeVC.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        composeVC.didMove(toParent: self)
    }

    // MARK: - IosSystemUiController

    func setImmersive(enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            if enabled {
                self?.presentImmersiveOverlay()
            } else {
                self?.dismissImmersiveOverlay()
            }
        }
    }

    // MARK: - Immersive overlay window

    private func presentImmersiveOverlay() {
        guard immersiveWindow == nil else { return }

        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive })
        else { return }

        // Remember the current key window so we can restore it when immersive
        // mode ends (it may be the Compose Dialog's window).
        previousKeyWindow = scene.windows.first(where: { $0.isKeyWindow })

        let window = PassThroughWindow(windowScene: scene)
        // Level above .alert so it also covers Compose Dialog windows.
        window.windowLevel = .alert + 1
        window.backgroundColor = .clear
        window.rootViewController = ImmersiveBarHidingVC()
        // Making this window key means its root VC controls the Status Bar.
        window.makeKeyAndVisible()
        immersiveWindow = window
    }

    private func dismissImmersiveOverlay() {
        immersiveWindow?.isHidden = true
        immersiveWindow = nil
        if let prev = previousKeyWindow, !prev.isHidden {
            prev.makeKey()
        }
        previousKeyWindow = nil
    }
}

/// Wraps the Compose Multiplatform `MainViewController` in a SwiftUI view.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeView>) -> UIViewController {
        let inner = MainViewControllerKt.MainViewController()
        // Use the system background color (adapts to light/dark mode) so it
        // matches the LaunchScreen's LaunchScreenBackground asset and avoids
        // a visible color flash between the system splash screen and the
        // first Compose frame. Also suppresses the green Metal-clear flash.
        inner.view.backgroundColor = .systemBackground
        let host = ComposeHostingController(composeVC: inner)
        IosPlatformRegistry.shared.setSystemUiController(controller: host)
        return host
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<ComposeView>) {}
}
