package app.salat.model

/** Primitive-only projection intended for Swift/ObjC and other platform boundaries. */
data class PrayerDaySnapshot(
    val dateIso: String,
    val fajrEpochMillis: Long,
    val sunriseEpochMillis: Long,
    val dhuhrEpochMillis: Long,
    val asrEpochMillis: Long,
    val maghribEpochMillis: Long,
    val ishaEpochMillis: Long,
    val calculationProfile: String
)

fun PrayerDay.toSnapshot(): PrayerDaySnapshot = PrayerDaySnapshot(
    dateIso = date.toString(),
    fajrEpochMillis = fajr.toEpochMilliseconds(),
    sunriseEpochMillis = sunrise.toEpochMilliseconds(),
    dhuhrEpochMillis = dhuhr.toEpochMilliseconds(),
    asrEpochMillis = asr.toEpochMilliseconds(),
    maghribEpochMillis = maghrib.toEpochMilliseconds(),
    ishaEpochMillis = isha.toEpochMilliseconds(),
    calculationProfile = calculationProfile
)
