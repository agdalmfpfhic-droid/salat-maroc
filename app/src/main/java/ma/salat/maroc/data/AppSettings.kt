package ma.salat.maroc.data

import ma.salat.maroc.astro.AsrSchool
import ma.salat.maroc.astro.CalcMethod
import ma.salat.maroc.astro.Prayer
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

enum class Lang { FR, AR }
enum class LocationMode { CITY, GPS, MANUAL }
enum class ThemeMode { AUTO, LIGHT, DARK }

/** AUTO = heure du téléphone ; GMT = force l'heure légale marocaine (UTC+0). */
enum class TimeMode { AUTO, GMT }

data class Coordinates(val lat: Double, val lon: Double) {
    /** Arrondi à ~1 km : seule précision conservée sur le téléphone (aucun historique). */
    fun rounded(): Coordinates = Coordinates(
        (lat * 100).roundToInt() / 100.0,
        (lon * 100).roundToInt() / 100.0,
    )
}

/** Réglages enregistrés UNIQUEMENT dans le stockage privé de l'application (DataStore). */
data class AppSettings(
    val onboardingDone: Boolean = false,
    val lang: Lang = Lang.FR,
    val locationMode: LocationMode = LocationMode.CITY,
    val cityId: String = Cities.DEFAULT_ID,
    val manual: Coordinates? = null,
    /** Dernière position GPS (arrondie, une seule valeur écrasée) pour les notifications après redémarrage. */
    val lastGps: Coordinates? = null,
    val method: CalcMethod = CalcMethod.MOROCCO,
    val asrSchool: AsrSchool = AsrSchool.STANDARD,
    val adjustments: Map<Prayer, Int> = emptyMap(),
    val notificationsEnabled: Boolean = true,
    val notifPrayers: Set<Prayer> = Prayer.FIVE.toSet(),
    /** 0 = pas de rappel ; sinon 5, 10, 15, 20 ou 30 minutes avant. */
    val reminderMinutes: Int = 0,
    val vibration: Boolean = true,
    /** Jouer l'adhan à l'heure de la prière (si un fichier adhan est inclus dans l'application). */
    val adhanEnabled: Boolean = true,
    val theme: ThemeMode = ThemeMode.AUTO,
    val timeMode: TimeMode = TimeMode.AUTO,
    val hijriOffset: Int = 0,
    val tzWarningDismissed: Boolean = false,
) {
    fun zone(): ZoneId = if (timeMode == TimeMode.GMT) ZoneOffset.UTC else ZoneId.systemDefault()
    fun adjustment(p: Prayer): Int = adjustments[p] ?: 0

    companion object {
        val REMINDER_CHOICES = listOf(0, 5, 10, 15, 20, 30)
        const val MAX_ADJUST = 30
    }
}

/** Lieu effectivement utilisé pour les calculs. */
data class Place(
    val coords: Coordinates,
    val mode: LocationMode,
    /** Ville choisie (mode CITY) ou ville de repli si le GPS n'a pas encore de position. */
    val city: City?,
    /** Vrai si le mode GPS est demandé mais qu'aucune position n'est encore disponible. */
    val gpsMissing: Boolean = false,
)

fun AppSettings.resolvePlace(liveGps: Coordinates?): Place {
    val city = Cities.byId(cityId)
    return when (locationMode) {
        LocationMode.CITY -> Place(Coordinates(city.lat, city.lon), LocationMode.CITY, city)
        LocationMode.MANUAL -> manual?.let { Place(it, LocationMode.MANUAL, null) }
            ?: Place(Coordinates(city.lat, city.lon), LocationMode.CITY, city)
        LocationMode.GPS -> {
            val c = liveGps ?: lastGps
            if (c != null) Place(c, LocationMode.GPS, null)
            else Place(Coordinates(city.lat, city.lon), LocationMode.GPS, city, gpsMissing = true)
        }
    }
}
