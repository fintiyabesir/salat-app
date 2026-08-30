import Combine
import CoreLocation
import Foundation

struct PrayerLocation: Equatable, Codable {
    let latitude: Double
    let longitude: Double
    let timeZoneId: String
    let countryCode: String
    let cityName: String?
    let regionName: String?

    var displayName: String {
        cityName ?? regionName ?? countryCode
    }
}

@MainActor
final class IOSLocationModel: NSObject, ObservableObject, @preconcurrency CLLocationManagerDelegate {
    @Published private(set) var location: PrayerLocation?
    @Published private(set) var isResolving = false
    @Published private(set) var errorMessage: String?

    private let manager = CLLocationManager()
    private let geocoder = CLGeocoder()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
        // Open on prayer times, not on the location picker. Without this the app
        // discards the resolved location on every launch, which forces a user who
        // declined location access to pick their city again every single time.
        location = Self.restore()
    }

    private static let storageKey = "app.location.last"

    private static func restore() -> PrayerLocation? {
        guard let data = UserDefaults.standard.data(forKey: storageKey) else { return nil }
        return try? JSONDecoder().decode(PrayerLocation.self, from: data)
    }

    private func apply(_ value: PrayerLocation) {
        location = value
        if let data = try? JSONEncoder().encode(value) {
            UserDefaults.standard.set(data, forKey: Self.storageKey)
        }
    }

    /// Reuses an existing user grant without causing a permission prompt on app launch.
    func resolveIfAlreadyAuthorized() {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            resolveOnce()
        default:
            break
        }
    }

    func requestLocation() {
        errorMessage = nil
        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            resolveOnce()
        case .denied, .restricted:
            errorMessage = "location_unavailable"
        @unknown default:
            errorMessage = "location_unavailable"
        }
    }

    func useManualLocation(_ value: PrayerLocation) {
        manager.stopUpdatingLocation()
        geocoder.cancelGeocode()
        apply(value)
        isResolving = false
        errorMessage = nil
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            resolveOnce()
        case .denied, .restricted:
            isResolving = false
            errorMessage = "location_unavailable"
        case .notDetermined:
            break
        @unknown default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let latest = locations.last else {
            isResolving = false
            errorMessage = "location_unavailable"
            return
        }
        reverseGeocode(latest)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        isResolving = false
        // Keep whatever location is already showing; a transient fix failure should
        // not drop the user back to the picker.
        errorMessage = location == nil ? "location_unavailable" : nil
    }

    private func resolveOnce() {
        guard !isResolving else { return }
        isResolving = true
        manager.requestLocation()
    }

    private func reverseGeocode(_ value: CLLocation) {
        geocoder.reverseGeocodeLocation(value) { [weak self] placemarks, _ in
            guard let self else { return }
            Task { @MainActor in
                let placemark = placemarks?.first
                let zone = placemark?.timeZone ?? .current
                let countryCode = placemark?.isoCountryCode?.uppercased() ?? "ZZ"
                self.apply(
                    PrayerLocation(
                        latitude: value.coordinate.latitude,
                        longitude: value.coordinate.longitude,
                        timeZoneId: zone.identifier,
                        countryCode: countryCode,
                        cityName: placemark?.locality ?? placemark?.subAdministrativeArea,
                        regionName: placemark?.administrativeArea
                    )
                )
                self.isResolving = false
                self.errorMessage = nil
            }
        }
    }
}
