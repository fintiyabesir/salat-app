package app.salat.verification

import app.salat.model.GeoPoint
import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate

interface OfficialSourceAdapter {
    val metadata: SourceMetadata
    fun supports(countryCode: String, regionCode: String? = null): Boolean
    /** May return a wider cacheable period than [request] when the source publishes annual/bulk data. */
    suspend fun fetch(request: VerificationRequest): List<PrayerDay>
    fun refreshPolicy(): RefreshPolicy
}

data class SourceMetadata(
    val id: String,
    val authorityName: String,
    val sourceUrl: String,
    val attribution: String,
    val preference: SourcePreference,
    val runtimeUse: RuntimeUsePolicy,
    val usage: SourceUsagePolicy
)

enum class SourcePreference { PREFER_OFFICIAL, COMPARE_ONLY }

enum class RuntimeUsePolicy { ENABLED, PERMISSION_REQUIRED }

enum class UsagePermission { ALLOWED, PERMISSION_REQUIRED, NOT_CONFIRMED }

enum class CredentialPolicy { NONE, OPTIONAL_SERVER_SIDE, REQUIRED_SERVER_SIDE }

data class SourceUsagePolicy(
    val termsUrl: String?,
    val licenseName: String,
    val commercialUse: UsagePermission,
    val redistribution: UsagePermission,
    val caching: UsagePermission,
    val attributionRequired: Boolean,
    val publishedRateLimit: String?,
    val credentials: CredentialPolicy,
    val reviewedOn: String
)

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
