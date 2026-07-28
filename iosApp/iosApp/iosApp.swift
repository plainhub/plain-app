import SwiftUI
import PlainShared

@main
struct PlainApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}

/// Wraps the Compose Multiplatform `MainViewController` in a SwiftUI view.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
