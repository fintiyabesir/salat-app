package app.salat.mobile

import android.content.Context
import app.salat.model.GeoPoint
import app.salat.model.LocationSource
import app.salat.model.ResolvedLocation
import java.text.Normalizer
import java.util.Locale

internal data class OfflineCityEntry(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val regionName: String?,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val searchKey: String,
    val foldedSearchKey: String
) {
    fun asResolvedLocation(): ResolvedLocation = ResolvedLocation(
        point = GeoPoint(latitude, longitude),
        timeZoneId = timeZoneId,
        countryCode = countryCode,
        cityName = name,
        regionName = regionName,
        source = LocationSource.MANUAL_CITY
    )
}

/**
 * Reads the generated GeoNames-derived catalog from the APK. No network access is
 * performed here; the asset is produced at build time and shipped with the app.
 */
internal class AndroidOfflineCityCatalog(private val context: Context) {
    @Volatile private var cache: List<OfflineCityEntry>? = null

    fun load(): List<OfflineCityEntry> {
        cache?.let { return it }
        val loaded = context.assets.open(CATALOG_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull(::parse)
                .toList()
        }
        cache = loaded
        return loaded
    }

    fun search(query: String, limit: Int = 30): List<OfflineCityEntry> {
        val all = load()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all.take(limit.coerceIn(1, 100))

        val normalized = normalize(trimmed)
        val folded = fold(trimmed)
        return all.asSequence()
            .filter { city ->
                normalized in city.searchKey || (folded.isNotEmpty() && folded in city.foldedSearchKey)
            }
            .take(limit.coerceIn(1, 100))
            .toList()
    }

    private fun parse(line: String): OfflineCityEntry? {
        val p = line.split('\t')
        if (p.size < 9) return null
        val latitude = p[5].toDoubleOrNull() ?: return null
        val longitude = p[6].toDoubleOrNull() ?: return null
        val region = p[4].ifBlank { null }
        val aliases = p[8].replace('|', ' ')
        val searchable = listOf(p[1], p[2], p[3], region.orEmpty(), aliases).joinToString(" ")
        return OfflineCityEntry(
            id = p[0],
            name = p[1],
            countryCode = p[2],
            countryName = p[3],
            regionName = region,
            latitude = latitude,
            longitude = longitude,
            timeZoneId = p[7],
            searchKey = normalize(searchable),
            foldedSearchKey = fold(searchable)
        )
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun fold(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("\\s+"), " ")

    private companion object {
        const val CATALOG_ASSET = "city_catalog.tsv"
    }
}
