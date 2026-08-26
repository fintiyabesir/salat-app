package app.salat.mobile

import android.content.Context

class PrayerScheduleReplanStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mark(reason: String) {
        prefs.edit()
            .putBoolean(KEY_STALE, true)
            .putString(KEY_REASON, reason)
            .putLong(KEY_MARKED_AT, System.currentTimeMillis())
            .apply()
    }

    fun isStale(): Boolean = prefs.getBoolean(KEY_STALE, false)

    fun reason(): String? = prefs.getString(KEY_REASON, null)

    fun clear() {
        prefs.edit().remove(KEY_STALE).remove(KEY_REASON).remove(KEY_MARKED_AT).apply()
    }

    companion object {
        private const val PREFS = "salat-prayer-replan-v1"
        private const val KEY_STALE = "stale"
        private const val KEY_REASON = "reason"
        private const val KEY_MARKED_AT = "markedAt"
    }
}
