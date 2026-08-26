package app.salat.mobile

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import app.salat.model.PrayerName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class SalatAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
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

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_next_prayer)
                views.setTextViewText(R.id.widget_brand, context.getString(R.string.brand_name))
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, id))

                if (timeline == null || next == null) {
                    views.setTextViewText(R.id.widget_location, context.getString(R.string.widget_open_app_hint))
                    views.setTextViewText(R.id.widget_prayer, context.getString(R.string.next_prayer))
                    views.setTextViewText(R.id.widget_time, "—")
                    views.setViewVisibility(R.id.widget_countdown, View.GONE)
                } else {
                    val zone = runCatching { ZoneId.of(timeline.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
                    val locale = Locale.getDefault()
                    val time = DateTimeFormatter.ofPattern("HH:mm", locale)
                        .withZone(zone)
                        .format(Instant.ofEpochMilli(next.atMillis))
                    views.setTextViewText(R.id.widget_location, timeline.locationName)
                    views.setTextViewText(R.id.widget_prayer, localizedPrayer(context, next.prayer))
                    views.setTextViewText(R.id.widget_time, time)
                    views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
                    val base = SystemClock.elapsedRealtime() + max(0L, next.atMillis - now)
                    views.setChronometer(R.id.widget_countdown, base, null, true)
                    views.setChronometerCountDown(R.id.widget_countdown, true)
                }
                manager.updateAppWidget(id, views)
            }

            if (next != null) scheduleRollover(context, next.atMillis)
        }

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
    }
}
