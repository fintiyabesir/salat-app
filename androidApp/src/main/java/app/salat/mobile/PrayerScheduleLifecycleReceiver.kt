package app.salat.mobile

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Rebuilds the alert plan after events that invalidate it.
 *
 * AlarmManager drops every pending alarm on reboot. Previously this receiver only
 * marked the plan stale and waited for the app to be opened, so prayer notifications
 * silently stopped after every restart. It now rebuilds from the remembered location,
 * and only falls back to the stale flag when no location has been stored yet.
 */
class PrayerScheduleLifecycleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in supportedActions) return

        val pending = goAsync()
        try {
            if (!AndroidPrayerNotificationCoordinator(context).rebuildFromStoredLocation()) {
                PrayerScheduleReplanStore(context).mark(action)
            }
        } finally {
            pending.finish()
        }
    }

    companion object {
        private val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            AndroidPrayerNotificationScheduler.ACTION_REPLAN
        )
    }
}
