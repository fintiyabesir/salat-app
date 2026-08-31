package app.salat.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.model.ResolvedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidManualCityPicker(
    onSelected: (ResolvedLocation) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val catalog = remember(context) { AndroidOfflineCityCatalog(context.applicationContext) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<OfflineCityEntry>>(emptyList()) }

    LaunchedEffect(query) {
        loading = true
        results = withContext(Dispatchers.IO) { catalog.search(query, 30) }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.location_choose_city),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            // A floating label rides up onto the field's top border, where it
            // collided with the title above it; a placeholder stays inside.
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text(stringResource(R.string.location_search_city)) }
            )
            OutlinedButton(
                onClick = onUseDeviceLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.location_use_device))
            }

            when {
                loading -> CircularProgressIndicator(modifier = Modifier.padding(vertical = 24.dp))
                results.isEmpty() -> Text(
                    stringResource(R.string.location_no_city_results),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(results, key = OfflineCityEntry::id) { city ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onSelected(city.asResolvedLocation()) }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(city.name, fontWeight = FontWeight.Medium)
                                Text(
                                    listOfNotNull(city.regionName, city.countryName)
                                        .distinct()
                                        .joinToString(" · "),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                city.countryCode,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
