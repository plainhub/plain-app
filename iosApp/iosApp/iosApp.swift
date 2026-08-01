import SwiftUI
import PlainShared
import Photos
import Contacts
import CoreLocation
import AVFoundation
import UserNotifications

@main
struct PlainApp: SwiftUI.App {
    private let httpServer = PlainHttpServer()
    private let networkInfo = NetworkInfoProvider()
    private let permissionChecker = PermissionChecker()
    private let filePicker = FilePickerController()
    private let shareController = ShareController()
    private let sslCertManager = SslCertManager()

    init() {
        IosPlatformRegistry.shared.setHttpServerBridge(bridge: httpServer)
        IosPlatformRegistry.shared.setNetworkInfoProvider(provider: networkInfo)
        IosPlatformRegistry.shared.setPermissionChecker(checker: permissionChecker)
        IosPlatformRegistry.shared.setFilePicker(picker: filePicker)
        IosPlatformRegistry.shared.setShareController(controller: shareController)
        IosPlatformRegistry.shared.setSslCertProvider(provider: sslCertManager)
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
        var results: [String] = []
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else { return [] }
        defer { freeifaddrs(ifaddr) }
        var ptr: UnsafeMutablePointer<ifaddrs>? = firstAddr
        while let current = ptr {
            let addr = current.pointee.ifa_addr
            if addr != nil && addr!.pointee.sa_family == sa_family_t(AF_INET) {
                var hostBuf = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                let len = socklen_t(MemoryLayout<sockaddr_in>.size)
                if getnameinfo(addr!, len, &hostBuf, socklen_t(hostBuf.count), nil, 0, NI_NUMERICHOST) == 0 {
                    let ip = String(cString: hostBuf)
                    if ip != "127.0.0.1" && !ip.hasPrefix("169.254.") {
                        results.append(ip)
                    }
                }
            }
            ptr = current.pointee.ifa_next
        }
        return results
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
