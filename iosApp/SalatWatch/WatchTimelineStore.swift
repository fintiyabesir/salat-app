import Combine
import Foundation
import WatchConnectivity
import WidgetKit

final class WatchTimelineStore: NSObject, ObservableObject, WCSessionDelegate {
    @Published private(set) var payload: GlanceTimelinePayload? =
        GlanceTimelinePersistence.load(appGroup: GlanceTimelinePersistence.watchAppGroup)

    private let session: WCSession?

    override init() {
        if WCSession.isSupported() {
            session = .default
        } else {
            session = nil
        }
        super.init()
        session?.delegate = self
        session?.activate()
        if let context = session?.applicationContext, !context.isEmpty {
            ingest(context)
        }
    }

    private func ingest(_ context: [String: Any]) {
        guard let data = context[GlanceTimelinePersistence.storageKey] as? Data,
              let decoded = try? JSONDecoder().decode(GlanceTimelinePayload.self, from: data) else {
            return
        }

        DispatchQueue.main.async {
            self.payload = decoded
            if GlanceTimelinePersistence.save(
                decoded,
                appGroup: GlanceTimelinePersistence.watchAppGroup
            ) {
                WidgetCenter.shared.reloadAllTimelines()
            }
        }
    }

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        if activationState == .activated, !session.applicationContext.isEmpty {
            ingest(session.applicationContext)
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        ingest(applicationContext)
    }
}
