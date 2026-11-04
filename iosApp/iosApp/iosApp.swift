import Darwin
import BackgroundTasks
import SwiftUI
import PlainShared
import Photos
import Contacts
import CoreLocation
import AVFoundation
import UserNotifications
import Network

@main
struct PlainApp: SwiftUI.App {
    private let backgroundScheduler = BackgroundScheduler()
    private let networkInfo = NetworkInfoProvider()
    private let permissionChecker = PermissionChecker()
    private let filePicker = FilePickerController()
    private let shareController = ShareController()
    private let soundMeter = SoundMeter()
    private let videoFrameObserver = VideoFrameObserver()
    private let notificationDelegate = NotificationDelegate()

    init() {
        IosPlatformRegistry.shared.setBackgroundScheduler(scheduler: backgroundScheduler)
        IosPlatformRegistry.shared.setNetworkInfoProvider(provider: networkInfo)
        IosPlatformRegistry.shared.setPermissionChecker(checker: permissionChecker)
        IosPlatformRegistry.shared.setFilePicker(picker: filePicker)
        IosPlatformRegistry.shared.setShareController(controller: shareController)
        IosPlatformRegistry.shared.setSoundMeter(meter: soundMeter)
        IosPlatformRegistry.shared.setVideoFrameObserver(observer: videoFrameObserver)
        UNUserNotificationCenter.current().delegate = notificationDelegate
        backgroundScheduler.register()
        AppInitializerKt.initializeApp()
        #if PLAIN_UI_WORKFLOWS
        NativeWorkflowRuntime.shared.prepare()
        #endif
    }

    var body: some Scene {
        WindowGroup {
            #if PLAIN_UI_WORKFLOWS
            WorkflowComposeView()
                .ignoresSafeArea()
                .background(Color(uiColor: .systemBackground))
            #else
            ComposeView()
                .ignoresSafeArea()
                // System background fills the gap between the system
                // LaunchScreen disappearing and the first Compose frame
                // rendering. Matches the LaunchScreenBackground asset so
                // the transition is visually seamless (no more white flash).
                .background(Color(uiColor: .systemBackground))
            #endif
        }
    }
}
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}

final class BackgroundScheduler: NSObject, IosBackgroundScheduler {
    private static let identifier = "com.ismartcoding.plain.background.refresh"

    func register() {
        let registered = BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.identifier, using: nil) { task in
            let lock = NSLock()
            lock.lock()
            var executionId: String?
            task.expirationHandler = {
                lock.lock()
                let id = executionId
                lock.unlock()
                if let id { BackgroundExecution.shared.cancel(id: id) }
            }
            executionId = BackgroundExecution.shared.begin(deadlineMs: Int64(Date().timeIntervalSince1970 * 1000) + 30_000) { success in
                task.setTaskCompleted(success: success.boolValue)
            }
            lock.unlock()
        }
        precondition(registered, "Background task registration failed")
    }

    func schedule(atMillis: Double) -> String {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.identifier)
        if atMillis <= 0 { return "" }
        let request = BGAppRefreshTaskRequest(identifier: Self.identifier)
        request.earliestBeginDate = Date(timeIntervalSince1970: atMillis / 1000)
        do { try BGTaskScheduler.shared.submit(request); return "" }
        catch { return error.localizedDescription }
    }
}

final class NetworkInfoProvider: NSObject, IosNetworkInfoProvider {
    private let monitor = NWPathMonitor()
    private let networkLock = NSLock()
    private var networkType = "NONE"

    override init() {
        super.init()
        monitor.pathUpdateHandler = { [weak self] path in
            guard let self else { return }
            let type: String
            if path.status != .satisfied { type = "NONE" }
            else if path.usesInterfaceType(.wifi) { type = "WIFI" }
            else if path.usesInterfaceType(.cellular) { type = "CELLULAR" }
            else if path.usesInterfaceType(.wiredEthernet) { type = "ETHERNET" }
            else { type = "OTHER" }
            self.networkLock.lock()
            self.networkType = type
            self.networkLock.unlock()
            BackgroundExecution.shared.executionStateChanged()
            NetworkEvents.shared.changed()
        }
        monitor.start(queue: DispatchQueue(label: "plain.network.facts"))
    }

    func getNetworkType() -> String {
        networkLock.lock()
        defer { networkLock.unlock() }
        return networkType
    }

    func isVpnActive() -> Bool {
        guard let selected = routeAddress(AF_INET, "192.0.2.1")
            ?? routeAddress(AF_INET6, "fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff") else { return false }
        var head: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&head) == 0 else { return false }
        defer { freeifaddrs(head) }
        var current = head
        while let row = current?.pointee {
            current = row.ifa_next
            guard let address = row.ifa_addr, let name = row.ifa_name,
                row.ifa_flags & UInt32(IFF_UP) != 0 else { continue }
            let interface = String(cString: name)
            let tunnel = row.ifa_flags & UInt32(IFF_POINTOPOINT) != 0
                || interface.hasPrefix("utun") || interface.hasPrefix("ipsec")
            if tunnel && numericAddress(address) == selected { return true }
        }
        return false
    }

    private func routeAddress(_ family: Int32, _ host: String) -> String? {
        var hints = addrinfo()
        hints.ai_flags = AI_NUMERICHOST
        hints.ai_family = family
        hints.ai_socktype = SOCK_DGRAM
        var result: UnsafeMutablePointer<addrinfo>?
        guard getaddrinfo(host, "1", &hints, &result) == 0, let result else { return nil }
        defer { freeaddrinfo(result) }
        let fd = socket(family, SOCK_DGRAM, 0)
        guard fd >= 0 else { return nil }
        defer { close(fd) }
        guard connect(fd, result.pointee.ai_addr, result.pointee.ai_addrlen) == 0 else { return nil }
        var address = sockaddr_storage()
        var length = socklen_t(MemoryLayout<sockaddr_storage>.size)
        return withUnsafeMutablePointer(to: &address) { pointer in
            pointer.withMemoryRebound(to: sockaddr.self, capacity: 1) { socketAddress in
                guard getsockname(fd, socketAddress, &length) == 0 else { return nil }
                return numericAddress(socketAddress)
            }
        }
    }

    private func numericAddress(_ address: UnsafePointer<sockaddr>) -> String? {
        var host = [CChar](repeating: 0, count: Int(NI_MAXHOST))
        guard getnameinfo(address, socklen_t(address.pointee.sa_len), &host,
            socklen_t(host.count), nil, 0, NI_NUMERICHOST) == 0 else { return nil }
        return String(cString: host)
    }

}

final class PermissionChecker: NSObject, IosPermissionChecker {
    private let locationDelegate = LocationPermissionDelegate()
    private var cachedNotificationStatus: UNAuthorizationStatus = .notDetermined

    override init() {
        super.init()
        refreshNotificationStatus()
    }

    func refreshNotificationStatus(completion: (() -> Void)? = nil) {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            self.cachedNotificationStatus = settings.authorizationStatus
            completion?()
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
            return locationDelegate.isGranted()
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
                self.locationDelegate.request()
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
                    self.refreshNotificationStatus {
                        IosPermissionCallback.shared.onPermissionResult(permissionName: permission, granted: granted)
                    }
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

final class LocationPermissionDelegate: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private let statusLock = NSLock()
    private var status: CLAuthorizationStatus = .notDetermined
    private var waitingForResult = false

    override init() {
        precondition(Thread.isMainThread)
        super.init()
        manager.delegate = self
    }

    func isGranted() -> Bool {
        statusLock.lock()
        defer { statusLock.unlock() }
        return status == .authorizedAlways || status == .authorizedWhenInUse
    }

    func request() {
        precondition(Thread.isMainThread)
        statusLock.lock()
        let current = status
        statusLock.unlock()
        if current != .notDetermined {
            IosPermissionCallback.shared.onPermissionResult(permissionName: "ACCESS_FINE_LOCATION", granted: isGranted())
            return
        }
        waitingForResult = true
        manager.requestWhenInUseAuthorization()
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let current = manager.authorizationStatus
        statusLock.lock()
        status = current
        statusLock.unlock()
        if waitingForResult && current != .notDetermined {
            waitingForResult = false
            IosPermissionCallback.shared.onPermissionResult(permissionName: "ACCESS_FINE_LOCATION", granted: isGranted())
        }
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
        #if PLAIN_UI_WORKFLOWS
        let inner = NativeWorkflowRuntimeKt.WorkflowMainViewController()
        #else
        let inner = MainViewControllerKt.MainViewController()
        #endif
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

#if PLAIN_UI_WORKFLOWS
private struct WorkflowComposeView: View {
    @State private var generation = UUID()
    var body: some View {
        ComposeView().id(generation).onAppear {
            NativeWorkflowRuntime.shared.setControllerReset { generation = UUID() }
        }
    }
}
#endif
