package app.salat.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * We deliberately do not request background location. System clock/timezone/reboot
 * events only mark the rolling plan stale; the next normal location refresh rebuilds it.
 */
class PrayerScheduleLifecycleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in supportedActions) return
        PrayerScheduleReplanStore(context).mark(action)
    }

    companion object {
        private val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
