import PlainShared
import SwiftUI
import UIKit

struct ContentView: View {
    var body: some View {
        PlainHomeComposeView()
            .ignoresSafeArea()
    }
}

private struct PlainHomeComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        PlainHomeViewControllerKt.PlainHomeViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
