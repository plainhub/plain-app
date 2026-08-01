import SwiftUI
import PlainShared
import PhotosUI
import UniformTypeIdentifiers
import UIKit

/// Bridges `PickFileEvent` / `ExportFileEvent` from Kotlin to native iOS pickers.
///
/// Kotlin calls `pickFile(tag:type:multiple:)` (via `IosPlatformRegistry.filePicker`)
/// when commonMain emits `PickFileEvent`. Depending on the type this presents:
///  - IMAGE / IMAGE_VIDEO → `PHPickerViewController` (system photo picker)
///  - FILE                → `UIDocumentPickerViewController` (document picker, asCopy)
///  - FOLDER              → `UIDocumentPickerViewController` (folder picker)
///
/// Picked items are copied into the app temp directory so the resulting
/// `file://` URLs are freely readable by Kotlin's `queryPickedFileInfo` /
/// `importChatFile` without security-scope concerns. On completion Swift calls
/// `IosFilePickerCallback.onPickResult`, which sends `PickFileResultEvent`.
///
/// `exportFile(type:fileName:)` resolves to a writable URL inside the app's
/// Documents directory (visible in the Files app) and reports it via
/// `IosFilePickerCallback.onExportResult`. This matches Android's
/// ACTION_CREATE_DOCUMENT contract (consumer writes to the returned URI).
final class FilePickerController: NSObject, IosFilePicker {
    private var currentTag: String = ""
    private var currentType: String = ""
    private var photoDelegate: PhotoPickerDelegate?
    private var documentDelegate: DocumentPickerDelegate?

    func pickFile(tag: String, type: String, multiple: Bool) {
        DispatchQueue.main.async {
            self.currentTag = tag
            self.currentType = type
            guard let rootVC = self.topViewController() else { return }
            switch type {
            case "IMAGE", "IMAGE_VIDEO":
                self.presentPhotoPicker(multiple: multiple, from: rootVC)
            case "FOLDER":
                self.presentDocumentPicker(contentTypes: [UTType.folder], multiple: false, from: rootVC)
            default:
                self.presentDocumentPicker(contentTypes: [UTType.item], multiple: multiple, from: rootVC)
            }
        }
    }

    func exportFile(type: String, fileName: String) {
        DispatchQueue.main.async {
            let docsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let destURL = docsDir.appendingPathComponent(fileName)
            try? FileManager.default.removeItem(at: destURL)
            IosFilePickerCallback.shared.onExportResult(type: type, uri: destURL.absoluteString)
        }
    }

    // MARK: - Picker presentation

    private func presentPhotoPicker(multiple: Bool, from rootVC: UIViewController) {
        var config = PHPickerConfiguration()
        config.filter = currentType == "IMAGE" ? .images : .any(of: [.images, .videos])
        config.selectionLimit = multiple ? 0 : 1
        let picker = PHPickerViewController(configuration: config)
        let delegate = PhotoPickerDelegate()
        delegate.onComplete = { [weak self] uris in
            guard let self = self else { return }
            if !uris.isEmpty {
                IosFilePickerCallback.shared.onPickResult(tag: self.currentTag, type: self.currentType, uris: uris)
            }
            self.photoDelegate = nil
        }
        picker.delegate = delegate
        self.photoDelegate = delegate
        rootVC.present(picker, animated: true)
    }

    private func presentDocumentPicker(contentTypes: [UTType], multiple: Bool, from rootVC: UIViewController) {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: contentTypes, asCopy: true)
        picker.allowsMultipleSelection = multiple
        let delegate = DocumentPickerDelegate()
        delegate.onComplete = { [weak self] uris in
            guard let self = self else { return }
            if !uris.isEmpty {
                IosFilePickerCallback.shared.onPickResult(tag: self.currentTag, type: self.currentType, uris: uris)
            }
            self.documentDelegate = nil
        }
        picker.delegate = delegate
        self.documentDelegate = delegate
        rootVC.present(picker, animated: true)
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

/// PHPicker delegate. Each `PHPickerResult` is loaded via
/// `loadFileRepresentation`, which yields a temporary `file://` URL that is
/// invalidated once the completion handler returns. We copy each item into the
/// app temp directory (unique names to avoid collisions) before the handler
/// returns, then report the collected URLs once all loads finish.
private final class PhotoPickerDelegate: NSObject, PHPickerViewControllerDelegate {
    var onComplete: ([String]) -> Void = { _ in }

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        if results.isEmpty {
            onComplete([])
            return
        }
        let group = DispatchGroup()
        var collected: [String] = []
        let lock = NSLock()
        for result in results {
            let identifiers = result.itemProvider.registeredTypeIdentifiers
            let typeID = identifiers.first(where: { $0.hasPrefix("public.image") || $0.hasPrefix("public.movie") || $0.hasPrefix("public.audio") }) ?? identifiers.first ?? "public.data"
            group.enter()
            result.itemProvider.loadFileRepresentation(forTypeIdentifier: typeID) { url, _ in
                if let url = url {
                    let name = "\(UUID().uuidString)_\(url.lastPathComponent)"
                    let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(name)
                    try? FileManager.default.removeItem(at: tempURL)
                    do {
                        try FileManager.default.copyItem(at: url, to: tempURL)
                        lock.lock()
                        collected.append(tempURL.absoluteString)
                        lock.unlock()
                    } catch {
                        // copy failed — skip this item
                    }
                }
                group.leave()
            }
        }
        group.notify(queue: .main) { [weak self] in
            self?.onComplete(collected)
        }
    }
}

/// UIDocumentPicker delegate. With `asCopy: true` the picked URLs already live
/// in the app temp directory and need no security-scope handling.
private final class DocumentPickerDelegate: NSObject, UIDocumentPickerDelegate {
    var onComplete: ([String]) -> Void = { _ in }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        controller.dismiss(animated: true)
        onComplete(urls.map { $0.absoluteString })
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        controller.dismiss(animated: true)
        onComplete([])
    }
}
