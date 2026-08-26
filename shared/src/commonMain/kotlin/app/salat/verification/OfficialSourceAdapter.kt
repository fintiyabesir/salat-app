package app.salat.verification

import app.salat.model.GeoPoint
import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate

interface OfficialSourceAdapter {
    val metadata: SourceMetadata
    fun supports(countryCode: String, regionCode: String? = null): Boolean
    suspend fun fetch(request: VerificationRequest): List<PrayerDay>
    fun refreshPolicy(): RefreshPolicy
}

data class SourceMetadata(
    val id: String,
    val authorityName: String,
    val sourceUrl: String,
    val attribution: String,
    val preference: SourcePreference
)

enum class SourcePreference { PREFER_OFFICIAL, COMPARE_ONLY }

data class VerificationRequest(
    val range: ClosedRange<LocalDate>,
    val point: GeoPoint,
    val timeZoneId: String,
    val locationKey: String? = null
)

sealed interface RefreshPolicy {
    data object Annual : RefreshPolicy
    data object Monthly : RefreshPolicy
    data object Daily : RefreshPolicy
    data class FixedHours(val hours: Int) : RefreshPolicy
}

fun interface TextHttpClient {
    suspend fun get(url: String): String
}
