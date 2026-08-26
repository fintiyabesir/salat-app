package app.salat.location

import app.salat.model.GeoPoint
import app.salat.model.LocationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManualCityCatalogTest {
    private val catalog = InMemoryManualCityCatalog(
        listOf(
            ManualCity("tr-ist", "Istanbul", "TR", "Türkiye", GeoPoint(41.0082, 28.9784), "Europe/Istanbul", "Istanbul"),
            ManualCity("my-kul", "Kuala Lumpur", "MY", "Malaysia", GeoPoint(3.1390, 101.6869), "Asia/Kuala_Lumpur")
        )
    )

    @Test fun searches_city_or_country_without_network() {
        assertEquals("my-kul", catalog.search("malay").single().id)
        assertEquals("tr-ist", catalog.search("istan").single().id)
    }

    @Test fun blank_search_lists_bundled_cities_for_initial_picker_state() {
        assertEquals(listOf("tr-ist", "my-kul"), catalog.search("").map { it.id })
        assertEquals(listOf("tr-ist"), catalog.search("", limit = 1).map { it.id })
    }

    @Test fun converts_manual_city_to_resolved_location() {
        val location = catalog.byId("tr-ist")!!.asResolvedLocation()
        assertEquals(LocationSource.MANUAL_CITY, location.source)
        assertEquals("Europe/Istanbul", location.timeZoneId)
        assertTrue(location.displayName.contains("Istanbul"))
    }
}
