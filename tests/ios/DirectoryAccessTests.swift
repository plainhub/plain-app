import Foundation

@main
struct DirectoryAccessTests {
    static func main() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let folder = root.appendingPathComponent("Library", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let file = folder.appendingPathComponent("song.mp3")
        let bytes = Data("directory access fixture".utf8)
        try bytes.write(to: file)
        let suite = "directory-access-test-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: suite)!
        defer {
            defaults.removePersistentDomain(forName: suite)
            try? FileManager.default.removeItem(at: root)
        }
        var access: DirectoryAccess? = DirectoryAccess(defaults: defaults)
        try access!.add(folder)
        try access!.add(folder)
        precondition(access!.directories() == [folder.resolvingSymlinksInPath().path])
        precondition(!access!.hasUnavailableDirectories())
        let actualBytes = try Data(contentsOf: file)
        precondition(actualBytes == bytes)
        let rootEntries = try FileManager.default.contentsOfDirectory(atPath: root.path)
        precondition(rootEntries == ["Library"])
        access = nil
        access = DirectoryAccess(defaults: defaults)
        precondition(access!.directories() == [folder.resolvingSymlinksInPath().path])
        precondition(!access!.hasUnavailableDirectories())
        let restoredBytes = try Data(contentsOf: file)
        precondition(restoredBytes == bytes)
        do {
            try access!.add(file)
            preconditionFailure("A regular file must not become an authorized directory")
        } catch {}
        try FileManager.default.removeItem(at: folder)
        precondition(access!.directories().isEmpty)
        precondition(access!.hasUnavailableDirectories())
        print("Directory access: original bytes, no copy, deduplication, restart restore and removed-root filtering passed")
    }
}
