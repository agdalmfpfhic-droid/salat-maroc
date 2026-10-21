package ma.salat.maroc.astro

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Direction de la Qibla par la formule du grand cercle (orthodromie). Calcul local. */
object Qibla {
    const val KAABA_LAT = 21.4225
    const val KAABA_LON = 39.8262

    /** Azimut vrai (0° = Nord géographique, sens horaire) vers la Kaaba. */
    fun bearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLambda = Math.toRadians(KAABA_LON - longitude)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Distance jusqu'à la Kaaba en kilomètres (haversine). */
    fun distanceKm(latitude: Double, longitude: Double): Double {
        val r = 6371.0
        val dPhi = Math.toRadians(KAABA_LAT - latitude)
        val dLambda = Math.toRadians(KAABA_LON - longitude)
        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SO, 6=O, 7=NO. */
    fun compassPoint(degrees: Double): Int {
        val d = ((degrees % 360.0) + 360.0) % 360.0
        return ((d + 22.5) / 45.0).toInt() % 8
    }

    /** Différence signée la plus courte entre deux angles, dans ]-180, 180]. */
    fun signedDelta(from: Double, to: Double): Double {
        var d = (to - from) % 360.0
        if (d > 180.0) d -= 360.0
        if (d <= -180.0) d += 360.0
        return d
    }
}
