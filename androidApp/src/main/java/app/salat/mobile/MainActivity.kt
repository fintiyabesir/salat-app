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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.model.ResolvedLocation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidLocaleController.apply(this, AndroidAppSettingsStore(this).load().languageTag)
        val resolver = AndroidLocationResolver(this)
        val notificationCoordinator = AndroidPrayerNotificationCoordinator(this)
        setContent { SalatApp(resolver, notificationCoordinator) }
    }
}

private val Canvas = Color(0xFFFAF8F3)
private val Sage = Color(0xFF467A69)

@Composable
private fun SalatApp(
    resolver: AndroidLocationResolver,
    notificationCoordinator: AndroidPrayerNotificationCoordinator
) {
    var location by remember { mutableStateOf<ResolvedLocation?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }

    fun resolve() {
        resolving = true
        locationError = false
        resolver.resolve { resolved ->
            location = resolved
            resolving = false
            locationError = resolved == null
            if (resolved != null) notificationCoordinator.rebuild(resolved)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) resolve() else locationError = true
    }

    LaunchedEffect(Unit) {
        if (resolver.hasPermission()) resolve()
    }

    if (location == null) {
        LocationStartScreen(
            resolving = resolving,
            showError = locationError,
            onUseLocation = {
                if (resolver.hasPermission()) resolve()
                else permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    } else {
        SalatMainShell(requireNotNull(location))
    }
}

@Composable
private fun LocationStartScreen(
    resolving: Boolean,
    showError: Boolean,
    onUseLocation: () -> Unit
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(
                Modifier.padding(horizontal = 26.dp, vertical = 52.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.brand_name),
                    color = Sage,
                    fontSize = 14.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.location_title),
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.location_privacy),
                    color = Color(0xFF6D716E),
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onUseLocation,
                    enabled = !resolving,
                    colors = ButtonDefaults.buttonColors(containerColor = Sage),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (resolving) CircularProgressIndicator(color = Color.White)
                    else Text(stringResource(R.string.use_current_location))
                }
                if (showError) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.location_unavailable),
                        color = Color(0xFF9A5B45),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
