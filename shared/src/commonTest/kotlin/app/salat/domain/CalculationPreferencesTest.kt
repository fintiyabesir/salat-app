package app.salat.domain

import app.salat.model.CalculationMethodId
import app.salat.model.CalculationPreferences
import app.salat.model.HighLatitudeRuleId
import app.salat.model.MadhabId
import app.salat.model.PrayerAdjustments
import app.salat.model.PrayerName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CalculationPreferencesTest {
    @Test
    fun default_preferences_preserve_regional_profile() {
        val original = RegionalCalculationProfileResolver.resolve("TR")
        val resolved = RegionalCalculationProfileResolver.resolve("TR", CalculationPreferences())
        assertEquals(original, resolved)
    }

    @Test
    fun turkey_uses_diyanet_asr_i_awwal_and_pakistan_defaults_hanafi() {
        val turkey = RegionalCalculationProfileResolver.resolve("TR")
        val pakistan = RegionalCalculationProfileResolver.resolve("PK")
        assertEquals(MadhabId.SHAFI, turkey.madhab)
        assertEquals(MadhabId.HANAFI, pakistan.madhab)
        assertTrue(turkey.id.endsWith("-shafi"))
        assertTrue(pakistan.id.endsWith("-hanafi"))
    }

    @Test
    fun explicit_preferences_create_distinct_profile() {
        val resolved = RegionalCalculationProfileResolver.resolve(
            "TR",
            CalculationPreferences(
                methodOverride = CalculationMethodId.MUSLIM_WORLD_LEAGUE,
                madhabOverride = MadhabId.HANAFI,
                highLatitudeRule = HighLatitudeRuleId.TWILIGHT_ANGLE,
                adjustments = PrayerAdjustments(fajr = 2, isha = -1)
            )
        )
        assertEquals(CalculationMethodId.MUSLIM_WORLD_LEAGUE, resolved.method)
        assertEquals(MadhabId.HANAFI, resolved.madhab)
        assertEquals(HighLatitudeRuleId.TWILIGHT_ANGLE, resolved.highLatitudeRule)
        assertTrue(resolved.id.startsWith("custom-tr-"))
    }

    @Test
    fun hanafi_asr_is_later_than_shafi_for_same_day() {
        val engine = SalatEngine()
        val shafi = engine.calculateDay(
            2026, 8, 26, 41.0082, 28.9784, "Europe/Istanbul", "TR",
            CalculationPreferences(madhabOverride = MadhabId.SHAFI)
        )
        val hanafi = engine.calculateDay(
            2026, 8, 26, 41.0082, 28.9784, "Europe/Istanbul", "TR",
            CalculationPreferences(madhabOverride = MadhabId.HANAFI)
        )
        assertTrue(hanafi.time(PrayerName.ASR) > shafi.time(PrayerName.ASR))
        assertNotEquals(hanafi.calculationProfile, shafi.calculationProfile)
    }

    @Test
    fun prayer_adjustment_shifts_only_selected_prayer() {
        val engine = SalatEngine()
        val base = engine.calculateDay(2026, 8, 26, 41.0082, 28.9784, "Europe/Istanbul", "TR")
        val adjusted = engine.calculateDay(
            2026, 8, 26, 41.0082, 28.9784, "Europe/Istanbul", "TR",
            CalculationPreferences(adjustments = PrayerAdjustments(maghrib = 7))
        )

        val deltaMinutes = (adjusted.maghrib - base.maghrib).inWholeMinutes
        assertEquals(7, deltaMinutes)
        assertEquals(base.fajr, adjusted.fajr)
        assertEquals(base.isha, adjusted.isha)
    }

    @Test
    fun high_latitude_choice_is_part_of_profile_identity() {
        val automatic = RegionalCalculationProfileResolver.resolve(
            "GB", CalculationPreferences(highLatitudeRule = HighLatitudeRuleId.AUTOMATIC)
        )
        val seventh = RegionalCalculationProfileResolver.resolve(
            "GB", CalculationPreferences(highLatitudeRule = HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT)
        )
        assertNotEquals(automatic.id, seventh.id)
    }
}
