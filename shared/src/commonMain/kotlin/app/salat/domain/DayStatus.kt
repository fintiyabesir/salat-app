package app.salat.domain

import app.salat.model.CalculationPreferences
import app.salat.model.PrayerDay
import app.salat.model.PrayerName

/**
 * Which stretch of the day we are standing in.
 *
 * These are windows, not instants: FAJR runs from the Fajr time until sunrise, and
 * ISHA runs until the next day's Fajr. DUHA is the gap between sunrise and Dhuhr,
 * which no obligatory prayer occupies — sunrise ends the Fajr window rather than
 * opening one of its own.
 */
enum class DayPeriodId { FAJR, DUHA, DHUHR, ASR, MAGHRIB, ISHA }

/** The window we are in, and the instant it gives way to the next. */
data class DayPeriod(
    val id: DayPeriodId,
    val startMillis: Long,
    val endMillis: Long
) {
    fun remainingMillis(nowMillis: Long): Long = (endMillis - nowMillis).coerceAtLeast(0L)
}

/**
 * The three windows in which prayer is discouraged, all of them tied to where the
 * sun is rather than to a prayer time.
 */
enum class KerahatId {
    /** From sunrise until the sun has climbed clear of the horizon. */
    SUNRISE,

    /** The sun at its zenith, until it passes the meridian and Dhuhr begins. */
    ZENITH,

    /** From the sun yellowing until it sets. */
    SUNSET
}

data class KerahatWindow(
    val id: KerahatId,
    val startMillis: Long,
    val endMillis: Long
) {
    fun contains(nowMillis: Long): Boolean = nowMillis in startMillis until endMillis

    fun remainingMillis(nowMillis: Long): Long = (endMillis - nowMillis).coerceAtLeast(0L)
}

/**
 * The six instants a day is built from, in order. Kept as plain longs so every
 * surface — phone, widget, watch — can feed it from whatever it already holds
 * without depending on the calculation engine.
 */
data class DayTimes(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
) {
    fun at(prayer: PrayerName): Long = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.SUNRISE -> sunrise
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }
}

/** Everything a surface needs to say where the day has got to. */
data class DayStatus(
    val period: DayPeriod,
    /** Non-null only while we are inside one of the three windows. */
    val kerahat: KerahatWindow?
)

/**
 * Turns prayer instants into the state people actually ask about: not "when is the
 * next prayer" but "which prayer am I in, and how long have I got".
 *
 * Both answers matter and they are not the same question — the number is identical
 * while the meaning is not, which is exactly why naming the current window is worth
 * the room it takes.
 */
object DayStatusCalculator {
    /** Diyanet publishes kerahat as three-quarters of an hour. */
    const val DEFAULT_KERAHAT_MINUTES = 45
    val KERAHAT_MINUTES_RANGE = 10..60

    /**
     * @param today the six instants of the day [nowMillis] falls in
     * @param yesterday the day before, needed because Isha runs past midnight
     * @param tomorrow the day after, needed to close the Isha window at the next Fajr
     * @param kerahatMinutes null when the user has turned kerahat off
     */
    fun evaluate(
        nowMillis: Long,
        today: DayTimes,
        yesterday: DayTimes,
        tomorrow: DayTimes,
        kerahatMinutes: Int? = DEFAULT_KERAHAT_MINUTES
    ): DayStatus = DayStatus(
        period = period(nowMillis, today, yesterday, tomorrow),
        kerahat = kerahatMinutes
            ?.let { minutes ->
                // Yesterday's sunset window can still be running just after midnight
                // only in the far north; checking both days costs nothing and avoids
                // a class of edge case entirely.
                (windows(yesterday, minutes) + windows(today, minutes) + windows(tomorrow, minutes))
                    .firstOrNull { it.contains(nowMillis) }
            }
    )

    /** The three kerahat windows of one day, in the order they occur. */
    fun windows(times: DayTimes, minutes: Int): List<KerahatWindow> {
        val span = minutes.coerceIn(KERAHAT_MINUTES_RANGE) * 60_000L
        return listOf(
            KerahatWindow(KerahatId.SUNRISE, times.sunrise, times.sunrise + span),
            KerahatWindow(KerahatId.ZENITH, times.dhuhr - span, times.dhuhr),
            KerahatWindow(KerahatId.SUNSET, times.maghrib - span, times.maghrib)
        )
    }

    private fun period(
        nowMillis: Long,
        today: DayTimes,
        yesterday: DayTimes,
        tomorrow: DayTimes
    ): DayPeriod = when {
        // Before today's Fajr we are still inside yesterday's Isha.
        nowMillis < today.fajr ->
            DayPeriod(DayPeriodId.ISHA, yesterday.isha, today.fajr)
        nowMillis < today.sunrise ->
            DayPeriod(DayPeriodId.FAJR, today.fajr, today.sunrise)
        nowMillis < today.dhuhr ->
            DayPeriod(DayPeriodId.DUHA, today.sunrise, today.dhuhr)
        nowMillis < today.asr ->
            DayPeriod(DayPeriodId.DHUHR, today.dhuhr, today.asr)
        nowMillis < today.maghrib ->
            DayPeriod(DayPeriodId.ASR, today.asr, today.maghrib)
        nowMillis < today.isha ->
            DayPeriod(DayPeriodId.MAGHRIB, today.maghrib, today.isha)
        else ->
            DayPeriod(DayPeriodId.ISHA, today.isha, tomorrow.fajr)
    }
}

/** The six instants of one calculated day, in the shape [DayStatusCalculator] wants. */
fun PrayerDay.toDayTimes(): DayTimes = DayTimes(
    fajr = fajr.toEpochMilliseconds(),
    sunrise = sunrise.toEpochMilliseconds(),
    dhuhr = dhuhr.toEpochMilliseconds(),
    asr = asr.toEpochMilliseconds(),
    maghrib = maghrib.toEpochMilliseconds(),
    isha = isha.toEpochMilliseconds()
)

fun SalatEngine.dayTimes(
    year: Int,
    month: Int,
    day: Int,
    latitude: Double,
    longitude: Double,
    timeZoneId: String,
    countryCode: String,
    preferences: CalculationPreferences
): DayTimes = calculateDay(
    year = year,
    month = month,
    day = day,
    latitude = latitude,
    longitude = longitude,
    timeZoneId = timeZoneId,
    countryCode = countryCode,
    preferences = preferences
).toDayTimes()
