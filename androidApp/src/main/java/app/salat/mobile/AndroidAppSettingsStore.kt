package app.salat.mobile

import android.content.Context
import app.salat.domain.DayStatusCalculator
import app.salat.model.AppPreferences
import app.salat.model.AppearanceMode
import app.salat.model.CalculationMethodId
import app.salat.model.CalculationPreferences
import app.salat.model.HighLatitudeRuleId
import app.salat.model.HijriCalendarMethodId
import app.salat.model.MadhabId
import app.salat.model.PrayerAdjustments

class AndroidAppSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /**
     * The Qibla threshold is seeded from the device the first time it is read, so
     * from then on it is a plain stored number. There is no "automatic" mode to
     * resolve at use time: when the Qibla will not appear, the user has to be able
     * to see the number that is hiding it.
     */
    fun load(): AppPreferences {
        if (prefs.getInt(KEY_QIBLA_THRESHOLD, 0) <= 0) {
            prefs.edit()
                .putInt(KEY_QIBLA_THRESHOLD, AndroidQiblaHeadingProvider(appContext).seedAccuracyThresholdDegrees)
                .apply()
        }
        return loadStored()
    }

    private fun loadStored(): AppPreferences {
        val calculation = CalculationPreferences(
            methodOverride = enumOrNull<CalculationMethodId>(prefs.getString(KEY_METHOD, null)),
            madhabOverride = enumOrNull<MadhabId>(prefs.getString(KEY_MADHAB, null)),
            highLatitudeRule = enumOrDefault(
                prefs.getString(KEY_HIGH_LATITUDE, null),
                HighLatitudeRuleId.AUTOMATIC
            ),
            adjustments = PrayerAdjustments(
                fajr = prefs.getInt(KEY_ADJ_FAJR, 0),
                sunrise = prefs.getInt(KEY_ADJ_SUNRISE, 0),
                dhuhr = prefs.getInt(KEY_ADJ_DHUHR, 0),
                asr = prefs.getInt(KEY_ADJ_ASR, 0),
                maghrib = prefs.getInt(KEY_ADJ_MAGHRIB, 0),
                isha = prefs.getInt(KEY_ADJ_ISHA, 0)
            )
        )
        return AppPreferences(
            calculation = calculation,
            hijriMethod = enumOrDefault(
                prefs.getString(KEY_HIJRI_METHOD, null),
                HijriCalendarMethodId.AUTOMATIC
            ),
            hijriDayAdjustment = prefs.getInt(KEY_HIJRI_OFFSET, 0).coerceIn(-2, 2),
            languageTag = prefs.getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() },
            qiblaAccuracyThresholdDegrees = prefs.getInt(KEY_QIBLA_THRESHOLD, 0).takeIf { it > 0 },
            // 0 means the user turned kerahat off; absent means never chosen.
            kerahatMinutes = prefs
                .getInt(KEY_KERAHAT_MINUTES, DayStatusCalculator.DEFAULT_KERAHAT_MINUTES)
                .takeIf { it > 0 },
            appearance = enumOrDefault(
                prefs.getString(KEY_APPEARANCE, null),
                AppearanceMode.SYSTEM
            )
        )
    }

    fun save(value: AppPreferences) {
        prefs.edit()
            .putNullableString(KEY_METHOD, value.calculation.methodOverride?.name)
            .putNullableString(KEY_MADHAB, value.calculation.madhabOverride?.name)
            .putString(KEY_HIGH_LATITUDE, value.calculation.highLatitudeRule.name)
            .putInt(KEY_ADJ_FAJR, value.calculation.adjustments.fajr)
            .putInt(KEY_ADJ_SUNRISE, value.calculation.adjustments.sunrise)
            .putInt(KEY_ADJ_DHUHR, value.calculation.adjustments.dhuhr)
            .putInt(KEY_ADJ_ASR, value.calculation.adjustments.asr)
            .putInt(KEY_ADJ_MAGHRIB, value.calculation.adjustments.maghrib)
            .putInt(KEY_ADJ_ISHA, value.calculation.adjustments.isha)
            .putString(KEY_HIJRI_METHOD, value.hijriMethod.name)
            .putInt(KEY_HIJRI_OFFSET, value.hijriDayAdjustment)
            // 0 stands for "decide from the hardware"; the range never includes it.
            .putInt(KEY_QIBLA_THRESHOLD, value.qiblaAccuracyThresholdDegrees ?: 0)
            .putInt(KEY_KERAHAT_MINUTES, value.kerahatMinutes ?: 0)
            .putNullableString(KEY_LANGUAGE, value.languageTag)
            .putString(KEY_APPEARANCE, value.appearance.name)
            .apply()

        // Keep backend-free widgets aligned with calculation-profile changes.
        AndroidGlanceTimelineStore(appContext).rebuildLastKnown(value)
    }

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
        enumOrNull<T>(raw) ?: fallback

    private fun android.content.SharedPreferences.Editor.putNullableString(key: String, value: String?) = apply {
        if (value == null) remove(key) else putString(key, value)
    }

    companion object {
        private const val FILE_NAME = "salat_app_settings"
        private const val KEY_METHOD = "calculation_method"
        private const val KEY_MADHAB = "madhab"
        private const val KEY_HIGH_LATITUDE = "high_latitude"
        private const val KEY_ADJ_FAJR = "adjustment_fajr"
        private const val KEY_ADJ_SUNRISE = "adjustment_sunrise"
        private const val KEY_ADJ_DHUHR = "adjustment_dhuhr"
        private const val KEY_ADJ_ASR = "adjustment_asr"
        private const val KEY_ADJ_MAGHRIB = "adjustment_maghrib"
        private const val KEY_ADJ_ISHA = "adjustment_isha"
        private const val KEY_KERAHAT_MINUTES = "kerahat_minutes"
        private const val KEY_HIJRI_METHOD = "hijri_method"
        private const val KEY_HIJRI_OFFSET = "hijri_day_adjustment"
        private const val KEY_LANGUAGE = "language_tag"
        private const val KEY_QIBLA_THRESHOLD = "qiblaAccuracyThresholdDegrees"
        private const val KEY_APPEARANCE = "appearance"
    }
}
