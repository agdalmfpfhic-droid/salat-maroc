package ma.salat.maroc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.salat.maroc.astro.AsrSchool
import ma.salat.maroc.astro.CalcMethod
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.data.AppSettings
import ma.salat.maroc.data.Lang
import ma.salat.maroc.data.ThemeMode
import ma.salat.maroc.data.TimeMode
import ma.salat.maroc.data.resolvePlace
import ma.salat.maroc.notif.Adhan
import ma.salat.maroc.notif.AdhanService
import ma.salat.maroc.notif.PrayerScheduler
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.components.BigButton
import ma.salat.maroc.ui.components.ChoiceButton
import ma.salat.maroc.ui.components.PlaceCard
import ma.salat.maroc.ui.components.SectionCard
import ma.salat.maroc.ui.components.SectionTitle
import ma.salat.maroc.ui.components.SwitchRow
import ma.salat.maroc.ui.components.WarningCard
import ma.salat.maroc.ui.i18n.LocalStrings
import ma.salat.maroc.ui.notificationsAllowed
import ma.salat.maroc.ui.openExactAlarmSettings
import ma.salat.maroc.ui.rememberNotificationPermissionRequester
import ma.salat.maroc.ui.rememberResumeCount

@Composable
fun SettingsScreen(vm: MainViewModel, onChangePlace: () -> Unit, onOpenPrivacy: () -> Unit) {
    val s = LocalStrings.current
    val settingsState by vm.settings.collectAsStateWithLifecycle()
    val st: AppSettings = settingsState ?: return
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    val notifOk = remember(resumes) { notificationsAllowed(context) }
    val exactOk = remember(resumes) { PrayerScheduler.canScheduleExact(context) }
    val askNotif = rememberNotificationPermissionRequester(context)
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val adhanAvailable = remember { Adhan.isAvailable(context) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ----- Langue -----
        SectionTitle(s.secLanguage)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceButton("العربية", st.lang == Lang.AR, { vm.setLang(Lang.AR) }, Modifier.weight(1f))
            ChoiceButton("Français", st.lang == Lang.FR, { vm.setLang(Lang.FR) }, Modifier.weight(1f))
        }

        // ----- Lieu -----
        SectionTitle(s.secLocation)
        PlaceCard(st.resolvePlace(null), s, onChangePlace)

        // ----- Méthode -----
        // Méthode fixe : Ministère des Habous, Asr maliki (pas de choix pour l'utilisateur).
        SectionTitle(s.secMethod)
        SectionCard {
            Text(s.methodName(CalcMethod.MOROCCO), style = MaterialTheme.typography.titleMedium)
            Text(s.methodDetail(CalcMethod.MOROCCO) + " · " + s.asrName(AsrSchool.STANDARD), style = MaterialTheme.typography.bodyMedium)
        }

        // ----- Notifications -----
        SectionTitle(s.secNotifications)
        SectionCard {
            SwitchRow(s.notifMaster, st.notificationsEnabled, { vm.setNotifications(it) })
            if (st.notificationsEnabled) {
                if (!notifOk) {
                    WarningCard(title = null, text = s.notifPermissionNeeded, actionText = s.grantNotif, onAction = askNotif)
                } else if (!exactOk) {
                    WarningCard(title = null, text = s.exactAlarmNeeded, actionText = s.grantExact, onAction = { openExactAlarmSettings(context) })
                }
                Text(s.notifWhich, style = MaterialTheme.typography.titleMedium)
                Prayer.FIVE.forEach { p ->
                    SwitchRow(s.prayer(p), p in st.notifPrayers, { vm.togglePrayerNotif(p) })
                }
                SwitchRow(s.vibration, st.vibration, { vm.setVibration(it) })
                // Option visible seulement si un fichier adhan est inclus dans l'application.
                if (adhanAvailable) {
                    SwitchRow(s.adhanSwitch, st.adhanEnabled, { vm.setAdhan(it) })
                    if (st.adhanEnabled) {
                        Text(s.adhanSilentNote, style = MaterialTheme.typography.bodySmall)
                        BigButton(s.adhanListen, {
                            AdhanService.start(context, Prayer.DHUHR, st.lang, s.adhanTest, "", st.vibration)
                        }, icon = Icons.Filled.PlayArrow, outlined = true)
                    }
                }
            }
        }

        // ----- Affichage -----
        SectionTitle(s.secDisplay)
        ThemeMode.entries.forEach { t ->
            val label = when (t) {
                ThemeMode.AUTO -> s.themeAuto
                ThemeMode.LIGHT -> s.themeLight
                ThemeMode.DARK -> s.themeDark
            }
            ChoiceButton(label, st.theme == t, { vm.setTheme(t) })
        }

        // ----- Heure -----
        SectionTitle(s.secTime)
        Text(s.timeHelp, style = MaterialTheme.typography.bodyMedium)
        ChoiceButton(s.timeAuto, st.timeMode == TimeMode.AUTO, { vm.setTimeMode(TimeMode.AUTO) })
        ChoiceButton(s.timeGmt, st.timeMode == TimeMode.GMT, { vm.setTimeMode(TimeMode.GMT) })

        // ----- Hégire -----
        SectionTitle(s.secHijri)
        SectionCard {
            Text(s.hijriHelp, style = MaterialTheme.typography.bodyMedium)
            AdjustRow(
                label = s.secHijri,
                valueText = s.days(st.hijriOffset),
                onMinus = { vm.setHijriOffset(st.hijriOffset - 1) },
                onPlus = { vm.setHijriOffset(st.hijriOffset + 1) },
                minusA11y = "−1",
                plusA11y = "+1",
            )
        }

        // ----- Confidentialité -----
        SectionTitle(s.secPrivacy)
        BigButton(s.privacyOpen, onOpenPrivacy, icon = Icons.Filled.Lock, outlined = true)
        BigButton(s.clearData, { confirmClear = true }, icon = Icons.Filled.Delete, danger = true)
        Text(
            "Salat Maroc 1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
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
                TextButton(onClick = { confirmClear = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

private fun formatMinutes(v: Int, unit: String): String = when {
    v > 0 -> "+$v $unit"
    else -> "$v $unit"
}

/** Ligne « − valeur + » avec de grosses touches. */
@Composable
fun AdjustRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusA11y: String,
    plusA11y: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalButton(
            onClick = onMinus,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(60.dp)
                .semantics { contentDescription = minusA11y },
        ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        Text(
            valueText,
            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 96.dp),
        )
        FilledTonalButton(
            onClick = onPlus,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(60.dp)
                .semantics { contentDescription = plusA11y },
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
}
