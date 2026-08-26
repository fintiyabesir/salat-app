package app.salat.repository

import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate
import platform.Foundation.NSUserDefaults
import kotlin.time.Instant

/**
 * Dependency-free iOS persistence for small official timetable horizons.
 * UserDefaults is appropriate here because entries are small scalar records rather than user documents.
 */
class IosPrayerCache(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : PrayerCache {

    override suspend fun official(
        sourceId: String,
        locationKey: String,
        date: LocalDate,
        now: Instant
    ): PrayerDay? {
        val raw = defaults.stringForKey(cacheKey(sourceId, locationKey, date)) ?: return null
        val fields = raw.split(SEPARATOR)
        if (fields.size < FIELD_COUNT_BASE) return null

        val refreshAfter = fields[8].toLongOrNull() ?: return null
        if (now.toEpochMilliseconds() >= refreshAfter) return null

        return runCatching {
            PrayerDay(
                date = date,
                fajr = Instant.fromEpochMilliseconds(fields[0].toLong()),
                sunrise = Instant.fromEpochMilliseconds(fields[1].toLong()),
                dhuhr = Instant.fromEpochMilliseconds(fields[2].toLong()),
                asr = Instant.fromEpochMilliseconds(fields[3].toLong()),
                maghrib = Instant.fromEpochMilliseconds(fields[4].toLong()),
                isha = Instant.fromEpochMilliseconds(fields[5].toLong()),
                calculationProfile = fields[6]
            )
        }.getOrNull()
    }

    override suspend fun putOfficial(
        sourceId: String,
        locationKey: String,
        days: List<PrayerDay>,
        fetchedAt: Instant,
        refreshAfter: Instant
    ) {
        days.forEach { day ->
            val encoded = listOf(
                day.fajr.toEpochMilliseconds(),
                day.sunrise.toEpochMilliseconds(),
                day.dhuhr.toEpochMilliseconds(),
                day.asr.toEpochMilliseconds(),
                day.maghrib.toEpochMilliseconds(),
                day.isha.toEpochMilliseconds(),
                day.calculationProfile,
                fetchedAt.toEpochMilliseconds(),
                refreshAfter.toEpochMilliseconds(),
                "",
                ""
            ).joinToString(SEPARATOR)
            defaults.setObject(encoded, forKey = cacheKey(sourceId, locationKey, day.date))
        }
    }

    override suspend fun recordDelta(
        sourceId: String,
        locationKey: String,
        date: LocalDate,
        calculationProfile: String,
        maxDeltaMinutes: Int
    ) {
        val key = cacheKey(sourceId, locationKey, date)
        val raw = defaults.stringForKey(key) ?: return
        val fields = raw.split(SEPARATOR).toMutableList()
        while (fields.size < FIELD_COUNT_WITH_COMPARISON) fields.add("")
        fields[9] = calculationProfile
        fields[10] = maxDeltaMinutes.toString()
        defaults.setObject(fields.joinToString(SEPARATOR), forKey = key)
    }

    private fun cacheKey(sourceId: String, locationKey: String, date: LocalDate): String =
        "salat.official.$sourceId.$locationKey.$date"

    private companion object {
        const val SEPARATOR = "|"
        const val FIELD_COUNT_BASE = 9
        const val FIELD_COUNT_WITH_COMPARISON = 11
    }
}
