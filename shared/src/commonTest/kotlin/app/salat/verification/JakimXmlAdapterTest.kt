package app.salat.verification

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class JakimXmlAdapterTest {
    @Test
    fun parses_official_esolat_rss_shape() {
        val xml = """
            <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <channel>
                <dc:date>25-08-2026 06:02:31</dc:date>
                <item><title>Imsak</title><description>05:51:00</description></item>
                <item><title>Subuh</title><description>06:01:00</description></item>
                <item><title>Syuruk</title><description>07:08:00</description></item>
                <item><title>Dhuha</title><description>07:33:00</description></item>
                <item><title>Zohor</title><description>13:18:00</description></item>
                <item><title>Asar</title><description>16:30:00</description></item>
                <item><title>Maghrib</title><description>19:23:00</description></item>
                <item><title>Isyak</title><description>20:34:00</description></item>
              </channel>
            </rss>
        """.trimIndent()
        val day = JakimXmlParser.parse(xml)
        val zone = TimeZone.of("Asia/Kuala_Lumpur")
        assertEquals("2026-08-25", day.date.toString())
        assertEquals(6, day.fajr.toLocalDateTime(zone).hour)
        assertEquals(1, day.fajr.toLocalDateTime(zone).minute)
        assertEquals(19, day.maghrib.toLocalDateTime(zone).hour)
        assertEquals(23, day.maghrib.toLocalDateTime(zone).minute)
    }
}
