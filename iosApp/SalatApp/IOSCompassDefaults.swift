import Foundation
import SalatShared

/// Which accuracy threshold a device starts on.
///
/// There is deliberately no "automatic" mode: the seed is written once and from
/// then on the threshold is a plain number the user can see and change. A hidden
/// rule is impossible to reason about when the Qibla will not appear.
///
/// The iPhone-12 boundary itself lives in the shared module, where it is pinned by
/// tests; this only reads the hardware identifier to feed it.
enum IOSCompassDefaults {
    static var seedThresholdDegrees: Int {
        Int(SalatApi.shared.qiblaSeedThresholdForAppleDevice(modelIdentifier: modelIdentifier))
    }

    static var modelIdentifier: String {
        if let simulated = ProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] {
            return simulated
        }
        var system = utsname()
        uname(&system)
        let bytes = withUnsafeBytes(of: &system.machine) { raw in
            Array(raw.prefix(while: { $0 != 0 }))
        }
        return String(decoding: bytes, as: UTF8.self)
    }
}
