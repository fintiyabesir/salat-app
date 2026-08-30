package app.salat.mobile

import android.content.Context
import app.salat.model.GeoPoint
import app.salat.model.LocationSource
import app.salat.model.ResolvedLocation

/**
 * Remembers the location the app is currently using.
 *
 * Without this the app reopens on the location picker every launch and only then
 * re-resolves, which means a user who declined location access has to choose their
 * city again every single time.
 */
class AndroidLocationStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): ResolvedLocation? {
        val timeZoneId = prefs.getString(KEY_TIME_ZONE, null) ?: return null
        if (!prefs.contains(KEY_LATITUDE) || !prefs.contains(KEY_LONGITUDE)) return null
        return ResolvedLocation(
            point = GeoPoint(
                latitude = prefs.getFloat(KEY_LATITUDE, 0f).toDouble(),
                longitude = prefs.getFloat(KEY_LONGITUDE, 0f).toDouble()
            ),
            timeZoneId = timeZoneId,
            countryCode = prefs.getString(KEY_COUNTRY, null),
            cityName = prefs.getString(KEY_CITY, null),
            regionName = prefs.getString(KEY_REGION, null),
            source = runCatching {
                LocationSource.valueOf(prefs.getString(KEY_SOURCE, null) ?: "")
            }.getOrDefault(LocationSource.DEVICE)
        )
    }

    fun save(location: ResolvedLocation) {
        prefs.edit()
            .putFloat(KEY_LATITUDE, location.point.latitude.toFloat())
            .putFloat(KEY_LONGITUDE, location.point.longitude.toFloat())
            .putString(KEY_TIME_ZONE, location.timeZoneId)
            .putString(KEY_COUNTRY, location.countryCode)
            .putString(KEY_CITY, location.cityName)
            .putString(KEY_REGION, location.regionName)
            .putString(KEY_SOURCE, location.source.name)
            .apply()
    }

    private companion object {
        const val PREFS = "awqat-location-v1"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_TIME_ZONE = "timeZoneId"
        const val KEY_COUNTRY = "countryCode"
        const val KEY_CITY = "cityName"
        const val KEY_REGION = "regionName"
        const val KEY_SOURCE = "source"
    }
}
