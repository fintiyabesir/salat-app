package app.salat.verification

import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MuisOpenDataAdapter(
    private val http: TextHttpClient
) : OfficialSourceAdapter {
    override val metadata = SourceMetadata(
        id = "muis-open-data",
        authorityName = "Majlis Ugama Islam Singapura (MUIS)",
        sourceUrl = DATASET_URL,
        attribution = "Contains information from the MUIS Muslim Prayer Timetable (consolidated) on data.gov.sg, made available under the Singapore Open Data Licence v1.0.",
        preference = SourcePreference.PREFER_OFFICIAL,
        runtimeUse = RuntimeUsePolicy.ENABLED,
        usage = SourceUsagePolicy(
            termsUrl = "https://data.gov.sg/open-data-licence",
            licenseName = "Singapore Open Data Licence v1.0",
            commercialUse = UsagePermission.ALLOWED,
            redistribution = UsagePermission.ALLOWED,
            caching = UsagePermission.ALLOWED,
            attributionRequired = true,
            publishedRateLimit = "4 Datastore Search calls per 10 seconds without an API key",
            credentials = CredentialPolicy.OPTIONAL_SERVER_SIDE,
            reviewedOn = "2026-08-29"
        )
    )

    override fun supports(countryCode: String, regionCode: String?): Boolean =
        countryCode.equals("SG", ignoreCase = true)

    override fun refreshPolicy(): RefreshPolicy = RefreshPolicy.Annual

    override suspend fun fetch(request: VerificationRequest): List<PrayerDay> {
        val payload = http.get(DATASTORE_URL)
        return MuisOpenDataParser.parse(payload, request.timeZoneId)
    }

    companion object {
        const val DATASET_ID = "d_a6a206cba471fe04b62dd886ef5eaf22"
        const val DATASET_URL = "https://data.gov.sg/datasets/$DATASET_ID/view"
        const val DATASTORE_URL =
            "https://data.gov.sg/api/action/datastore_search?resource_id=$DATASET_ID&limit=5000"
    }
}

object MuisOpenDataParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(payload: String, timeZoneId: String = "Asia/Singapore"): List<PrayerDay> {
        val root = json.parseToJsonElement(payload).jsonObject
        require(root.getValue("success").jsonPrimitive.boolean) { "MUIS data.gov.sg response was unsuccessful" }
        val result = root.getValue("result").jsonObject
        val records = result.getValue("records").jsonArray
        val total = result.getValue("total").jsonPrimitive.int
        require(records.size == total) {
            "MUIS data.gov.sg response was partial: received ${records.size} of $total records"
        }
        require(records.isNotEmpty()) { "MUIS data.gov.sg response contained no timetable records" }
        val zone = TimeZone.of(timeZoneId)
        return records.map { element ->
            val record = element.jsonObject
            val date = LocalDate.parse(record.value("Date"))
            fun instant(field: String, afternoon: Boolean = false) = LocalDateTime(
                date,
                parseTime(record.value(field), afternoon)
            ).toInstant(zone)

            PrayerDay(
                date = date,
                fajr = instant("Subuh"),
                sunrise = instant("Syuruk"),
                dhuhr = instant("Zohor", afternoon = true),
                asr = instant("Asar", afternoon = true),
                maghrib = instant("Maghrib", afternoon = true),
                isha = instant("Isyak", afternoon = true),
                calculationProfile = "official:muis-open-data"
            )
        }.sortedBy { it.date }
    }

    private fun Map<String, JsonElement>.value(name: String): String =
        getValue(name).jsonPrimitive.content.trim()

    private fun parseTime(raw: String, afternoon: Boolean): LocalTime {
        val parts = raw.split(':')
        require(parts.size in 2..3) { "Unexpected MUIS time: $raw" }
        var hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val second = parts.getOrNull(2)?.toInt() ?: 0
        if (afternoon && hour in 1..11) hour += 12
        return LocalTime(hour, minute, second)
    }
}
