import Foundation

final class DirectoryAccess {
    private struct Grant {
        var bookmark: Data
        var url: URL?
        var scoped: Bool
    }

    private let defaults: UserDefaults
    private let key = "directoryAccessBookmarks"
    private let lock = NSLock()
    private var grants: [Grant] = []

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let bookmarks = defaults.array(forKey: key) as? [Data] ?? []
        for bookmark in bookmarks {
            do {
                var stale = false
                let url = try URL(resolvingBookmarkData: bookmark, options: [], relativeTo: nil, bookmarkDataIsStale: &stale)
                let scoped = url.startAccessingSecurityScopedResource()
                do {
                    let renewed = stale ? try url.bookmarkData(options: .minimalBookmark, includingResourceValuesForKeys: nil, relativeTo: nil) : bookmark
                    grants.append(Grant(bookmark: renewed, url: url, scoped: scoped))
                } catch {
                    if scoped { url.stopAccessingSecurityScopedResource() }
                    throw error
                }
            } catch {
                grants.append(Grant(bookmark: bookmark, url: nil, scoped: false))
                NSLog("Directory access restore failed: %@", error.localizedDescription)
            }
        }
        defaults.set(grants.map { $0.bookmark }, forKey: key)
    }

    deinit {
        for grant in grants where grant.scoped { grant.url?.stopAccessingSecurityScopedResource() }
    }

    func add(_ url: URL) throws {
        let scoped = url.startAccessingSecurityScopedResource()
        do {
            let values = try url.resourceValues(forKeys: [.isDirectoryKey])
            guard values.isDirectory == true else { throw CocoaError(.fileReadUnsupportedScheme) }
            guard FileManager.default.isReadableFile(atPath: url.path) else { throw CocoaError(.fileReadNoPermission) }
            let bookmark = try url.bookmarkData(options: .minimalBookmark, includingResourceValuesForKeys: nil, relativeTo: nil)
            lock.lock()
            defer { lock.unlock() }
            let grant = Grant(bookmark: bookmark, url: url, scoped: scoped)
            if let index = grants.firstIndex(where: { $0.url?.resolvingSymlinksInPath().path == url.resolvingSymlinksInPath().path }) {
                let previous = grants[index]
                grants[index] = grant
                if previous.scoped { previous.url?.stopAccessingSecurityScopedResource() }
            } else {
                grants.append(grant)
            }
            defaults.set(grants.map { $0.bookmark }, forKey: key)
        } catch {
            if scoped { url.stopAccessingSecurityScopedResource() }
            throw error
        }
    }

    func hasUnavailableDirectories() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return grants.contains { grant in
            guard let url = grant.url else { return true }
            var directory: ObjCBool = false
            return !FileManager.default.fileExists(atPath: url.path, isDirectory: &directory) ||
                !directory.boolValue || !FileManager.default.isReadableFile(atPath: url.path)
        }
    }

    func directories() -> [String] {
        lock.lock()
        defer { lock.unlock() }
        return grants.compactMap { $0.url }.filter {
            var directory: ObjCBool = false
            return FileManager.default.fileExists(atPath: $0.path, isDirectory: &directory) &&
                directory.boolValue && FileManager.default.isReadableFile(atPath: $0.path)
        }.map { $0.resolvingSymlinksInPath().path }
    }
}
