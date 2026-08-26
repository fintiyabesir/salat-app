package app.salat.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

enum class PrayerName { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

enum class CalculationMethodId {
    TURKEY,
    MALAYSIA,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
    DUBAI,
    QATAR,
    KUWAIT,
    MOON_SIGHTING_COMMITTEE,
    SINGAPORE,
    NORTH_AMERICA
}

enum class MadhabId { SHAFI, HANAFI }

enum class HighLatitudeRuleId {
    AUTOMATIC,
    MIDDLE_OF_THE_NIGHT,
    SEVENTH_OF_THE_NIGHT,
    TWILIGHT_ANGLE
}

data class PrayerAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    fun isZero(): Boolean = fajr == 0 && sunrise == 0 && dhuhr == 0 && asr == 0 && maghrib == 0 && isha == 0
}

data class CalculationPreferences(
    val methodOverride: CalculationMethodId? = null,
    val madhabOverride: MadhabId? = null,
    val highLatitudeRule: HighLatitudeRuleId = HighLatitudeRuleId.AUTOMATIC,
    val adjustments: PrayerAdjustments = PrayerAdjustments()
) {
    fun isDefault(): Boolean = methodOverride == null &&
        madhabOverride == null &&
        highLatitudeRule == HighLatitudeRuleId.AUTOMATIC &&
        adjustments.isZero()
}

data class CalculationProfile(
    val id: String,
    val method: CalculationMethodId,
    val madhab: MadhabId = MadhabId.SHAFI,
    val highLatitudeRule: HighLatitudeRuleId = HighLatitudeRuleId.AUTOMATIC,
    val adjustments: PrayerAdjustments = PrayerAdjustments()
)

data class PrayerDay(
    val date: LocalDate,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
    val calculationProfile: String,
    val verification: VerificationState = VerificationState.Unverified
) {
    fun time(prayer: PrayerName): Instant = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.SUNRISE -> sunrise
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }
}

sealed interface VerificationState {
    data object Unverified : VerificationState
    data class Verified(
        val sourceId: String,
        val verifiedAt: Instant,
        val maxDeltaMinutes: Int
    ) : VerificationState
    data class Different(
        val sourceId: String,
        val maxDeltaMinutes: Int
    ) : VerificationState
    data class Unavailable(val sourceId: String?) : VerificationState
}
