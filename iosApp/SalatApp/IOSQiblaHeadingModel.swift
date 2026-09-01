import Combine
import CoreLocation
import Foundation
import UIKit

@MainActor
final class IOSQiblaHeadingModel: NSObject, ObservableObject, @preconcurrency CLLocationManagerDelegate {
    @Published private(set) var heading: Double?
    @Published private(set) var accuracy: Double?

    private let manager = CLLocationManager()
    private var orientationObserver: NSObjectProtocol?

    var isAvailable: Bool { CLLocationManager.headingAvailable() }

    override init() {
        super.init()
        manager.delegate = self
        manager.headingFilter = 1
        applyDeviceOrientation()
    }

    deinit {
        if let orientationObserver {
            NotificationCenter.default.removeObserver(orientationObserver)
        }
    }

    func start() {
        guard isAvailable else { return }
        applyDeviceOrientation()
        // CLHeading is reported against a fixed reference, so on a rotated screen the
        // whole rose is a quarter turn out unless CoreLocation is told which way up
        // the interface is.
        if orientationObserver == nil {
            UIDevice.current.beginGeneratingDeviceOrientationNotifications()
            orientationObserver = NotificationCenter.default.addObserver(
                forName: UIDevice.orientationDidChangeNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                MainActor.assumeIsolated { self?.applyDeviceOrientation() }
            }
        }
        manager.startUpdatingHeading()
    }

    func stop() {
        manager.stopUpdatingHeading()
        if let orientationObserver {
            NotificationCenter.default.removeObserver(orientationObserver)
            UIDevice.current.endGeneratingDeviceOrientationNotifications()
        }
        orientationObserver = nil
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

    private func applyDeviceOrientation() {
        manager.headingOrientation = Self.headingOrientation(for: interfaceOrientation)
    }

    private var interfaceOrientation: UIInterfaceOrientation {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?
            .interfaceOrientation ?? .portrait
    }

    /// The interface orientation, not the device's: a phone lying flat still has an
    /// interface that is the right way up, and that is what the rose is drawn in.
    static func headingOrientation(for orientation: UIInterfaceOrientation) -> CLDeviceOrientation {
        switch orientation {
        case .landscapeLeft: return .landscapeLeft
        case .landscapeRight: return .landscapeRight
        case .portraitUpsideDown: return .portraitUpsideDown
        default: return .portrait
        }
    }
}
