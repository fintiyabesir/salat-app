package app.salat.mobile

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import app.salat.model.PrayerName
import app.salat.domain.KerahatId
import app.salat.domain.DayTimes
import app.salat.domain.DayStatusCalculator
import app.salat.domain.DayStatus
import app.salat.domain.DayPeriodId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class SalatAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    /** Resizing decides which artboard applies, so it has to redraw, not just relayout. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        render(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ROLLOVER) {
            refreshAll(context)
        }
    }

    companion object {
        private const val ACTION_ROLLOVER = "app.salat.mobile.WIDGET_ROLLOVER"
        private const val REQUEST_ROLLOVER = 7201

        /** Below this the dense strip has no room, so the large-text artboard applies. */
        private const val DENSE_MIN_WIDTH_DP = 220

        fun refreshAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, SalatAppWidgetProvider::class.java)
            render(appContext, manager, manager.getAppWidgetIds(component))
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val timeline = AndroidGlanceTimelineStore(context).load()
            val next = timeline?.next()
            val now = System.currentTimeMillis()
            val locale = Locale.getDefault()

            ids.forEach { id ->
                val width = manager.getAppWidgetOptions(id)
                    ?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
                // A brand-new widget reports 0 until the host measures it; the dense
                // layout is the declared target size, so default to it.
                val dense = width == 0 || width >= DENSE_MIN_WIDTH_DP
                val layout = if (dense) R.layout.widget_next_prayer else R.layout.widget_next_prayer_large
                val views = RemoteViews(context.packageName, layout)
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, id))

                if (timeline == null || next == null) {
                    if (dense) {
                        views.setTextViewText(R.id.widget_location, context.getString(R.string.widget_open_app_hint))
                        views.setTextViewText(R.id.widget_hijri, "")
                        SLOT_IDS.forEach { (labelId, timeId) ->
                            views.setTextViewText(labelId, "")
                            views.setTextViewText(timeId, "")
                        }
                    }
                    views.setTextViewText(R.id.widget_period, "")
                    views.setTextViewText(R.id.widget_prayer, context.getString(R.string.next_prayer))
                    views.setTextViewText(R.id.widget_time, "—")
                    views.setViewVisibility(R.id.widget_countdown, View.GONE)
                } else {
                    val zone = runCatching { ZoneId.of(timeline.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
                    views.setTextViewText(R.id.widget_prayer, localizedPrayer(context, next.prayer))
                    views.setTextViewText(R.id.widget_time, formatTime(next.atMillis, zone, locale))
                    views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
                    val base = SystemClock.elapsedRealtime() + max(0L, next.atMillis - now)
                    views.setChronometer(R.id.widget_countdown, base, null, true)
                    views.setChronometerCountDown(R.id.widget_countdown, true)
                    // The same "where am I / what is next" the phone shows, so the
                    // widget is not telling a different story from the app behind it.
                    val status = dayStatus(context, timeline, zone, now)
                    views.setTextViewText(R.id.widget_period, periodText(context, status))
                    views.setTextColor(
                        R.id.widget_period,
                        context.getColor(
                            if (status?.kerahat != null) R.color.widget_kerahat else R.color.widget_brand
                        )
                    )
                    if (dense) {
                        views.setTextViewText(R.id.widget_location, timeline.locationName)
                        views.setTextViewText(R.id.widget_hijri, hijriToday(context, zone, locale))
                        renderDayStrip(context, views, timeline, zone, locale, next.prayer, now)
                    }
                }
                manager.updateAppWidget(id, views)
            }

            if (next != null) scheduleRollover(context, next.atMillis)
        }

        /**
         * The six slots of today, coloured the way the day strip on the phone is: spent
         * prayers recede, the one you are waiting for is the gold mark.
         */
        private fun renderDayStrip(
            context: Context,
            views: RemoteViews,
            timeline: AndroidGlanceTimeline,
            zone: ZoneId,
            locale: Locale,
            nextPrayer: PrayerName,
            nowMillis: Long
        ) {
            val today = LocalDate.now(zone)
            val todayEvents = timeline.events.filter {
                Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == today
            }.associateBy { it.prayer }
            val past = context.getColor(R.color.widget_slot_past)
            val current = context.getColor(R.color.widget_slot_current)
            val label = context.getColor(R.color.widget_text_secondary)
            val value = context.getColor(R.color.widget_text_primary)

            PrayerName.entries.forEachIndexed { index, prayer ->
                val (labelId, timeId) = SLOT_IDS[index]
                val event = todayEvents[prayer]
                views.setTextViewText(labelId, shortPrayer(context, prayer))
                views.setTextViewText(timeId, event?.let { formatTime(it.atMillis, zone, locale) } ?: "—")
                val labelColor: Int
                val valueColor: Int
                when {
                    prayer == nextPrayer -> { labelColor = current; valueColor = current }
                    event != null && event.atMillis <= nowMillis -> { labelColor = past; valueColor = past }
                    else -> { labelColor = label; valueColor = value }
                }
                views.setTextColor(labelId, labelColor)
                views.setTextColor(timeId, valueColor)
            }
        }

        /**
         * Built from the stored projection rather than recalculated: the timeline
         * already holds every instant these windows hang off.
         */
        private fun dayStatus(
            context: Context,
            timeline: AndroidGlanceTimeline,
            zone: ZoneId,
            nowMillis: Long
        ): DayStatus? {
            val today = LocalDate.now(zone)
            fun times(date: LocalDate): DayTimes? {
                val ofDay = timeline.events
                    .filter { Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == date }
                    .associateBy { it.prayer }
                if (ofDay.size < PrayerName.entries.size) return null
                return DayTimes(
                    fajr = ofDay.getValue(PrayerName.FAJR).atMillis,
                    sunrise = ofDay.getValue(PrayerName.SUNRISE).atMillis,
                    dhuhr = ofDay.getValue(PrayerName.DHUHR).atMillis,
                    asr = ofDay.getValue(PrayerName.ASR).atMillis,
                    maghrib = ofDay.getValue(PrayerName.MAGHRIB).atMillis,
                    isha = ofDay.getValue(PrayerName.ISHA).atMillis
                )
            }
            // The projection starts today, so yesterday is absent; Isha's own start is
            // only needed to label the window, never to decide which one we are in.
            val todayTimes = times(today) ?: return null
            val tomorrow = times(today.plusDays(1)) ?: todayTimes
            return DayStatusCalculator.evaluate(
                nowMillis = nowMillis,
                today = todayTimes,
                yesterday = times(today.minusDays(1)) ?: todayTimes,
                tomorrow = tomorrow,
                kerahatMinutes = AndroidAppSettingsStore(context).load().kerahatMinutes
            )
        }

        private fun periodText(context: Context, status: DayStatus?): String = when {
            status == null -> ""
            status.kerahat != null -> context.getString(
                when (status.kerahat!!.id) {
                    KerahatId.SUNRISE -> R.string.kerahat_sunrise
                    KerahatId.ZENITH -> R.string.kerahat_zenith
                    KerahatId.SUNSET -> R.string.kerahat_sunset
                }
            )
            else -> context.getString(
                when (status.period.id) {
                    DayPeriodId.FAJR -> R.string.period_fajr
                    DayPeriodId.DUHA -> R.string.period_duha
                    DayPeriodId.DHUHR -> R.string.period_dhuhr
                    DayPeriodId.ASR -> R.string.period_asr
                    DayPeriodId.MAGHRIB -> R.string.period_maghrib
                    DayPeriodId.ISHA -> R.string.period_isha
                }
            )
        }

        private fun hijriToday(context: Context, zone: ZoneId, locale: Locale): String {
            val settings = AndroidAppSettingsStore(context).load()
            return AndroidHijriFormatter.format(
                date = LocalDate.now(zone),
                zoneId = zone,
                locale = locale,
                method = settings.hijriMethod,
                dayAdjustment = settings.hijriDayAdjustment
            )
        }

        private fun formatTime(atMillis: Long, zone: ZoneId, locale: Locale): String =
            DateTimeFormatter.ofPattern("HH:mm", locale)
                .withZone(zone)
                .format(Instant.ofEpochMilli(atMillis))

        private fun openAppIntent(context: Context, widgetId: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                widgetId,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun scheduleRollover(context: Context, atMillis: Long) {
            if (atMillis <= System.currentTimeMillis()) return
            val alarm = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, SalatAppWidgetProvider::class.java).setAction(ACTION_ROLLOVER)
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_ROLLOVER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis + 1_000L, pending)
        }

        private val SLOT_IDS = listOf(
            R.id.widget_slot_0_label to R.id.widget_slot_0_time,
            R.id.widget_slot_1_label to R.id.widget_slot_1_time,
            R.id.widget_slot_2_label to R.id.widget_slot_2_time,
            R.id.widget_slot_3_label to R.id.widget_slot_3_time,
            R.id.widget_slot_4_label to R.id.widget_slot_4_time,
            R.id.widget_slot_5_label to R.id.widget_slot_5_time
        )

        private fun localizedPrayer(context: Context, prayer: PrayerName): String = context.getString(
            when (prayer) {
                PrayerName.FAJR -> R.string.prayer_fajr
                PrayerName.SUNRISE -> R.string.prayer_sunrise
                PrayerName.DHUHR -> R.string.prayer_dhuhr
                PrayerName.ASR -> R.string.prayer_asr
                PrayerName.MAGHRIB -> R.string.prayer_maghrib
                PrayerName.ISHA -> R.string.prayer_isha
            }
        )

        private fun shortPrayer(context: Context, prayer: PrayerName): String = context.getString(
            when (prayer) {
                PrayerName.FAJR -> R.string.prayer_fajr_short
                PrayerName.SUNRISE -> R.string.prayer_sunrise_short
                PrayerName.DHUHR -> R.string.prayer_dhuhr_short
                PrayerName.ASR -> R.string.prayer_asr_short
                PrayerName.MAGHRIB -> R.string.prayer_maghrib_short
                PrayerName.ISHA -> R.string.prayer_isha_short
            }
        )
    }
}
