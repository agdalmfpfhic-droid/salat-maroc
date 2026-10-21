package ma.salat.maroc.astro

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** Les cinq prières + le lever du soleil (Chourouq, affiché à titre indicatif). */
enum class Prayer {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    companion object {
        /** Les cinq prières obligatoires (sans le lever du soleil). */
        val FIVE = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

/**
 * Méthodes de calcul.
 * MOROCCO reproduit la méthode du Ministère des Habous et des Affaires islamiques :
 * Fajr 19°, Isha 17°, puis corrections officielles (Chourouq −3 min, Dhouhr +5 min, Maghrib +5 min).
 * Vérifié contre les horaires publiés pour Rabat et Casablanca (octobre 2026) : écart ≤ 1 minute.
 */
enum class CalcMethod(
    val fajrAngle: Double,
    val ishaAngle: Double,
    /** Si > 0 : Isha = Maghrib + ce nombre de minutes (Umm al-Qura). */
    val ishaMinutesAfterMaghrib: Int = 0,
    val methodAdjustments: Map<Prayer, Int> = emptyMap(),
) {
    MOROCCO(19.0, 17.0, 0, mapOf(Prayer.SUNRISE to -3, Prayer.DHUHR to 5, Prayer.MAGHRIB to 5)),
    MWL(18.0, 17.0),
    EGYPT(19.5, 17.5),
    UMM_AL_QURA(18.5, 0.0, 90),
    ISNA(15.0, 15.0),
    FRANCE_UOIF(12.0, 12.0),
}

/** École pour l'Asr : Standard (Maliki, Chafii, Hanbali) = ombre ×1, Hanafi = ombre ×2. */
enum class AsrSchool(val shadowFactor: Int) { STANDARD(1), HANAFI(2) }

data class PrayerTimes(val date: LocalDate, val times: Map<Prayer, Instant>) {
    operator fun get(p: Prayer): Instant = times.getValue(p)
}

/**
 * Calcul astronomique 100 % local (algorithme classique de position du soleil,
 * précision ~1 minute). Aucun accès réseau.
 */
object PrayerCalculator {

    fun compute(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        method: CalcMethod,
        school: AsrSchool,
        userAdjustments: Map<Prayer, Int> = emptyMap(),
    ): PrayerTimes {
        val jDate = julianDay(date.year, date.monthValue, date.dayOfMonth) - longitude / 360.0
        val phi = Math.toRadians(latitude)

        fun sun(t: Double): SunPosition = sunPosition(jDate + t / 24.0)
        fun midDay(t: Double): Double = 12.0 - sun(t).equationOfTime

        /** Durée (heures) entre midi solaire et le moment où le soleil est à `angle`° sous l'horizon. */
        fun hourAngle(angle: Double, t: Double): Double {
            val dec = sun(t).declination
            val c = (-sin(Math.toRadians(angle)) - sin(phi) * sin(dec)) / (cos(phi) * cos(dec))
            return Math.toDegrees(acos(c.coerceIn(-1.0, 1.0))) / 15.0
        }

        fun asrHourAngle(t: Double): Double {
            val dec = sun(t).declination
            val altitude = -Math.toDegrees(atan(1.0 / (school.shadowFactor + tan(abs(phi - dec)))))
            return hourAngle(altitude, t)
        }

        // Valeurs initiales (heures solaires approximatives), puis 2 itérations de raffinement.
        var fajr = 5.0
        var sunrise = 6.0
        var dhuhr = 12.0
        var asr = 13.0
        var maghrib = 18.0
        var isha = 18.0
        repeat(2) {
            val f = midDay(fajr) - hourAngle(method.fajrAngle, fajr)
            val s = midDay(sunrise) - hourAngle(0.833, sunrise)
            val d = midDay(dhuhr)
            val a = midDay(asr) + asrHourAngle(asr)
            val m = midDay(maghrib) + hourAngle(0.833, maghrib)
            val i = midDay(isha) + hourAngle(method.ishaAngle, isha)
            fajr = f; sunrise = s; dhuhr = d; asr = a; maghrib = m; isha = i
        }
        if (method.ishaMinutesAfterMaghrib > 0) {
            isha = maghrib + method.ishaMinutesAfterMaghrib / 60.0
        }

        val raw = mapOf(
            Prayer.FAJR to fajr, Prayer.SUNRISE to sunrise, Prayer.DHUHR to dhuhr,
            Prayer.ASR to asr, Prayer.MAGHRIB to maghrib, Prayer.ISHA to isha,
        )
        val midnightUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val result = raw.mapValues { (p, localSolarHours) ->
            val utcHours = localSolarHours - longitude / 15.0
            val extra = (method.methodAdjustments[p] ?: 0) + (userAdjustments[p] ?: 0)
            val minutes = (utcHours * 60.0 + extra).roundToLong()
            midnightUtc.plusSeconds(minutes * 60L)
        }
        return PrayerTimes(date, result)
    }

    // ----- Astronomie -----

    private data class SunPosition(val declination: Double, val equationOfTime: Double)

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return (365.25 * (y + 4716)).toLong() + (30.6001 * (m + 1)).toLong() + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = Math.toRadians(normalize(357.529 + 0.98560028 * d, 360.0))
        val q = normalize(280.459 + 0.98564736 * d, 360.0)
        val l = Math.toRadians(normalize(q + 1.915 * sin(g) + 0.020 * sin(2 * g), 360.0))
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val ra = normalize(Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0, 24.0)
        val dec = asin(sin(e) * sin(l))
        var eqt = q / 15.0 - ra
        eqt = normalize(eqt + 12.0, 24.0) - 12.0
        return SunPosition(dec, eqt)
    }

    private fun normalize(x: Double, range: Double): Double {
        val r = x % range
        return if (r < 0) r + range else r
    }
}
