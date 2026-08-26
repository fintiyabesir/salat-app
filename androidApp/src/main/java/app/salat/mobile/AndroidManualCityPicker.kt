package app.salat.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.salat.location.ManualCity
import app.salat.location.StarterManualCityCatalog
import app.salat.model.ResolvedLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidManualCityPicker(
    onSelected: (ResolvedLocation) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { StarterManualCityCatalog.search(query, 30) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.location_choose_city),
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.location_search_city)) }
            )
            OutlinedButton(
                onClick = onUseDeviceLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.location_use_device))
            }

            if (results.isEmpty()) {
                Text(
                    stringResource(R.string.location_no_city_results),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(results, key = ManualCity::id) { city ->
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
                                        .joinToString(" · ")
                                )
                            }
                            Text(city.countryCode)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
