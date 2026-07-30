import SwiftUI
import PlainShared

@main
struct PlainApp: SwiftUI.App {
    // Keep a strong reference to the HTTP server so it survives the SwiftUI
    // scene lifecycle. Registered with IosPlatformRegistry in init().
    private let httpServer = PlainHttpServer()

    init() {
        // Register the SwiftNIO-backed HTTP server with Kotlin before the
        // Compose UI tree is created so that startHttpServerService() can
        // dispatch through the bridge as soon as the user enables the web
        // console.
        IosPlatformRegistry.shared.setHttpServerBridge(bridge: httpServer)
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
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
