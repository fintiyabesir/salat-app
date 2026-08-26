package app.salat.repository

import app.salat.domain.PrayerCalculator
import app.salat.model.CalculationProfile
import app.salat.model.GeoPoint
import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import app.salat.model.VerificationState
import app.salat.verification.OfficialSourceAdapter
import app.salat.verification.RefreshPolicy
import app.salat.verification.SourcePreference
import app.salat.verification.VerificationRequest
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

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

        val cacheLocationKey = locationKey ?: coordinateKey(point)
        val now = Clock.System.now()
        val official = cache.official(adapter.metadata.id, cacheLocationKey, date, now) ?: runCatching {
            adapter.fetch(VerificationRequest(date..date, point, timeZoneId, locationKey))
        }.getOrNull()?.also { days ->
            cache.putOfficial(
                sourceId = adapter.metadata.id,
                locationKey = cacheLocationKey,
                days = days,
                fetchedAt = now,
                refreshAfter = RefreshSchedule.next(now, adapter.refreshPolicy())
            )
        }?.firstOrNull { it.date == date }

        if (official == null) return local.copy(verification = VerificationState.Unavailable(adapter.metadata.id))

        val maxDelta = PrayerComparator.maxDeltaMinutes(local, official)
        val state = if (maxDelta <= expectedDeltaMinutes) {
            VerificationState.Verified(adapter.metadata.id, now, maxDelta)
        } else {
            VerificationState.Different(adapter.metadata.id, maxDelta)
        }

        return when (adapter.metadata.preference) {
            SourcePreference.PREFER_OFFICIAL -> official.copy(verification = state)
            SourcePreference.COMPARE_ONLY -> local.copy(verification = state)
        }
    }

    private fun coordinateKey(point: GeoPoint): String =
        "${quantize(point.latitude)},${quantize(point.longitude)}"

    private fun quantize(value: Double): Double = (value * 10_000.0).toInt() / 10_000.0
}

object RefreshSchedule {
    fun next(now: Instant, policy: RefreshPolicy): Instant = when (policy) {
        RefreshPolicy.Annual -> now + 365.days
        RefreshPolicy.Monthly -> now + 30.days
        RefreshPolicy.Daily -> now + 24.hours
        is RefreshPolicy.FixedHours -> now + policy.hours.hours
    }
}

object PrayerComparator {
    fun maxDeltaMinutes(a: PrayerDay, b: PrayerDay): Int = PrayerName.entries.maxOf { prayer ->
        abs((a.time(prayer) - b.time(prayer)).inWholeMinutes.toInt())
    }
}

data class PrayerCacheKey(
    val sourceId: String,
    val locationKey: String,
    val date: LocalDate
)

data class CachedPrayerDay(
    val day: PrayerDay,
    val fetchedAt: Instant,
    val refreshAfter: Instant
)

interface PrayerCache {
    suspend fun official(sourceId: String, locationKey: String, date: LocalDate, now: Instant): PrayerDay?
    suspend fun putOfficial(
        sourceId: String,
        locationKey: String,
        days: List<PrayerDay>,
        fetchedAt: Instant,
        refreshAfter: Instant
    )
}

class InMemoryPrayerCache : PrayerCache {
    private val data = mutableMapOf<PrayerCacheKey, CachedPrayerDay>()

    override suspend fun official(sourceId: String, locationKey: String, date: LocalDate, now: Instant): PrayerDay? {
        val cached = data[PrayerCacheKey(sourceId, locationKey, date)] ?: return null
        return cached.day.takeIf { now < cached.refreshAfter }
    }

    override suspend fun putOfficial(
        sourceId: String,
        locationKey: String,
        days: List<PrayerDay>,
        fetchedAt: Instant,
        refreshAfter: Instant
    ) {
        days.forEach { day ->
            data[PrayerCacheKey(sourceId, locationKey, day.date)] = CachedPrayerDay(day, fetchedAt, refreshAfter)
        }
    }
}
