package ma.salat.maroc.astro

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDay(val day: Int, val month: Int, val year: Int)

/**
 * Date hégirienne calculée hors ligne avec le calendrier intégré à Android (Umm al-Qura).
 * Au Maroc le mois commence après observation du croissant : un décalage de ±1 ou ±2 jours
 * est réglable par l'utilisateur.
 */
object HijriHelper {
    fun from(date: LocalDate, offsetDays: Int): HijriDay? = try {
        val h = HijrahDate.from(date.plusDays(offsetDays.toLong()))
        HijriDay(
            day = h.get(ChronoField.DAY_OF_MONTH),
            month = h.get(ChronoField.MONTH_OF_YEAR),
            year = h.get(ChronoField.YEAR),
        )
    } catch (e: Exception) {
        null
    }
}
