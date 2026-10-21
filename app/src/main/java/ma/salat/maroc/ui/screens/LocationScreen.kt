package ma.salat.maroc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.salat.maroc.data.Cities
import ma.salat.maroc.data.LocationMode
import ma.salat.maroc.sensors.GpsStatus
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.components.BigButton
import ma.salat.maroc.ui.i18n.LocalStrings
import ma.salat.maroc.ui.rememberLocationPermissionRequester
import java.text.Normalizer

private fun normalize(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace("-", " ")
        .trim()

/**
 * Choix du lieu : position GPS, ville marocaine dans la liste, ou coordonnées manuelles.
 * [onDone] est appelé dès qu'un lieu est choisi.
 */
@Composable
fun LocationScreen(vm: MainViewModel, onDone: () -> Unit) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val gpsStatus by vm.gpsStatus.collectAsStateWithLifecycle()
    val st = settings ?: return

    var query by rememberSaveable { mutableStateOf("") }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var latText by rememberSaveable { mutableStateOf(st.manual?.lat?.toString() ?: "") }
    var lonText by rememberSaveable { mutableStateOf(st.manual?.lon?.toString() ?: "") }
    var manualError by rememberSaveable { mutableStateOf(false) }

    val requestLocation = rememberLocationPermissionRequester { granted ->
        vm.onLocationPermissionResult(granted)
        if (granted) onDone()
    }

    val sorted = remember(s.lang) { Cities.all.sortedBy { normalize(s.city(it)) } }
    val filtered = remember(query, sorted) {
        val q = normalize(query)
        if (q.isEmpty()) sorted
        else sorted.filter { normalize(it.nameFr).contains(q) || normalize(it.nameAr).contains(q) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            BigButton(
                if (st.locationMode == LocationMode.GPS) s.refreshPosition else s.useMyLocation,
                onClick = requestLocation,
                icon = Icons.Filled.LocationOn,
            )
        }
        item {
            val msg = when (gpsStatus) {
                GpsStatus.SEARCHING -> s.gpsSearching
                GpsStatus.FAILED -> s.gpsFailed
                GpsStatus.DISABLED -> s.gpsDisabled
                GpsStatus.DENIED -> s.gpsDenied
                else -> null
            }
            if (msg != null) Text(msg, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Text(
                s.chooseCity,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(s.searchCity) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
        items(filtered, key = { it.id }) { city ->
            val isSelected = st.locationMode == LocationMode.CITY && st.cityId == city.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { selected = isSelected }
                    .clickable(role = Role.Button) { vm.chooseCity(city.id); onDone() },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.city(city), style = MaterialTheme.typography.titleLarge)
                        // Nom dans l'autre langue, en petit (aide à la reconnaissance).
                        Text(
                            if (s.isRtl) city.nameFr else city.nameAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(30.dp))
                }
            }
        }
        item {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                TextButton(
                    onClick = { showManual = !showManual },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) { Text(s.advanced, style = MaterialTheme.typography.labelLarge) }
            }
        }
        if (showManual) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.coordinatesHelp, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = latText, onValueChange = { latText = it; manualError = false },
                        label = { Text(s.latitude) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = lonText, onValueChange = { lonText = it; manualError = false },
                        label = { Text(s.longitude) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge,
                    )
                    if (manualError) Text(s.invalidCoordinates, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    BigButton(s.ok, onClick = {
                        val lat = latText.trim().replace(',', '.').toDoubleOrNull()
                        val lon = lonText.trim().replace(',', '.').toDoubleOrNull()
                        if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                            manualError = true
                        } else {
                            vm.setManual(lat, lon)
                            onDone()
                        }
                    })
                }
            }
        }
    }
}
