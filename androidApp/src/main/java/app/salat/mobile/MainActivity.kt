package app.salat.mobile

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.model.AppearanceMode
import app.salat.model.ResolvedLocation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidLocaleController.apply(this, AndroidAppSettingsStore(this).load().languageTag)
        val resolver = AndroidLocationResolver(this)
        val notificationCoordinator = AndroidPrayerNotificationCoordinator(this)
        val glanceTimelineStore = AndroidGlanceTimelineStore(this)
        val locationStore = AndroidLocationStore(this)
        setContent { SalatApp(resolver, notificationCoordinator, glanceTimelineStore, locationStore) }
    }
}

@Composable
private fun SalatApp(
    resolver: AndroidLocationResolver,
    notificationCoordinator: AndroidPrayerNotificationCoordinator,
    glanceTimelineStore: AndroidGlanceTimelineStore,
    locationStore: AndroidLocationStore
) {
    // Start from the remembered location so the app opens on prayer times instead of
    // the picker; a fresh device fix replaces it when one arrives.
    val context = LocalContext.current
    val appearance = remember { AndroidAppSettingsStore(context).load().appearance }
    var location by remember { mutableStateOf(locationStore.load()) }
    var resolving by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }

    fun applyLocation(resolved: ResolvedLocation?) {
        resolving = false
        if (resolved == null) {
            // A failed fix must not drop a user back to the picker mid-use.
            locationError = location == null
            return
        }
        location = resolved
        locationError = false
        locationStore.save(resolved)
        notificationCoordinator.rebuild(resolved)
        glanceTimelineStore.rebuild(resolved)
    }

    fun resolveDeviceLocation() {
        resolving = true
        locationError = false
        resolver.resolve(::applyLocation)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) resolveDeviceLocation() else locationError = true
    }

    fun requestOrResolveDeviceLocation() {
        if (resolver.hasPermission()) resolveDeviceLocation()
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        // A restored location still needs the widget, watch and alarms rebuilt, since
        // nothing else runs on a launch that reuses it.
        location?.let { restored ->
            notificationCoordinator.rebuild(restored)
            glanceTimelineStore.rebuild(restored)
        }
        if (resolver.hasPermission()) resolveDeviceLocation()
    }

    if (location == null) {
        LocationStartScreen(
            resolving = resolving,
            showError = locationError,
            onUseLocation = ::requestOrResolveDeviceLocation,
            onChooseCity = { showCityPicker = true },
            appearance = appearance
        )
    } else {
        SalatMainShell(
            location = requireNotNull(location),
            onChooseCity = { showCityPicker = true },
            onUseDeviceLocation = ::requestOrResolveDeviceLocation
        )
    }

    if (showCityPicker) {
        AndroidManualCityPicker(
            onSelected = { selected ->
                showCityPicker = false
                applyLocation(selected)
            },
            onUseDeviceLocation = {
                showCityPicker = false
                requestOrResolveDeviceLocation()
            },
            onDismiss = { showCityPicker = false }
        )
    }
}

@Composable
internal fun LocationStartScreen(
    resolving: Boolean,
    showError: Boolean,
    onUseLocation: () -> Unit,
    onChooseCity: () -> Unit,
    appearance: AppearanceMode = AppearanceMode.SYSTEM
) {
    AwqatTheme(appearance) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.padding(horizontal = 26.dp, vertical = 52.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.location_title),
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.location_privacy),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onUseLocation,
                    enabled = !resolving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (resolving) CircularProgressIndicator(color = Color.White)
                    else Text(stringResource(R.string.use_current_location))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onChooseCity,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(stringResource(R.string.location_choose_city))
                }
                if (showError) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.location_unavailable),
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
