package app.salat.mobile

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import app.salat.model.GeoPoint
import app.salat.model.LocationSource
import app.salat.model.ResolvedLocation
import java.time.ZoneId
import java.util.Locale

/**
 * Uses Android platform services only: no Google Play Services SDK and no Salat backend.
 */
class AndroidLocationResolver(private val activity: Activity) {
    private val manager = activity.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun resolve(onResult: (ResolvedLocation?) -> Unit) {
        if (!hasPermission()) {
            onResult(null)
            return
        }

        val lastKnown = runCatching {
            manager.getProviders(true)
                .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull(Location::getTime)
        }.getOrNull()

        if (lastKnown != null) {
            enrich(lastKnown, onResult)
            return
        }

        val provider = when {
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
            else -> null
        }

        if (provider == null) {
            onResult(null)
            return
        }

        @Suppress("DEPRECATION")
        runCatching {
            manager.requestSingleUpdate(
                provider,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) = enrich(location, onResult)
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Legacy callback")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                },
                Looper.getMainLooper()
            )
        }.onFailure { onResult(null) }
    }

    private fun enrich(location: Location, onResult: (ResolvedLocation?) -> Unit) {
        val base = ResolvedLocation(
            point = GeoPoint(location.latitude, location.longitude),
            timeZoneId = ZoneId.systemDefault().id,
            source = LocationSource.DEVICE
        )

        if (!Geocoder.isPresent()) {
            onResult(base)
            return
        }

        val geocoder = Geocoder(activity, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        onResult(base.withAddress(addresses.firstOrNull()))
                    }

                    override fun onError(errorMessage: String?) {
                        onResult(base)
                    }
                }
            )
        } else {
            Thread {
                @Suppress("DEPRECATION")
                val address = runCatching {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                }.getOrNull()
                activity.runOnUiThread { onResult(base.withAddress(address)) }
            }.start()
        }
    }

    private fun ResolvedLocation.withAddress(address: Address?): ResolvedLocation = copy(
        countryCode = address?.countryCode?.uppercase(Locale.ROOT),
        cityName = address?.locality ?: address?.subAdminArea ?: address?.adminArea,
        regionName = address?.adminArea
    )
}
