import SwiftUI
import PlainShared
import UIKit

/// Bridges `shareText` / `shareFile` / `shareFiles` from Kotlin to native iOS
/// share sheets via `UIActivityViewController`.
///
/// Kotlin calls `shareText` / `shareFile(path:mimeType:)` /
/// `shareFiles(paths:mimeTypes:)` (via `IosPlatformRegistry.shareController`)
/// when commonMain invokes the `shareText` / `shareFile` / `shareFiles`
/// expect fun. Swift presents a `UIActivityViewController` with the
/// appropriate items (text / file URL / multiple file URLs) and dismisses it
/// on completion.
///
/// `openFileExternal(path:)` presents a `UIDocumentInteractionController` so
/// the user can open the file in another app that registered for the file's
/// UTI.
final class ShareController: NSObject, IosShareController {
    private var documentInteractionController: UIDocumentInteractionController?

    func shareText(text: String) {
        DispatchQueue.main.async {
            guard let rootVC = self.topViewController() else { return }
            let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            self.present(activityVC, from: rootVC)
        }
    }

    func shareFile(path: String, mimeType: String) {
        DispatchQueue.main.async {
            guard let url = self.resolveFileURL(path) else { return }
            guard let rootVC = self.topViewController() else { return }
            let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
            self.present(activityVC, from: rootVC)
        }
    }

    func shareFiles(paths: [String], mimeTypes: [String]) {
        DispatchQueue.main.async {
            let urls = paths.compactMap { self.resolveFileURL($0) }
            if urls.isEmpty { return }
            guard let rootVC = self.topViewController() else { return }
            let activityVC = UIActivityViewController(activityItems: urls, applicationActivities: nil)
            self.present(activityVC, from: rootVC)
        }
    }

    func openFileExternal(path: String) {
        DispatchQueue.main.async {
            guard let url = self.resolveFileURL(path) else { return }
            let controller = UIDocumentInteractionController(url: url)
            controller.delegate = self
            controller.presentPreview(animated: true)
            self.documentInteractionController = controller
        }
    }

    // MARK: - Helpers

    private func resolveFileURL(_ path: String) -> URL? {
        if path.hasPrefix("http://") || path.hasPrefix("https://") {
            return URL(string: path)
        }
        if path.hasPrefix("file://") {
            return URL(string: path)
        }
        if path.hasPrefix("/") {
            return URL(fileURLWithPath: path)
        }
        return URL(fileURLWithPath: path)
    }

    private func present(_ controller: UIActivityViewController, from rootVC: UIViewController) {
        if let popover = controller.popoverPresentationController {
            popover.sourceView = rootVC.view
            popover.sourceRect = CGRect(
                x: rootVC.view.bounds.midX,
                y: rootVC.view.bounds.midY,
                width: 0,
                height: 0
            )
            popover.permittedArrowDirections = []
        }
        rootVC.present(controller, animated: true)
    }

    private func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }),
            let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            return nil
        }
        var top = root
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}

extension ShareController: UIDocumentInteractionControllerDelegate {
    func documentInteractionControllerViewControllerForPreview(
        _ controller: UIDocumentInteractionController
    ) -> UIViewController {
        return topViewController() ?? UIViewController()
    }
}
