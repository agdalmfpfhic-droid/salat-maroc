package ma.salat.maroc.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.salat.maroc.astro.HijriHelper
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.data.TimeMode
import ma.salat.maroc.notif.PrayerScheduler
import ma.salat.maroc.sensors.GpsStatus
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.components.PlaceCard
import ma.salat.maroc.ui.components.WarningCard
import ma.salat.maroc.ui.i18n.LocalStrings
import ma.salat.maroc.ui.i18n.Strings
import ma.salat.maroc.ui.notificationsAllowed
import ma.salat.maroc.ui.openExactAlarmSettings
import ma.salat.maroc.ui.rememberNotificationPermissionRequester
import ma.salat.maroc.ui.rememberResumeCount
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MOROCCO_GMT_SINCE: Instant = Instant.parse("2026-09-20T02:00:00Z")

/** Le téléphone est-il resté sur GMT+1 alors que le Maroc est revenu à GMT ? */
fun phoneClockLooksOutdated(now: Instant): Boolean {
    val zone = ZoneId.systemDefault()
    if (zone.id != "Africa/Casablanca" && zone.id != "Africa/El_Aaiun") return false
    if (now.isBefore(MOROCCO_GMT_SINCE)) return false
    return zone.rules.getOffset(now).totalSeconds != 0
}

@Composable
fun PrayersScreen(vm: MainViewModel, onChangePlace: () -> Unit) {
    val s: Strings = LocalStrings.current
    val day by vm.day.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val gpsStatus by vm.gpsStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    val askNotif = rememberNotificationPermissionRequester(context)

    val st = day
    if (st == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val settings = st.settings
    val fmt = remember(st.zone) { DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(st.zone) }

    // Demande unique de la permission de notifications (Android 13+).
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.notificationsEnabled) {
        if (!askedOnce && settings.notificationsEnabled && Build.VERSION.SDK_INT >= 33 && !notificationsAllowed(context)) {
            askedOnce = true
            askNotif()
        }
    }
    // Re-programme à chaque retour dans l'app (si l'utilisateur vient d'accorder une permission).
    LaunchedEffect(resumes) { vm.rescheduleNow(); vm.refreshGpsIfNeeded() }

    val notifOk = remember(resumes) { notificationsAllowed(context) }
    val exactOk = remember(resumes) { PrayerScheduler.canScheduleExact(context) }

    val (nextPrayer, nextTime) = st.next(now)
    val remaining = Duration.between(now, nextTime).coerceAtLeast(Duration.ZERO)
    val totalSec = remaining.seconds
    val countdown = String.format(Locale.ROOT, "%02d:%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ----- Avertissements -----
        if (settings.timeMode == TimeMode.AUTO && !settings.tzWarningDismissed && phoneClockLooksOutdated(now)) {
            WarningCard(
                title = s.tzWarningTitle, text = s.tzWarningText,
                actionText = s.useGmt, onAction = { vm.setTimeMode(TimeMode.GMT) },
                secondaryText = s.ignore, onSecondary = { vm.dismissTzWarning() },
            )
        }
        if (settings.notificationsEnabled && !notifOk) {
            WarningCard(title = null, text = s.notifPermissionNeeded, actionText = s.grantNotif, onAction = askNotif)
        } else if (settings.notificationsEnabled && !exactOk) {
            WarningCard(title = null, text = s.exactAlarmNeeded, actionText = s.grantExact, onAction = { openExactAlarmSettings(context) })
        }

        // ----- Lieu + dates -----
        PlaceCard(st.place, s, onChangePlace) {
            Spacer(Modifier.height(4.dp))
            Text(s.formatGregorian(st.date), style = MaterialTheme.typography.bodyLarge)
            HijriHelper.from(st.date, settings.hijriOffset)?.let {
                Text(s.formatHijri(it), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            when {
                gpsStatus == GpsStatus.SEARCHING -> Text(s.gpsSearching, style = MaterialTheme.typography.bodyMedium)
                st.place.gpsMissing -> Text(
                    s.gpsFallback(st.place.city?.let { s.city(it) } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ----- Prochaine prière -----
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = s.countdownA11y(s.prayer(nextPrayer), fmt.format(nextTime), hours, minutes)
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.nextPrayer, style = MaterialTheme.typography.titleMedium)
                Text(s.prayer(nextPrayer), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
                Text(
                    fmt.format(nextTime),
                    style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Ltr),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${s.inLabel}  $countdown",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }

        // ----- Liste des horaires -----
        for (p in listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)) {
            PrayerRow(
                name = s.prayer(p),
                time = fmt.format(st.today[p]),
                isNext = p == nextPrayer && st.today[p].isAfter(now),
                isSunrise = p == Prayer.SUNRISE,
                subtitle = if (p == Prayer.SUNRISE) s.sunriseNote else null,
                adjustment = settings.adjustment(p),
                notifOn = settings.notificationsEnabled && p in settings.notifPrayers,
                strings = s,
                onToggleNotif = {
                    if (!settings.notificationsEnabled) vm.setNotifications(true)
                    vm.togglePrayerNotif(p)
                },
            )
        }

        Text(
            s.methodFooter(s.methodName(settings.method)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrayerRow(
    name: String,
    time: String,
    isNext: Boolean,
    isSunrise: Boolean,
    subtitle: String?,
    adjustment: Int,
    notifOn: Boolean,
    strings: Strings,
    onToggleNotif: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isNext -> colors.primaryContainer
                isSunrise -> colors.surface
                else -> colors.surfaceVariant
            },
        ),
        border = if (isNext) BorderStroke(3.dp, colors.primary) else if (isSunrise) BorderStroke(1.dp, colors.outline) else null,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (isSunrise) 60.dp else 76.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    name,
                    style = if (isSunrise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                    fontWeight = if (isNext) FontWeight.Bold else null,
                )
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                if (adjustment != 0) {
                    Text(
                        (if (adjustment > 0) "+" else "") + "$adjustment ${strings.minutesShort}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondary,
                    )
                }
            }
            Text(
                time,
                style = (if (isSunrise) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
                    .copy(textDirection = TextDirection.Ltr),
                fontWeight = if (isNext) FontWeight.Bold else null,
            )
            if (isSunrise) {
                Spacer(Modifier.size(56.dp))
            } else {
                IconButton(
                    onClick = onToggleNotif,
                    modifier = Modifier
                        .size(64.dp)
                        .semantics {
                            contentDescription = if (notifOn) strings.notifOnA11y(name) else strings.notifOffA11y(name)
                        },
                ) {
                    Icon(
                        if (notifOn) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (notifOn) colors.primary else colors.outline,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}
