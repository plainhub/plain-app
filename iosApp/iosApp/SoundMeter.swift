import Foundation
import AVFoundation
import PlainShared

/// Swift-backed sound meter that bridges AVAudioRecorder to Kotlin's
/// `IosSoundMeter` protocol. The Kotlin side polls `peakPower()` every ~180 ms
/// to get the latest dBFS reading, which it then converts to the Android
/// decibel scale (dBFS + 89.2 ≈ 16-bit PCM dB).
final class SoundMeter: NSObject, IosSoundMeter {
    private var recorder: AVAudioRecorder?
    private var tempFileURL: URL?

    @objc func start_() -> Bool {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let tempURL = FileManager.default.temporaryDirectory
                .appendingPathComponent("sound_meter_temp_\(UUID().uuidString).caf")
            tempFileURL = tempURL

            let settings: [String: Any] = [
                AVFormatIDKey: kAudioFormatMPEG4AAC,
                AVSampleRateKey: 44100.0,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue,
            ]

            let recorder = try AVAudioRecorder(url: tempURL, settings: settings)
            recorder.isMeteringEnabled = true
            recorder.prepareToRecord()

            guard recorder.record() else {
                NSLog("SoundMeter: failed to start recording")
                return false
            }

            self.recorder = recorder
            return true
        } catch {
            NSLog("SoundMeter: error starting sound meter: \(error)")
            return false
        }
    }

    @objc func stop() {
        recorder?.stop()
        recorder = nil

        if let url = tempFileURL {
            try? FileManager.default.removeItem(at: url)
            tempFileURL = nil
        }

        do {
            try AVAudioSession.sharedInstance().setActive(false)
        } catch {
            NSLog("SoundMeter: error deactivating audio session: \(error)")
        }
    }

    @objc func peakPower() -> Float {
        guard let recorder = recorder else { return -Float.greatestFiniteMagnitude }
        recorder.updateMeters()
        return recorder.peakPower(forChannel: 0)
    }
}
