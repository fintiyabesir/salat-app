package app.salat.verification

import app.salat.model.GeoPoint
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MuisOpenDataAdapterTest {
    @Test
    fun parses_legacy_12_hour_and_current_24_hour_records() {
        val days = MuisOpenDataParser.parse(FIXTURE)
        val zone = TimeZone.of("Asia/Singapore")

        assertEquals(listOf("2025-12-31", "2026-01-01"), days.map { it.date.toString() })
        assertEquals(13, days[0].dhuhr.toLocalDateTime(zone).hour)
        assertEquals(16, days[0].asr.toLocalDateTime(zone).hour)
        assertEquals(19, days[0].maghrib.toLocalDateTime(zone).hour)
        assertEquals(20, days[0].isha.toLocalDateTime(zone).hour)
        assertEquals(13, days[1].dhuhr.toLocalDateTime(zone).hour)
        assertEquals("official:muis-open-data", days[1].calculationProfile)
    }

    @Test
    fun fetches_the_bulk_dataset_for_annual_cache_and_declares_open_data_policy() = runImmediateSuspend {
        var requestedUrl: String? = null
        val adapter = MuisOpenDataAdapter(TextHttpClient { url ->
            requestedUrl = url
            FIXTURE
        })

        val days = adapter.fetch(
            VerificationRequest(
                range = LocalDate(2026, 1, 1)..LocalDate(2026, 1, 1),
                point = GeoPoint(1.3521, 103.8198),
                timeZoneId = "Asia/Singapore"
            )
        )

        assertEquals(2, days.size)
        assertEquals(MuisOpenDataAdapter.DATASTORE_URL, requestedUrl)
        assertEquals(RuntimeUsePolicy.ENABLED, adapter.metadata.runtimeUse)
        assertEquals(UsagePermission.ALLOWED, adapter.metadata.usage.commercialUse)
        assertEquals(UsagePermission.ALLOWED, adapter.metadata.usage.caching)
        assertTrue(adapter.metadata.attribution.contains("Singapore Open Data Licence"))
        assertEquals(RefreshPolicy.Annual, adapter.refreshPolicy())
    }

    @Test
    fun rejects_partial_bulk_response_so_repository_can_fall_back_locally() {
        val partial = FIXTURE.replace("\"total\": 2", "\"total\": 3")
        assertFailsWith<IllegalArgumentException> {
            MuisOpenDataParser.parse(partial)
        }
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
        private val FIXTURE = """
            {
              "success": true,
              "result": {
                "records": [
                  {"_id": 2, "Date": "2026-01-01", "Day": "Thu", "Subuh": "05:45", "Syuruk": "07:08", "Zohor": "13:10", "Asar": "16:34", "Maghrib": "19:11", "Isyak": "20:26"},
                  {"_id": 1, "Date": "2025-12-31", "Day": "Wed", "Subuh": "05:44", "Syuruk": "07:07", "Zohor": "01:10", "Asar": "04:34", "Maghrib": "07:10", "Isyak": "08:25"}
                ],
                "total": 2,
                "limit": 5000
              }
            }
        """.trimIndent()
    }
}
