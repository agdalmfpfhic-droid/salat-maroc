package ma.salat.maroc.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.astro.PrayerCalculator
import ma.salat.maroc.data.AppSettings
import ma.salat.maroc.data.SettingsRepository
import ma.salat.maroc.data.resolvePlace
import java.time.Instant
import java.time.LocalDate

/**
 * Programme UNE alarme locale : le prochain évènement (prière ou rappel avant la prière).
 * Quand elle sonne, le récepteur affiche la notification puis programme la suivante.
 */
object PrayerScheduler {
    const val ACTION_PRAYER = "ma.salat.maroc.PRAYER_ALARM"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_TIME = "prayer_time"
    const val EXTRA_BEFORE = "minutes_before"
    private const val REQUEST_CODE = 4242

    data class Event(val prayer: Prayer, val prayerTime: Instant, val minutesBefore: Int) {
        val fireAt: Instant get() = prayerTime.minusSeconds(minutesBefore * 60L)
    }

    suspend fun rescheduleFromStorage(context: Context) {
        schedule(context, SettingsRepository(context).current())
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    fun nextEvent(s: AppSettings, after: Instant): Event? {
        if (!s.onboardingDone || !s.notificationsEnabled || s.notifPrayers.isEmpty()) return null
        val place = s.resolvePlace(liveGps = null)
        val zone = s.zone()
        val today = after.atZone(zone).toLocalDate()
        val events = mutableListOf<Event>()
        for (offset in 0L..1L) {
            val date: LocalDate = today.plusDays(offset)
            val times = PrayerCalculator.compute(
                date, place.coords.lat, place.coords.lon, s.method, s.asrSchool, s.adjustments,
            )
            for (p in Prayer.FIVE) {
                if (p !in s.notifPrayers) continue
                events += Event(p, times[p], 0)
                if (s.reminderMinutes > 0) events += Event(p, times[p], s.reminderMinutes)
            }
        }
        return events.filter { it.fireAt.isAfter(after) }.minByOrNull { it.fireAt }
    }

    fun schedule(context: Context, s: AppSettings, after: Instant = Instant.now()) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val baseIntent = Intent(app, PrayerAlarmReceiver::class.java).setAction(ACTION_PRAYER)
        val event = nextEvent(s, after)
        if (event == null) {
            val existing = PendingIntent.getBroadcast(
                app, REQUEST_CODE, baseIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (existing != null) { am.cancel(existing); existing.cancel() }
            return
        }
        val intent = baseIntent
            .putExtra(EXTRA_PRAYER, event.prayer.name)
            .putExtra(EXTRA_TIME, event.prayerTime.toEpochMilli())
            .putExtra(EXTRA_BEFORE, event.minutesBefore)
        val pi = PendingIntent.getBroadcast(
            app, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = event.fireAt.toEpochMilli()
        try {
            if (canScheduleExact(app)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                // Sans l'autorisation « alarmes exactes », Android peut décaler de quelques minutes.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancelAll(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(app, PrayerAlarmReceiver::class.java).setAction(ACTION_PRAYER)
        val pi = PendingIntent.getBroadcast(
            app, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) { am.cancel(pi); pi.cancel() }
    }
}
