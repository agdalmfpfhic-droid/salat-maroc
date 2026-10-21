package ma.salat.maroc.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.data.SettingsRepository
import java.time.Instant
import kotlin.math.abs

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Reçoit l'alarme : affiche la notification puis programme l'évènement suivant. */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        receiverScope.launch {
            try {
                val s = SettingsRepository(app).current()
                val prayer = intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER)
                    ?.let { n -> Prayer.entries.firstOrNull { it.name == n } }
                val prayerTime = Instant.ofEpochMilli(intent.getLongExtra(PrayerScheduler.EXTRA_TIME, 0L))
                val before = intent.getIntExtra(PrayerScheduler.EXTRA_BEFORE, 0)
                val fireAt = prayerTime.minusSeconds(before * 60L)
                val now = Instant.now()

                if (prayer != null && s.notificationsEnabled && prayer in s.notifPrayers) {
                    // On n'affiche pas une notification arrivée avec plus de 15 min de retard.
                    if (abs(now.toEpochMilli() - fireAt.toEpochMilli()) < 15 * 60_000L) {
                        Notifications.show(app, s, prayer, prayerTime, before)
                    }
                }
                val after = if (fireAt.isAfter(now)) fireAt else now
                PrayerScheduler.schedule(app, s, after.plusSeconds(1))
            } finally {
                pending.finish()
            }
        }
    }
}

/** Après redémarrage, changement d'heure/fuseau ou mise à jour : reprogramme. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        receiverScope.launch {
            try {
                PrayerScheduler.rescheduleFromStorage(app)
            } finally {
                pending.finish()
            }
        }
    }
}
