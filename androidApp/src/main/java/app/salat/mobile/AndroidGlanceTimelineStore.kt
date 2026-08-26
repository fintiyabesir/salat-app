package app.salat.mobile

import android.content.Context
import app.salat.domain.SalatEngine
import app.salat.model.AppPreferences
import app.salat.model.GeoPoint
import app.salat.model.LocationSource
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

data class AndroidGlanceEvent(
    val prayer: PrayerName,
    val atMillis: Long
)

data class AndroidGlanceTimeline(
    val locationName: String,
    val timeZoneId: String,
    val events: List<AndroidGlanceEvent>
) {
    fun next(nowMillis: Long = System.currentTimeMillis()): AndroidGlanceEvent? =
        events.firstOrNull { it.atMillis > nowMillis }
}

/**
 * Device-local rolling timetable used by Android glance surfaces.
 *
 * The widget never needs network or background location: the phone app writes a
 * compact 30-day projection whenever location/calculation settings change, and
 * the widget only reads that persisted projection.
 */
class AndroidGlanceTimelineStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val engine = SalatEngine()

    fun rebuild(
        location: ResolvedLocation,
        settings: AppPreferences = AndroidAppSettingsStore(appContext).load(),
        horizonDays: Int = DEFAULT_HORIZON_DAYS
    ) {
        val zone = ZoneId.of(location.timeZoneId)
        val start = LocalDate.now(zone)
        val events = JSONArray()

        repeat(horizonDays.coerceIn(2, 45)) { offset ->
            val date = start.plusDays(offset.toLong())
            val day = engine.calculateDay(
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                latitude = location.point.latitude,
                longitude = location.point.longitude,
                timeZoneId = location.timeZoneId,
                countryCode = location.countryCode ?: "ZZ",
                preferences = settings.calculation
            )
            PrayerName.entries.forEach { prayer ->
                events.put(
                    JSONObject()
                        .put("prayer", prayer.name)
                        .put("at", day.time(prayer).toEpochMilliseconds())
                )
            }
        }

        preferences.edit()
            .putString(KEY_LOCATION, location.displayName)
            .putString(KEY_TIME_ZONE, location.timeZoneId)
            .putString(KEY_COUNTRY, location.countryCode)
            .putString(KEY_CITY, location.cityName)
            .putString(KEY_REGION, location.regionName)
            .putString(KEY_SOURCE, location.source.name)
            .putLong(KEY_LATITUDE_BITS, location.point.latitude.toBits())
            .putLong(KEY_LONGITUDE_BITS, location.point.longitude.toBits())
            .putString(KEY_EVENTS, events.toString())
            .apply()

        SalatAppWidgetProvider.refreshAll(appContext)
    }

    /** Recalculate the persisted projection after prayer calculation settings change. */
    fun rebuildLastKnown(settings: AppPreferences): Boolean {
        val location = lastKnownLocation() ?: return false
        rebuild(location = location, settings = settings)
        return true
    }

    fun load(): AndroidGlanceTimeline? {
        val location = preferences.getString(KEY_LOCATION, null) ?: return null
        val timeZone = preferences.getString(KEY_TIME_ZONE, null) ?: return null
        val raw = preferences.getString(KEY_EVENTS, null) ?: return null
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        val events = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val prayer = runCatching { PrayerName.valueOf(item.optString("prayer")) }.getOrNull() ?: continue
                val at = item.optLong("at", 0L)
                if (at > 0L) add(AndroidGlanceEvent(prayer, at))
            }
        }.sortedBy { it.atMillis }
        return AndroidGlanceTimeline(location, timeZone, events)
    }

    private fun lastKnownLocation(): ResolvedLocation? {
        if (!preferences.contains(KEY_LATITUDE_BITS) || !preferences.contains(KEY_LONGITUDE_BITS)) return null
        val latitude = Double.fromBits(preferences.getLong(KEY_LATITUDE_BITS, 0L))
        val longitude = Double.fromBits(preferences.getLong(KEY_LONGITUDE_BITS, 0L))
        val timeZone = preferences.getString(KEY_TIME_ZONE, null) ?: return null
        val source = preferences.getString(KEY_SOURCE, null)
            ?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }
            ?: LocationSource.DEVICE

        return runCatching {
            ResolvedLocation(
                point = GeoPoint(latitude, longitude),
                timeZoneId = timeZone,
                countryCode = preferences.getString(KEY_COUNTRY, null),
                cityName = preferences.getString(KEY_CITY, null),
                regionName = preferences.getString(KEY_REGION, null),
                source = source
            )
        }.getOrNull()
    }

    companion object {
        const val DEFAULT_HORIZON_DAYS = 30
        private const val PREFERENCES = "salat_glance_timeline"
        private const val KEY_LOCATION = "location"
        private const val KEY_TIME_ZONE = "time_zone"
        private const val KEY_COUNTRY = "country"
        private const val KEY_CITY = "city"
        private const val KEY_REGION = "region"
        private const val KEY_SOURCE = "source"
        private const val KEY_LATITUDE_BITS = "latitude_bits"
        private const val KEY_LONGITUDE_BITS = "longitude_bits"
        private const val KEY_EVENTS = "events"
    }
}
