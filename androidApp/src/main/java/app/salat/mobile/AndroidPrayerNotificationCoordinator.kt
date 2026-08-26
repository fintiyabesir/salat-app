package app.salat.mobile

import android.content.Context
import app.salat.domain.SalatEngine
import app.salat.model.ResolvedLocation
import app.salat.notification.PrayerNotificationPlanner
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/** Rebuilds a rolling device-local prayer notification plan from current app state. */
class AndroidPrayerNotificationCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val engine = SalatEngine()
    private val notificationSettingsStore = AndroidPrayerNotificationSettingsStore(appContext)
    private val appSettingsStore = AndroidAppSettingsStore(appContext)
    private val scheduler = AndroidPrayerNotificationScheduler(appContext)
    private val replanStore = PrayerScheduleReplanStore(appContext)

    fun rebuild(location: ResolvedLocation, horizonDays: Int = DEFAULT_HORIZON_DAYS) {
        val zone = ZoneId.of(location.timeZoneId)
        val start = LocalDate.now(zone)
        val calculationPreferences = appSettingsStore.load().calculation
        val days = (0 until horizonDays.coerceIn(1, 10)).map { offset ->
            val date = start.plusDays(offset.toLong())
            engine.calculateDay(
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                latitude = location.point.latitude,
                longitude = location.point.longitude,
                timeZoneId = location.timeZoneId,
                countryCode = location.countryCode ?: "ZZ",
                preferences = calculationPreferences
            )
        }

        val plan = PrayerNotificationPlanner.plan(
            days = days,
            settings = notificationSettingsStore.load(),
            now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        scheduler.reschedule(plan)
        replanStore.clear()
    }

    fun cancelAll() = scheduler.cancelAll()

    companion object {
        /** 6 daily times × 7 days keeps a compact rolling horizon. */
        const val DEFAULT_HORIZON_DAYS = 7
    }
}
