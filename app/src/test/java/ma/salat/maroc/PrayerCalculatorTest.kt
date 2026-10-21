package ma.salat.maroc

import ma.salat.maroc.astro.AsrSchool
import ma.salat.maroc.astro.CalcMethod
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.astro.PrayerCalculator
import ma.salat.maroc.astro.Qibla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Compare le calcul hors ligne aux horaires publiés selon le Ministère des Habous
 * (Maroc à l'heure GMT depuis le 20/09/2026). Tolérance : 1 minute.
 */
class PrayerCalculatorTest {

    private fun check(date: LocalDate, lat: Double, lon: Double, expected: Map<Prayer, String>) {
        val t = PrayerCalculator.compute(date, lat, lon, CalcMethod.MOROCCO, AsrSchool.STANDARD)
        for ((p, hhmm) in expected) {
            val exp = date.atTime(LocalTime.parse(hhmm)).toInstant(ZoneOffset.UTC)
            val diff = Duration.between(exp, t[p]).abs().toMinutes()
            assertTrue("$p le $date : attendu $hhmm, obtenu ${t[p]}", diff <= 1)
        }
    }

    @Test
    fun rabat_19_octobre_2026() = check(
        LocalDate.of(2026, 10, 19), 34.0209, -6.8416,
        mapOf(
            Prayer.FAJR to "05:07", Prayer.SUNRISE to "06:33", Prayer.DHUHR to "12:17",
            Prayer.ASR to "15:22", Prayer.MAGHRIB to "17:54", Prayer.ISHA to "19:07",
        ),
    )

    @Test
    fun rabat_31_octobre_2026() = check(
        LocalDate.of(2026, 10, 31), 34.0209, -6.8416,
        mapOf(
            Prayer.FAJR to "05:16", Prayer.SUNRISE to "06:43", Prayer.DHUHR to "12:16",
            Prayer.ASR to "15:13", Prayer.MAGHRIB to "17:41", Prayer.ISHA to "18:55",
        ),
    )

    @Test
    fun casablanca_19_octobre_2026() = check(
        LocalDate.of(2026, 10, 19), 33.5731, -7.5898,
        mapOf(
            Prayer.FAJR to "05:10", Prayer.SUNRISE to "06:35", Prayer.DHUHR to "12:20",
            Prayer.ASR to "15:26", Prayer.MAGHRIB to "17:57", Prayer.ISHA to "19:10",
        ),
    )

    @Test
    fun ajustement_utilisateur() {
        val d = LocalDate.of(2026, 10, 19)
        val base = PrayerCalculator.compute(d, 34.0209, -6.8416, CalcMethod.MOROCCO, AsrSchool.STANDARD)
        val adj = PrayerCalculator.compute(d, 34.0209, -6.8416, CalcMethod.MOROCCO, AsrSchool.STANDARD, mapOf(Prayer.ISHA to 3))
        assertEquals(3, Duration.between(base[Prayer.ISHA], adj[Prayer.ISHA]).toMinutes())
    }

    @Test
    fun qibla_depuis_le_maroc() {
        val casa = Qibla.bearing(33.5731, -7.5898)
        assertTrue("Qibla Casablanca ≈ 93-95°, obtenu $casa", casa in 92.0..96.0)
        assertEquals(2, Qibla.compassPoint(casa)) // Est
        val dakhla = Qibla.bearing(23.6848, -15.9580)
        assertTrue(dakhla in 80.0..90.0)
    }
}
