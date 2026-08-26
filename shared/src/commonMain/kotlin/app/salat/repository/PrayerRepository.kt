package app.salat.repository

import app.salat.domain.PrayerCalculator
import app.salat.model.CalculationProfile
import app.salat.model.GeoPoint
import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import app.salat.model.VerificationState
import app.salat.verification.OfficialSourceAdapter
import app.salat.verification.SourcePreference
import app.salat.verification.VerificationRequest
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.time.Clock

class PrayerRepository(
    private val calculator: PrayerCalculator,
    private val adapters: List<OfficialSourceAdapter>,
    private val cache: PrayerCache,
    private val expectedDeltaMinutes: Int = 2
) {
    suspend fun load(
        date: LocalDate,
        point: GeoPoint,
        timeZoneId: String,
        countryCode: String,
        regionCode: String?,
        locationKey: String?,
        profile: CalculationProfile
    ): PrayerDay {
        val local = calculator.calculate(date, point, timeZoneId, profile)
        val adapter = adapters.firstOrNull { it.supports(countryCode, regionCode) }
            ?: return local.copy(verification = VerificationState.Unavailable(null))

        val official = cache.official(adapter.metadata.id, date) ?: runCatching {
            adapter.fetch(VerificationRequest(date..date, point, timeZoneId, locationKey)).firstOrNull()
        }.getOrNull()?.also { cache.putOfficial(adapter.metadata.id, it) }

        if (official == null) return local.copy(verification = VerificationState.Unavailable(adapter.metadata.id))

        val maxDelta = PrayerComparator.maxDeltaMinutes(local, official)
        val state = if (maxDelta <= expectedDeltaMinutes) {
            VerificationState.Verified(adapter.metadata.id, Clock.System.now(), maxDelta)
        } else {
            VerificationState.Different(adapter.metadata.id, maxDelta)
        }

        return when (adapter.metadata.preference) {
            SourcePreference.PREFER_OFFICIAL -> official.copy(verification = state)
            SourcePreference.COMPARE_ONLY -> local.copy(verification = state)
        }
    }
}

object PrayerComparator {
    fun maxDeltaMinutes(a: PrayerDay, b: PrayerDay): Int = PrayerName.entries.maxOf { prayer ->
        abs((a.time(prayer) - b.time(prayer)).inWholeMinutes.toInt())
    }
}

interface PrayerCache {
    suspend fun official(sourceId: String, date: LocalDate): PrayerDay?
    suspend fun putOfficial(sourceId: String, day: PrayerDay)
}

class InMemoryPrayerCache : PrayerCache {
    private val data = mutableMapOf<Pair<String, LocalDate>, PrayerDay>()
    override suspend fun official(sourceId: String, date: LocalDate): PrayerDay? = data[sourceId to date]
    override suspend fun putOfficial(sourceId: String, day: PrayerDay) {
        data[sourceId to day.date] = day
    }
}
