package app.salat.location

import app.salat.model.GeoPoint

/**
 * Small built-in catalog used to make manual selection functional before issue #13
 * replaces it with the generated GeoNames-derived offline catalog.
 */
object StarterManualCityCatalog : ManualCityCatalog {
    private val delegate = InMemoryManualCityCatalog(
        listOf(
            city("tr-istanbul", "İstanbul", "TR", "Türkiye", 41.0082, 28.9784, "Europe/Istanbul", "İstanbul"),
            city("tr-ankara", "Ankara", "TR", "Türkiye", 39.9334, 32.8597, "Europe/Istanbul", "Ankara"),
            city("tr-izmir", "İzmir", "TR", "Türkiye", 38.4237, 27.1428, "Europe/Istanbul", "İzmir"),
            city("tr-bursa", "Bursa", "TR", "Türkiye", 40.1950, 29.0600, "Europe/Istanbul", "Bursa"),
            city("tr-antalya", "Antalya", "TR", "Türkiye", 36.8969, 30.7133, "Europe/Istanbul", "Antalya"),
            city("tr-konya", "Konya", "TR", "Türkiye", 37.8746, 32.4932, "Europe/Istanbul", "Konya"),
            city("tr-adana", "Adana", "TR", "Türkiye", 37.0000, 35.3213, "Europe/Istanbul", "Adana"),
            city("tr-gaziantep", "Gaziantep", "TR", "Türkiye", 37.0662, 37.3833, "Europe/Istanbul", "Gaziantep"),
            city("tr-samsun", "Samsun", "TR", "Türkiye", 41.2867, 36.3300, "Europe/Istanbul", "Samsun"),
            city("tr-diyarbakir", "Diyarbakır", "TR", "Türkiye", 37.9144, 40.2306, "Europe/Istanbul", "Diyarbakır"),
            city("az-baku", "Baku", "AZ", "Azerbaijan", 40.4093, 49.8671, "Asia/Baku", "Baku"),
            city("kz-almaty", "Almaty", "KZ", "Kazakhstan", 43.2389, 76.8897, "Asia/Almaty", "Almaty"),
            city("uz-tashkent", "Tashkent", "UZ", "Uzbekistan", 41.2995, 69.2401, "Asia/Tashkent", "Tashkent"),
            city("sa-makkah", "Makkah", "SA", "Saudi Arabia", 21.3891, 39.8579, "Asia/Riyadh", "Makkah"),
            city("sa-madinah", "Madinah", "SA", "Saudi Arabia", 24.5247, 39.5692, "Asia/Riyadh", "Madinah"),
            city("ae-dubai", "Dubai", "AE", "United Arab Emirates", 25.2048, 55.2708, "Asia/Dubai", "Dubai"),
            city("qa-doha", "Doha", "QA", "Qatar", 25.2854, 51.5310, "Asia/Qatar", "Doha"),
            city("eg-cairo", "Cairo", "EG", "Egypt", 30.0444, 31.2357, "Africa/Cairo", "Cairo"),
            city("pk-karachi", "Karachi", "PK", "Pakistan", 24.8607, 67.0011, "Asia/Karachi", "Sindh"),
            city("my-kuala-lumpur", "Kuala Lumpur", "MY", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur", "Kuala Lumpur"),
            city("sg-singapore", "Singapore", "SG", "Singapore", 1.3521, 103.8198, "Asia/Singapore", "Singapore"),
            city("id-jakarta", "Jakarta", "ID", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta", "Jakarta"),
            city("gb-london", "London", "GB", "United Kingdom", 51.5074, -0.1278, "Europe/London", "England"),
            city("de-berlin", "Berlin", "DE", "Germany", 52.5200, 13.4050, "Europe/Berlin", "Berlin"),
            city("fr-paris", "Paris", "FR", "France", 48.8566, 2.3522, "Europe/Paris", "Île-de-France"),
            city("ba-sarajevo", "Sarajevo", "BA", "Bosnia and Herzegovina", 43.8563, 18.4131, "Europe/Sarajevo", "Sarajevo"),
            city("us-new-york", "New York", "US", "United States", 40.7128, -74.0060, "America/New_York", "New York"),
            city("ca-toronto", "Toronto", "CA", "Canada", 43.6532, -79.3832, "America/Toronto", "Ontario"),
            city("au-sydney", "Sydney", "AU", "Australia", -33.8688, 151.2093, "Australia/Sydney", "New South Wales"),
            city("za-johannesburg", "Johannesburg", "ZA", "South Africa", -26.2041, 28.0473, "Africa/Johannesburg", "Gauteng")
        )
    )

    override fun search(query: String, limit: Int): List<ManualCity> = delegate.search(query, limit)

    override fun byId(id: String): ManualCity? = delegate.byId(id)

    private fun city(
        id: String,
        name: String,
        countryCode: String,
        countryName: String,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        regionName: String
    ) = ManualCity(
        id = id,
        name = name,
        countryCode = countryCode,
        countryName = countryName,
        point = GeoPoint(latitude, longitude),
        timeZoneId = timeZoneId,
        regionName = regionName
    )
}
