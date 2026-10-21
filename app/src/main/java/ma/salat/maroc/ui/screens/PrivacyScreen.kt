package ma.salat.maroc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.components.BigButton
import ma.salat.maroc.ui.i18n.LocalStrings

@Composable
fun PrivacyScreen(vm: MainViewModel) {
    val s = LocalStrings.current
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.size(8.dp))
                Text(s.privacyMain, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        s.privacyPoints.forEach { point ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(point, style = MaterialTheme.typography.bodyLarge)
            }
        }
        BigButton(s.clearData, { confirmClear = true }, icon = Icons.Filled.Delete, danger = true)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(s.clearConfirmTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(s.clearConfirmText, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearAllData() }) {
                    Text(s.clearConfirm, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(s.cancel, style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}
