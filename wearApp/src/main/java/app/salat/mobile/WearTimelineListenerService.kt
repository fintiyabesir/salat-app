package app.salat.mobile

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearTimelineListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearTimelineStore.TIMELINE_MESSAGE_PATH) {
            WearTimelineStore(this).save(messageEvent.data)
        }
    }
}
