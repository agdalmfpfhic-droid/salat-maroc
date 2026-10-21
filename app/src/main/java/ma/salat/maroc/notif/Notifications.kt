package ma.salat.maroc.notif

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ma.salat.maroc.MainActivity
import ma.salat.maroc.R
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.data.AppSettings
import ma.salat.maroc.data.LocationMode
import ma.salat.maroc.data.resolvePlace
import ma.salat.maroc.ui.i18n.Strings
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Notifications 100 % locales (aucun service de push, aucun serveur). */
object Notifications {
    const val CHANNEL_VIBRATE = "prayer_vibrate"
    const val CHANNEL_SILENT = "prayer_no_vibrate"

    /** Crée ou renomme les canaux (appelé au démarrage et quand la langue change). */
    fun createChannels(context: Context, strings: Strings) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val vib = NotificationChannel(CHANNEL_VIBRATE, strings.channelVibrate, NotificationManager.IMPORTANCE_HIGH).apply {
            description = strings.channelDesc
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600)
            setSound(sound, audio)
        }
        val silent = NotificationChannel(CHANNEL_SILENT, strings.channelSilent, NotificationManager.IMPORTANCE_HIGH).apply {
            description = strings.channelDesc
            enableVibration(false)
            vibrationPattern = longArrayOf(0)
            setSound(sound, audio)
        }
        nm.createNotificationChannels(listOf(vib, silent))
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, s: AppSettings, prayer: Prayer, prayerTime: Instant, minutesBefore: Int) {
        val nmc = NotificationManagerCompat.from(context)
        if (!nmc.areNotificationsEnabled()) return
        val strings = Strings.of(s.lang)
        createChannels(context, strings)

        val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(s.zone()).format(prayerTime)
        val name = strings.prayer(prayer)
        val place = s.resolvePlace(null)
        val placeName = when {
            place.mode == LocationMode.GPS -> strings.myPosition
            place.mode == LocationMode.MANUAL -> strings.manualCoordinates
            place.city != null -> strings.city(place.city)
            else -> ""
        }
        val title: String
        val text: String
        if (minutesBefore > 0) {
            title = strings.notifTitleBefore(name, minutesBefore)
            text = strings.notifTextBefore(name, time)
        } else {
            title = strings.notifTitleAt(name)
            text = strings.notifTextAt(time, placeName)
        }

        // À l'heure exacte : adhan si le fichier est présent, l'option activée et le téléphone pas en silencieux.
        if (minutesBefore == 0 && s.adhanEnabled && Adhan.phoneAllowsSound(context) &&
            AdhanService.start(context, prayer, s.lang, title, text, s.vibration)
        ) return

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, if (s.vibration) CHANNEL_VIBRATE else CHANNEL_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            nmc.notify(prayer.ordinal * 10 + if (minutesBefore > 0) 1 else 0, notification)
        } catch (e: SecurityException) {
            // Permission retirée entre-temps : on ignore.
        }
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
