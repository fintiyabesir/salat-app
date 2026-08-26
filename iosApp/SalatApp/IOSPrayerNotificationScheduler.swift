import Foundation
import UserNotifications

enum IOSNotificationSoundMode: String {
    case silent
    case system
    case shortAdhan
}

struct IOSPrayerAlert: Identifiable {
    let id: String
    let prayerName: String
    let prayerAt: Date
    let triggerAt: Date
    let soundMode: IOSNotificationSoundMode
}

/// Native iOS delivery for prayer alerts. This type never asks for notification
/// permission unless the UI explicitly calls requestAuthorizationAfterUserOptIn().
///
/// Custom notification sounds on iOS must be short (under 30 seconds). Salat only
/// uses `adhan_short.caf` when a properly licensed asset is bundled; otherwise the
/// system notification sound is used.
final class IOSPrayerNotificationScheduler {
    private let center: UNUserNotificationCenter
    private let identifierPrefix = "salat.prayer."

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    func authorizationStatus(completion: @escaping (UNAuthorizationStatus) -> Void) {
        center.getNotificationSettings { settings in
            completion(settings.authorizationStatus)
        }
    }

    /// Call only as the direct result of the user enabling at least one prayer alert.
    func requestAuthorizationAfterUserOptIn(completion: @escaping (Bool, Error?) -> Void) {
        center.requestAuthorization(options: [.alert, .sound, .badge], completionHandler: completion)
    }

    /// Replaces every Salat-owned pending prayer notification with a fresh plan.
    /// Invoke after location, timezone, calculation-profile or alert-setting changes.
    func replaceAll(with alerts: [IOSPrayerAlert], completion: ((Error?) -> Void)? = nil) {
        center.getPendingNotificationRequests { [weak self] pending in
            guard let self else { return }
            let owned = pending.map(\.identifier).filter { $0.hasPrefix(self.identifierPrefix) }
            if !owned.isEmpty {
                self.center.removePendingNotificationRequests(withIdentifiers: owned)
            }

            let future = alerts.filter { $0.triggerAt > Date() }
            guard !future.isEmpty else {
                completion?(nil)
                return
            }

            let group = DispatchGroup()
            let lock = NSLock()
            var firstError: Error?

            for alert in future {
                group.enter()
                self.add(alert) { error in
                    if let error {
                        lock.lock()
                        if firstError == nil { firstError = error }
                        lock.unlock()
                    }
                    group.leave()
                }
            }

            group.notify(queue: .main) { completion?(firstError) }
        }
    }

    func removeAllPrayerAlerts() {
        center.getPendingNotificationRequests { [weak self] pending in
            guard let self else { return }
            let owned = pending.map(\.identifier).filter { $0.hasPrefix(self.identifierPrefix) }
            self.center.removePendingNotificationRequests(withIdentifiers: owned)
        }
    }

    private func add(_ alert: IOSPrayerAlert, completion: @escaping (Error?) -> Void) {
        let content = UNMutableNotificationContent()
        content.title = alert.prayerName

        let minutesBefore = max(0, Int(alert.prayerAt.timeIntervalSince(alert.triggerAt) / 60.0))
        content.body = minutesBefore > 0
            ? L10n.format("prayer_in_minutes", alert.prayerName, minutesBefore)
            : L10n.format("prayer_time_now", alert.prayerName)
        content.sound = sound(for: alert.soundMode)
        content.userInfo = [
            "prayer": alert.prayerName,
            "prayerAt": alert.prayerAt.timeIntervalSince1970,
            "stableId": alert.id
        ]

        let interval = max(1.0, alert.triggerAt.timeIntervalSinceNow)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let request = UNNotificationRequest(
            identifier: identifierPrefix + alert.id,
            content: content,
            trigger: trigger
        )
        center.add(request, withCompletionHandler: completion)
    }

    private func sound(for mode: IOSNotificationSoundMode) -> UNNotificationSound? {
        switch mode {
        case .silent:
            return nil
        case .system:
            return .default
        case .shortAdhan:
            guard Bundle.main.url(forResource: "adhan_short", withExtension: "caf") != nil else {
                return .default
            }
            return UNNotificationSound(named: UNNotificationSoundName(rawValue: "adhan_short.caf"))
        }
    }
}
