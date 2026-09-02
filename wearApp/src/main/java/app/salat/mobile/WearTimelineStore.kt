package app.salat.mobile

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import app.salat.domain.DayStatus
import app.salat.domain.DayStatusCalculator
import app.salat.domain.DayTimes

data class WearPrayerEvent(
    val prayerId: String,
    val atMillis: Long
)

data class WearPrayerTimeline(
    val generatedAtMillis: Long,
    val locationName: String,
    val timeZoneId: String,
    val events: List<WearPrayerEvent>,
    /** Null when the phone has kerahat turned off. */
    val kerahatMinutes: Int? = null
) {
    /**
     * The same reading the phone shows. Built from the projection rather than
     * recalculated, so the watch cannot drift from the handset in its pocket.
     */
    fun status(nowMillis: Long = System.currentTimeMillis()): DayStatus? {
        val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        fun times(date: java.time.LocalDate): DayTimes? {
            val ofDay = events
                .filter { Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == date }
                .associateBy { it.prayerId.uppercase() }
            val ids = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
            if (!ids.all { ofDay.containsKey(it) }) return null
            return DayTimes(
                fajr = ofDay.getValue("FAJR").atMillis,
                sunrise = ofDay.getValue("SUNRISE").atMillis,
                dhuhr = ofDay.getValue("DHUHR").atMillis,
                asr = ofDay.getValue("ASR").atMillis,
                maghrib = ofDay.getValue("MAGHRIB").atMillis,
                isha = ofDay.getValue("ISHA").atMillis
            )
        }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val todayTimes = times(today) ?: return null
        return DayStatusCalculator.evaluate(
            nowMillis = nowMillis,
            today = todayTimes,
            yesterday = times(today.minusDays(1)) ?: todayTimes,
            tomorrow = times(today.plusDays(1)) ?: todayTimes,
            kerahatMinutes = kerahatMinutes
        )
    }

    fun next(nowMillis: Long = System.currentTimeMillis()): WearPrayerEvent? =
        events.firstOrNull { it.atMillis > nowMillis }

    fun eventsOn(dayMillis: Long = System.currentTimeMillis()): List<WearPrayerEvent> {
        val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val day = Instant.ofEpochMilli(dayMillis).atZone(zone).toLocalDate()
        return events.filter {
            Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == day
        }
    }
}

class WearTimelineStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    fun save(raw: ByteArray): Boolean {
        val encoded = raw.toString(Charsets.UTF_8)
        if (decodeWearTimeline(encoded) == null) return false
        preferences.edit().putString(KEY_PAYLOAD, encoded).apply()
        return true
    }

    fun load(): WearPrayerTimeline? =
        preferences.getString(KEY_PAYLOAD, null)?.let(::decodeWearTimeline)

    companion object {
        const val TIMELINE_MESSAGE_PATH = "/salat/glance/timeline/v1"
        const val TIMELINE_REQUEST_PATH = "/salat/glance/request/v1"
        private const val PREFERENCES = "salat_wear_timeline"
        private const val KEY_PAYLOAD = "timeline_payload"
    }
}

internal fun decodeWearTimeline(raw: String): WearPrayerTimeline? = runCatching {
    val root = JSONObject(raw)
    require(root.optInt("version") == 1)
    val timeZoneId = root.getString("timeZoneId")
    ZoneId.of(timeZoneId)
    val eventsJson = root.getJSONArray("events")
    val events = buildList {
        for (index in 0 until eventsJson.length()) {
            val item = eventsJson.getJSONObject(index)
            val prayerId = item.getString("prayer")
            val atMillis = item.getLong("at")
            if (prayerId.isNotBlank() && atMillis > 0L) {
                add(WearPrayerEvent(prayerId, atMillis))
            }
        }
    }.sortedBy { it.atMillis }
    require(events.isNotEmpty())
    WearPrayerTimeline(
        generatedAtMillis = root.getLong("generatedAt"),
        locationName = root.getString("locationName"),
        timeZoneId = timeZoneId,
        events = events,
        kerahatMinutes = root.optInt("kerahatMinutes", 0).takeIf { it > 0 }
    )
}.getOrNull()

fun WearPrayerEvent.localizedName(context: Context): String = context.getString(
    when (prayerId.uppercase()) {
        "FAJR" -> R.string.prayer_fajr
        "SUNRISE" -> R.string.prayer_sunrise
        "DHUHR" -> R.string.prayer_dhuhr
        "ASR" -> R.string.prayer_asr
        "MAGHRIB" -> R.string.prayer_maghrib
        "ISHA" -> R.string.prayer_isha
        else -> return prayerId
    }
)
