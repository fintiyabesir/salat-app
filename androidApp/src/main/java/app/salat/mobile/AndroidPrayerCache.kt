package app.salat.mobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.salat.model.PrayerDay
import app.salat.repository.PrayerCache
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Small dependency-free persistent cache for official prayer timetables. */
class AndroidPrayerCache(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), PrayerCache {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE official_prayer_cache (
                source_id TEXT NOT NULL,
                location_key TEXT NOT NULL,
                prayer_date TEXT NOT NULL,
                fajr INTEGER NOT NULL,
                sunrise INTEGER NOT NULL,
                dhuhr INTEGER NOT NULL,
                asr INTEGER NOT NULL,
                maghrib INTEGER NOT NULL,
                isha INTEGER NOT NULL,
                calculation_profile TEXT NOT NULL,
                fetched_at INTEGER NOT NULL,
                refresh_after INTEGER NOT NULL,
                PRIMARY KEY (source_id, location_key, prayer_date)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Development schema only. Replace with migrations before public release.
        db.execSQL("DROP TABLE IF EXISTS official_prayer_cache")
        onCreate(db)
    }

    override suspend fun official(
        sourceId: String,
        locationKey: String,
        date: LocalDate,
        now: Instant
    ): PrayerDay? {
        readableDatabase.query(
            "official_prayer_cache",
            COLUMNS,
            "source_id = ? AND location_key = ? AND prayer_date = ? AND refresh_after > ?",
            arrayOf(sourceId, locationKey, date.toString(), now.toEpochMilliseconds().toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return PrayerDay(
                date = LocalDate.parse(cursor.getString(cursor.getColumnIndexOrThrow("prayer_date"))),
                fajr = Instant.fromEpochMilliseconds(cursor.getLong(cursor.getColumnIndexOrThrow("fajr"))),
                sunrise = Instant.fromEpochMilliseconds(cursor.getLong(cursor.getColumnIndexOrThrow("sunrise"))),
                dhuhr = Instant.fromEpochMilliseconds(cursor.getLong(cursor.getColumnIndexOrThrow("dhuhr"))),
                asr = Instant.fromEpochMilliseconds(cursor.getLong(cursor.getColumnIndexOrThrow("asr"))),
                maghrib = Instant.fromEpochMilliseconds(cursor.getLong(cursor.getColumnIndexOrThrow("maghrib"))),
                isha = Instant.fromEpochMilliseconds(cursor.getLong(cursor.getColumnIndexOrThrow("isha"))),
                calculationProfile = cursor.getString(cursor.getColumnIndexOrThrow("calculation_profile"))
            )
        }
    }

    override suspend fun putOfficial(
        sourceId: String,
        locationKey: String,
        days: List<PrayerDay>,
        fetchedAt: Instant,
        refreshAfter: Instant
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            days.forEach { day ->
                val values = ContentValues().apply {
                    put("source_id", sourceId)
                    put("location_key", locationKey)
                    put("prayer_date", day.date.toString())
                    put("fajr", day.fajr.toEpochMilliseconds())
                    put("sunrise", day.sunrise.toEpochMilliseconds())
                    put("dhuhr", day.dhuhr.toEpochMilliseconds())
                    put("asr", day.asr.toEpochMilliseconds())
                    put("maghrib", day.maghrib.toEpochMilliseconds())
                    put("isha", day.isha.toEpochMilliseconds())
                    put("calculation_profile", day.calculationProfile)
                    put("fetched_at", fetchedAt.toEpochMilliseconds())
                    put("refresh_after", refreshAfter.toEpochMilliseconds())
                }
                db.insertWithOnConflict("official_prayer_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private companion object {
        const val DATABASE_NAME = "salat_cache.db"
        const val DATABASE_VERSION = 1
        val COLUMNS = arrayOf(
            "prayer_date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha",
            "calculation_profile", "fetched_at", "refresh_after"
        )
    }
}
