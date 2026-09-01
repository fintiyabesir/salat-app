import Combine
import CoreLocation
import Foundation

@MainActor
final class IOSQiblaHeadingModel: NSObject, ObservableObject, @preconcurrency CLLocationManagerDelegate {
    @Published private(set) var heading: Double?
    @Published private(set) var accuracy: Double?

    private let manager = CLLocationManager()

    var isAvailable: Bool { CLLocationManager.headingAvailable() }

    override init() {
        super.init()
        manager.delegate = self
        manager.headingFilter = 1
    }

    func start() {
        guard isAvailable else { return }
        manager.startUpdatingHeading()
    }

    func stop() {
        manager.stopUpdatingHeading()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let preferred = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
        heading = preferred
        // A negative headingAccuracy means the reading is unusable, not that the
        // whole update should be discarded — dropping it left the compass looking
        // dead instead of merely uncalibrated.
        accuracy = newHeading.headingAccuracy >= 0 ? newHeading.headingAccuracy : nil
    }

    func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool {
        true
    }
}
