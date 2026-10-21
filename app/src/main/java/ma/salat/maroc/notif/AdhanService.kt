package ma.salat.maroc.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ma.salat.maroc.MainActivity
import ma.salat.maroc.R
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.data.Lang
import ma.salat.maroc.ui.i18n.Strings

/**
 * Lecture locale de l'adhan (fichier inclus dans l'application, aucun téléchargement).
 *
 * Pour activer l'adhan, placez simplement :
 *   app/src/main/res/raw/adhan.mp3        (obligatoire)
 *   app/src/main/res/raw/adhan_fajr.mp3   (facultatif : adhan spécial du Fajr)
 * Le code détecte ces fichiers par leur nom : sans fichier, l'application compile et
 * utilise le son de notification du téléphone.
 */
class AdhanService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        val prayer = intent?.getStringExtra(EXTRA_PRAYER)?.let { n -> Prayer.entries.firstOrNull { it.name == n } }
        val lang = intent?.getStringExtra(EXTRA_LANG)?.let { n -> Lang.entries.firstOrNull { it.name == n } } ?: Lang.FR
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: Strings.of(lang).adhanTest
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: ""
        val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
        val strings = Strings.of(lang)

        createChannels(this, strings)
        val notification = buildNotification(title, text, strings, vibrate)
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification,
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (e: Exception) {
            // Android a refusé le service : on se contente d'une notification normale.
            stopSelf()
            return START_NOT_STICKY
        }

        val resId = Adhan.resourceFor(this, prayer)
        if (resId == 0) { stopEverything(); return START_NOT_STICKY }

        player?.release()
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener { stopEverything() }
                setOnErrorListener { _, _, _ -> stopEverything(); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopEverything()
            null
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, text: String, s: Strings, vibrate: Boolean) =
        NotificationCompat.Builder(this, if (vibrate) CHANNEL_ADHAN_VIBRATE else CHANNEL_ADHAN_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            // Gros bouton « Arrêter l'adhan » dans la notification.
            .addAction(0, s.stopAdhan, stopIntent(this))
            .setDeleteIntent(stopIntent(this))
            .build()

    private fun stopEverything() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "ma.salat.maroc.STOP_ADHAN"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_LANG = "lang"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_VIBRATE = "vibrate"
        private const val NOTIF_ID = 777
        const val CHANNEL_ADHAN_VIBRATE = "adhan_vibrate"
        const val CHANNEL_ADHAN_SILENT = "adhan_no_vibrate"

        /** Canaux sans son système : c'est le MediaPlayer qui joue l'adhan. */
        fun createChannels(context: Context, s: Strings) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val vib = NotificationChannel(CHANNEL_ADHAN_VIBRATE, s.channelAdhan, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600)
            }
            val silent = NotificationChannel(CHANNEL_ADHAN_SILENT, s.channelAdhan + " ·", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannels(listOf(vib, silent))
        }

        private fun stopIntent(context: Context): PendingIntent = PendingIntent.getService(
            context, 2, Intent(context, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** Lance l'adhan. Retourne false si impossible (le son normal sera alors utilisé). */
        fun start(
            context: Context, prayer: Prayer?, lang: Lang, title: String, text: String, vibrate: Boolean,
        ): Boolean {
            if (!Adhan.isAvailable(context)) return false
            val intent = Intent(context, AdhanService::class.java)
                .putExtra(EXTRA_PRAYER, prayer?.name)
                .putExtra(EXTRA_LANG, lang.name)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_VIBRATE, vibrate)
            return try {
                ContextCompat.startForegroundService(context, intent); true
            } catch (e: Exception) {
                false
            }
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, AdhanService::class.java).setAction(ACTION_STOP)) }
        }
    }
}

/** Détection des fichiers d'adhan inclus dans l'application (res/raw). */
object Adhan {
    private fun id(context: Context, name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    fun isAvailable(context: Context): Boolean = id(context, "adhan") != 0

    /** Adhan du Fajr s'il existe, sinon l'adhan normal. */
    fun resourceFor(context: Context, prayer: Prayer?): Int {
        if (prayer == Prayer.FAJR) id(context, "adhan_fajr").let { if (it != 0) return it }
        return id(context, "adhan")
    }

    /** Respecte le mode silencieux / vibreur du téléphone (ex. à la mosquée). */
    fun phoneAllowsSound(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return true
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
