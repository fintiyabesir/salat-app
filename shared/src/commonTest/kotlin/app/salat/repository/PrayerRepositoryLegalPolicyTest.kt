package app.salat.repository

import app.salat.domain.PrayerCalculator
import app.salat.model.CalculationMethodId
import app.salat.model.CalculationProfile
import app.salat.model.GeoPoint
import app.salat.model.PrayerDay
import app.salat.model.VerificationState
import app.salat.verification.CredentialPolicy
import app.salat.verification.OfficialSourceAdapter
import app.salat.verification.RefreshPolicy
import app.salat.verification.RuntimeUsePolicy
import app.salat.verification.SourceMetadata
import app.salat.verification.SourcePreference
import app.salat.verification.SourceUsagePolicy
import app.salat.verification.UsagePermission
import app.salat.verification.VerificationRequest
import kotlinx.datetime.LocalDate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class PrayerRepositoryLegalPolicyTest {
    @Test
    fun permission_required_adapter_is_never_called_and_local_result_continues() = runImmediateSuspend {
        var adapterCalled = false
        val localDay = day("local")
        val adapter = object : OfficialSourceAdapter {
            override val metadata = SourceMetadata(
                id = "blocked-source",
                authorityName = "Blocked source",
                sourceUrl = "https://example.invalid/source",
                attribution = "Blocked source",
                preference = SourcePreference.PREFER_OFFICIAL,
                runtimeUse = RuntimeUsePolicy.PERMISSION_REQUIRED,
                usage = SourceUsagePolicy(
                    termsUrl = "https://example.invalid/terms",
                    licenseName = "Permission required",
                    commercialUse = UsagePermission.PERMISSION_REQUIRED,
                    redistribution = UsagePermission.PERMISSION_REQUIRED,
                    caching = UsagePermission.PERMISSION_REQUIRED,
                    attributionRequired = true,
                    publishedRateLimit = null,
                    credentials = CredentialPolicy.NONE,
                    reviewedOn = "2026-08-29"
                )
            )

            override fun supports(countryCode: String, regionCode: String?) = countryCode == "MY"
            override fun refreshPolicy() = RefreshPolicy.Daily
            override suspend fun fetch(request: VerificationRequest): List<PrayerDay> {
                adapterCalled = true
                return listOf(day("official"))
            }
        }
        val repository = PrayerRepository(
            calculator = object : PrayerCalculator {
                override fun calculate(
                    date: LocalDate,
                    point: GeoPoint,
                    timeZoneId: String,
                    profile: CalculationProfile
                ) = localDay
            },
            adapters = listOf(adapter),
            cache = InMemoryPrayerCache()
        )

        val result = repository.load(
            date = DATE,
            point = GeoPoint(3.139, 101.6869),
            timeZoneId = "Asia/Kuala_Lumpur",
            countryCode = "MY",
            regionCode = null,
            locationKey = "SGR01",
            profile = CalculationProfile("malaysia", CalculationMethodId.MALAYSIA)
        )

        assertFalse(adapterCalled)
        assertEquals("local", result.calculationProfile)
        assertEquals(VerificationState.Unavailable(null), result.verification)
    }

    private fun day(profile: String): PrayerDay {
        val instant = Instant.parse("2026-08-29T00:00:00Z")
        return PrayerDay(DATE, instant, instant, instant, instant, instant, instant, profile)
    }

    private fun <T> runImmediateSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome = result }
        })
        return requireNotNull(outcome) { "Test helper only supports immediately completing suspend functions" }.getOrThrow()
    }

    companion object {
        private val DATE = LocalDate(2026, 8, 29)
    }
}
