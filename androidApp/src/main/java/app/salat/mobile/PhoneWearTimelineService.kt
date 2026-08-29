package app.salat.mobile

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class PhoneWearTimelineService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != AndroidGlanceTimelineStore.TIMELINE_REQUEST_PATH) return
        val payload = AndroidGlanceTimelineStore(this).payloadForWear() ?: return
        Wearable.getMessageClient(this).sendMessage(
            messageEvent.sourceNodeId,
            AndroidGlanceTimelineStore.TIMELINE_MESSAGE_PATH,
            payload
        )
    }
}
