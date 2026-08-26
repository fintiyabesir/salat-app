package app.salat.location

import app.salat.model.GeoPoint
import app.salat.model.LocationSource
import app.salat.model.ResolvedLocation

data class ManualCity(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val point: GeoPoint,
    val timeZoneId: String,
    val regionName: String? = null
) {
    fun asResolvedLocation(): ResolvedLocation = ResolvedLocation(
        point = point,
        timeZoneId = timeZoneId,
        countryCode = countryCode,
        cityName = name,
        regionName = regionName,
        source = LocationSource.MANUAL_CITY
    )
}

/**
 * Platform apps can back this with a compressed bundled dataset. Search must not
 * require a Salat server; the first production dataset candidate is a reduced
 * GeoNames export after attribution and app-size review.
 */
interface ManualCityCatalog {
    fun search(query: String, limit: Int = 30): List<ManualCity>
    fun byId(id: String): ManualCity?
}

class InMemoryManualCityCatalog(cities: List<ManualCity>) : ManualCityCatalog {
    private val all = cities.toList()
    private val byId = all.associateBy { it.id }

    override fun search(query: String, limit: Int): List<ManualCity> {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) return emptyList()
        return all.asSequence()
            .filter { city ->
                city.name.lowercase().contains(needle) ||
                    city.countryName.lowercase().contains(needle) ||
                    city.regionName?.lowercase()?.contains(needle) == true
            }
            .take(limit.coerceIn(1, 100))
            .toList()
    }

    override fun byId(id: String): ManualCity? = byId[id]
}
