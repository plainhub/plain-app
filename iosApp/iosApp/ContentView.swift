import PlainShared
import SwiftUI
import UIKit

struct ContentView: View {
    var body: some View {
        SharedAppNavHostRepresentable()
            .ignoresSafeArea()
    }
}

private struct SharedAppNavHostRepresentable: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        SharedAppNavHostKt.SharedAppNavHost()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
