import Foundation
import WatchConnectivity

final class IOSWatchTimelineBridge: NSObject, WCSessionDelegate {
    static let shared = IOSWatchTimelineBridge()

    private let session: WCSession?
    private var pendingPayload: GlanceTimelinePayload?

    private override init() {
        if WCSession.isSupported() {
            session = .default
        } else {
            session = nil
        }
        super.init()
        session?.delegate = self
        session?.activate()
    }

    func publish(_ payload: GlanceTimelinePayload) {
        pendingPayload = payload
        publishPendingIfPossible()
    }

    private func publishPendingIfPossible() {
        guard let session,
              session.activationState == .activated,
              session.isWatchAppInstalled,
              let payload = pendingPayload,
              let data = try? JSONEncoder().encode(payload) else {
            return
        }

        do {
            try session.updateApplicationContext([GlanceTimelinePersistence.storageKey: data])
            pendingPayload = nil
        } catch {
            // Keep the latest payload pending; the next session activation or rebuild retries it.
        }
    }

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        if activationState == .activated {
            publishPendingIfPossible()
        }
    }

    /// Fires when the watch app is installed or the paired watch changes. Without this,
    /// a timeline that failed with WCErrorCodeWatchAppNotInstalled stays pending until
    /// something else calls rebuild(), so installing the watch app after choosing a
    /// location leaves the watch empty.
    func sessionWatchStateDidChange(_ session: WCSession) {
        publishPendingIfPossible()
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}
