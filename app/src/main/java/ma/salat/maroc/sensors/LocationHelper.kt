package ma.salat.maroc.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import ma.salat.maroc.data.Coordinates

enum class GpsStatus { IDLE, SEARCHING, OK, FAILED, DISABLED, DENIED }

/**
 * Position via le LocationManager d'Android (GPS du téléphone, fonctionne sans Internet).
 * Aucun service Google, aucun envoi : la position est simplement rendue à l'appelant.
 */
object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun granted(context: Context, p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    /** Appelle [onResult] une seule fois, sur le thread principal (null si échec). */
    @SuppressLint("MissingPermission")
    fun getLocation(context: Context, onResult: (Coordinates?, GpsStatus) -> Unit) {
        if (!hasPermission(context)) { onResult(null, GpsStatus.DENIED); return }
        val lm = context.getSystemService(LocationManager::class.java)
        if (lm == null || !LocationManagerCompat.isLocationEnabled(lm)) { onResult(null, GpsStatus.DISABLED); return }

        val fine = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val providers = buildList {
            if (fine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
        }

        val last: Location? = (providers + LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }

        // Une position de moins de 30 minutes suffit largement pour des horaires de prière.
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60_000L) {
            onResult(Coordinates(last.latitude, last.longitude), GpsStatus.OK); return
        }
        if (providers.isEmpty()) {
            if (last != null) onResult(Coordinates(last.latitude, last.longitude), GpsStatus.OK)
            else onResult(null, GpsStatus.FAILED)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        val signals = mutableListOf<CancellationSignal>()
        var done = false
        // Termine une seule fois : à la première position reçue, ou après 45 s (dernière position connue).
        fun finish(loc: Location?) {
            if (done) return
            done = true
            signals.forEach { runCatching { it.cancel() } }
            handler.removeCallbacksAndMessages(null)
            val chosen = loc ?: last
            if (chosen != null) onResult(Coordinates(chosen.latitude, chosen.longitude), GpsStatus.OK)
            else onResult(null, GpsStatus.FAILED)
        }
        handler.postDelayed({ finish(null) }, 45_000L)

        val executor = ContextCompat.getMainExecutor(context)
        providers.forEach { provider ->
            val signal = CancellationSignal()
            signals += signal
            LocationManagerCompat.getCurrentLocation(lm, provider, signal, executor) { loc: Location? ->
                if (loc != null) finish(loc)
            }
        }
    }
}
