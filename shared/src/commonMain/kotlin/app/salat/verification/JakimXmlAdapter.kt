package app.salat.verification

import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class JakimXmlAdapter(
    private val http: TextHttpClient
) : OfficialSourceAdapter {
    override val metadata = SourceMetadata(
        id = "jakim-esolat",
        authorityName = "Jabatan Kemajuan Islam Malaysia (JAKIM)",
        sourceUrl = "https://www.e-solat.gov.my/",
        attribution = "Prayer timetable verification: JAKIM e-Solat",
        preference = SourcePreference.PREFER_OFFICIAL
    )

    override fun supports(countryCode: String, regionCode: String?): Boolean =
        countryCode.equals("MY", ignoreCase = true)

    override fun refreshPolicy(): RefreshPolicy = RefreshPolicy.Daily

    override suspend fun fetch(request: VerificationRequest): List<PrayerDay> {
        val zone = requireNotNull(request.locationKey) { "JAKIM zone code is required (for example SGR01)" }
        val xml = http.get("https://www.e-solat.gov.my/index.php?r=esolatApi/xmlfeed&zon=$zone")
        val day = JakimXmlParser.parse(xml, request.timeZoneId)
        return if (day.date in request.range) listOf(day) else emptyList()
    }
}

object JakimXmlParser {
    fun parse(xml: String, timeZoneId: String = "Asia/Kuala_Lumpur"): PrayerDay {
        val date = parseDate(extractTag(xml, "dc:date"))
        val times = Regex("<item>\\s*<title>([^<]+)</title>\\s*<description>([^<]+)</description>\\s*</item>", RegexOption.IGNORE_CASE)
            .findAll(xml)
            .associate { it.groupValues[1].trim().lowercase() to it.groupValues[2].trim() }

        fun instant(label: String) = LocalDateTime(date, parseTime(requireNotNull(times[label]) { "Missing JAKIM time: $label" }))
            .toInstant(TimeZone.of(timeZoneId))

        return PrayerDay(
            date = date,
            fajr = instant("subuh"),
            sunrise = instant("syuruk"),
            dhuhr = instant("zohor"),
            asr = instant("asar"),
            maghrib = instant("maghrib"),
            isha = instant("isyak"),
            calculationProfile = "official:jakim-esolat"
        )
    }

    private fun extractTag(xml: String, tag: String): String =
        Regex("<$tag>([^<]+)</$tag>", RegexOption.IGNORE_CASE).find(xml)?.groupValues?.get(1)?.trim()
            ?: error("Missing XML tag <$tag>")

    private fun parseDate(raw: String): LocalDate {
        val datePart = raw.substringBefore(' ')
        val p = datePart.split('-')
        require(p.size == 3) { "Unexpected JAKIM date: $raw" }
        return LocalDate(p[2].toInt(), p[1].toInt(), p[0].toInt())
    }

    private fun parseTime(raw: String): LocalTime {
        val p = raw.split(':')
        require(p.size >= 2) { "Unexpected JAKIM time: $raw" }
        return LocalTime(p[0].toInt(), p[1].toInt(), p.getOrNull(2)?.toInt() ?: 0)
    }
}
