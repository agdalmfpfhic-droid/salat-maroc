package ma.salat.maroc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ma.salat.maroc.astro.AsrSchool
import ma.salat.maroc.astro.CalcMethod
import ma.salat.maroc.astro.Prayer
import java.io.IOException
import java.util.Locale

/** Fichier privé : /data/data/ma.salat.maroc/files/datastore/settings.preferences_pb */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    /** « Effacer mes données » : supprime tous les réglages locaux. */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val lang = stringPreferencesKey("lang")
        val locMode = stringPreferencesKey("location_mode")
        val city = stringPreferencesKey("city_id")
        val manualLat = doublePreferencesKey("manual_lat")
        val manualLon = doublePreferencesKey("manual_lon")
        val gpsLat = doublePreferencesKey("gps_lat")
        val gpsLon = doublePreferencesKey("gps_lon")
        val method = stringPreferencesKey("method")
        val asr = stringPreferencesKey("asr_school")
        val notif = booleanPreferencesKey("notifications")
        val notifPrayers = stringSetPreferencesKey("notif_prayers")
        val reminder = intPreferencesKey("reminder_minutes")
        val vibration = booleanPreferencesKey("vibration")
        val adhan = booleanPreferencesKey("adhan")
        val theme = stringPreferencesKey("theme")
        val timeMode = stringPreferencesKey("time_mode")
        val hijri = intPreferencesKey("hijri_offset")
        val tzDismissed = booleanPreferencesKey("tz_warning_dismissed")
        fun adj(p: Prayer) = intPreferencesKey("adj_" + p.name.lowercase(Locale.ROOT))
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

    private fun defaultLang(): Lang =
        if (Locale.getDefault().language == "ar") Lang.AR else Lang.FR

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        val mLat = this[K.manualLat]
        val mLon = this[K.manualLon]
        val gLat = this[K.gpsLat]
        val gLon = this[K.gpsLon]
        return AppSettings(
            onboardingDone = this[K.onboarding] ?: false,
            lang = enumOr(this[K.lang], defaultLang()),
            locationMode = enumOr(this[K.locMode], d.locationMode),
            cityId = this[K.city] ?: d.cityId,
            manual = if (mLat != null && mLon != null) Coordinates(mLat, mLon) else null,
            lastGps = if (gLat != null && gLon != null) Coordinates(gLat, gLon) else null,
            method = enumOr(this[K.method], d.method),
            asrSchool = enumOr(this[K.asr], d.asrSchool),
            adjustments = Prayer.entries.associateWith { this[K.adj(it)] ?: 0 }.filterValues { it != 0 },
            notificationsEnabled = this[K.notif] ?: d.notificationsEnabled,
            notifPrayers = this[K.notifPrayers]
                ?.mapNotNull { n -> Prayer.entries.firstOrNull { it.name == n } }?.toSet()
                ?: d.notifPrayers,
            reminderMinutes = this[K.reminder] ?: d.reminderMinutes,
            vibration = this[K.vibration] ?: d.vibration,
            adhanEnabled = this[K.adhan] ?: d.adhanEnabled,
            theme = enumOr(this[K.theme], d.theme),
            timeMode = enumOr(this[K.timeMode], d.timeMode),
            hijriOffset = this[K.hijri] ?: d.hijriOffset,
            tzWarningDismissed = this[K.tzDismissed] ?: false,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.onboarding] = s.onboardingDone
        this[K.lang] = s.lang.name
        this[K.locMode] = s.locationMode.name
        this[K.city] = s.cityId
        if (s.manual != null) {
            this[K.manualLat] = s.manual.lat; this[K.manualLon] = s.manual.lon
        } else {
            remove(K.manualLat); remove(K.manualLon)
        }
        if (s.lastGps != null) {
            this[K.gpsLat] = s.lastGps.lat; this[K.gpsLon] = s.lastGps.lon
        } else {
            remove(K.gpsLat); remove(K.gpsLon)
        }
        this[K.method] = s.method.name
        this[K.asr] = s.asrSchool.name
        Prayer.entries.forEach { p ->
            val v = s.adjustment(p)
            if (v != 0) this[K.adj(p)] = v else remove(K.adj(p))
        }
        this[K.notif] = s.notificationsEnabled
        this[K.notifPrayers] = s.notifPrayers.map { it.name }.toSet()
        this[K.reminder] = s.reminderMinutes
        this[K.vibration] = s.vibration
        this[K.adhan] = s.adhanEnabled
        this[K.theme] = s.theme.name
        this[K.timeMode] = s.timeMode.name
        this[K.hijri] = s.hijriOffset
        this[K.tzDismissed] = s.tzWarningDismissed
    }
}
