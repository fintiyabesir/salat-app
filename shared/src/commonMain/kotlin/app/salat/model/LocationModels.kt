package app.salat.model

/**
 * Location context needed by the prayer domain. Coordinates are the source of
 * truth; city/country labels are optional presentation and regional-policy hints.
 */
data class ResolvedLocation(
    val point: GeoPoint,
    val timeZoneId: String,
    val countryCode: String? = null,
    val cityName: String? = null,
    val regionName: String? = null,
    val source: LocationSource = LocationSource.DEVICE
) {
    val displayName: String
        get() = cityName ?: "${point.latitude.formatCoordinate()}, ${point.longitude.formatCoordinate()}"

    private fun Double.formatCoordinate(): String =
        ((this * 1000.0).toInt() / 1000.0).toString()
}

enum class LocationSource { DEVICE, MANUAL_CITY }
