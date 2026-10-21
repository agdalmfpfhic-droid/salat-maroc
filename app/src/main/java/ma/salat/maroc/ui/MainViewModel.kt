package ma.salat.maroc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.salat.maroc.astro.AsrSchool
import ma.salat.maroc.astro.CalcMethod
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.astro.PrayerCalculator
import ma.salat.maroc.astro.PrayerTimes
import ma.salat.maroc.data.AppSettings
import ma.salat.maroc.data.Coordinates
import ma.salat.maroc.data.Lang
import ma.salat.maroc.data.LocationMode
import ma.salat.maroc.data.Place
import ma.salat.maroc.data.SettingsRepository
import ma.salat.maroc.data.ThemeMode
import ma.salat.maroc.data.TimeMode
import ma.salat.maroc.data.resolvePlace
import ma.salat.maroc.notif.AdhanService
import ma.salat.maroc.notif.Notifications
import ma.salat.maroc.notif.PrayerScheduler
import ma.salat.maroc.sensors.GpsStatus
import ma.salat.maroc.sensors.LocationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Tout ce qu'il faut pour afficher une journée. */
data class DayState(
    val settings: AppSettings,
    val place: Place,
    val date: LocalDate,
    val zone: ZoneId,
    val today: PrayerTimes,
    val tomorrow: PrayerTimes,
) {
    /** Prochaine des cinq prières (après Isha : Fajr du lendemain). */
    fun next(now: Instant): Pair<Prayer, Instant> {
        for (p in Prayer.FIVE) if (today[p].isAfter(now)) return p to today[p]
        return Prayer.FAJR to tomorrow[Prayer.FAJR]
    }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    val settings: StateFlow<AppSettings?> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Position GPS précise : gardée UNIQUEMENT en mémoire (perdue à la fermeture). */
    private val liveGps = MutableStateFlow<Coordinates?>(null)

    private val _gpsStatus = MutableStateFlow(GpsStatus.IDLE)
    val gpsStatus: StateFlow<GpsStatus> = _gpsStatus.asStateFlow()

    /** Horloge qui avance chaque seconde (compte à rebours). */
    val now: StateFlow<Instant> = flow {
        while (true) {
            val n = Instant.now()
            emit(n)
            delay(1000L - n.toEpochMilli() % 1000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Instant.now())

    val day: StateFlow<DayState?> =
        combine(settings.filterNotNull(), liveGps, now) { s, gps, n ->
            Triple(s, gps, n.atZone(s.zone()).toLocalDate())
        }
            .distinctUntilChanged()
            .map { (s, gps, date) ->
                val place = s.resolvePlace(gps)
                val c = place.coords
                DayState(
                    settings = s,
                    place = place,
                    date = date,
                    zone = s.zone(),
                    today = PrayerCalculator.compute(date, c.lat, c.lon, s.method, s.asrSchool, s.adjustments),
                    tomorrow = PrayerCalculator.compute(date.plusDays(1), c.lat, c.lon, s.method, s.asrSchool, s.adjustments),
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Reprogramme la prochaine notification à chaque changement de réglage.
        viewModelScope.launch {
            settings.filterNotNull().distinctUntilChanged().collectLatest { s ->
                delay(400)
                withContext(Dispatchers.Default) { PrayerScheduler.schedule(getApplication(), s) }
            }
        }
    }

    private fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(block) }
    }

    // ---------- Lieu ----------

    fun refreshGps() {
        val ctx = getApplication<Application>()
        _gpsStatus.value = GpsStatus.SEARCHING
        LocationHelper.getLocation(ctx) { coords, status ->
            _gpsStatus.value = status
            if (coords != null) {
                liveGps.value = coords
                // Une seule valeur arrondie (~1 km), écrasée à chaque fois : sert aux notifications après redémarrage.
                update { it.copy(lastGps = coords.rounded()) }
            }
        }
    }

    /** Appelé après la demande de permission de localisation. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            update { it.copy(locationMode = LocationMode.GPS) }
            refreshGps()
        } else {
            _gpsStatus.value = GpsStatus.DENIED
        }
    }

    fun refreshGpsIfNeeded() {
        val s = settings.value ?: return
        if (s.locationMode == LocationMode.GPS && liveGps.value == null &&
            _gpsStatus.value != GpsStatus.SEARCHING && LocationHelper.hasPermission(getApplication())
        ) refreshGps()
    }

    fun chooseCity(id: String) = update { it.copy(locationMode = LocationMode.CITY, cityId = id) }

    fun setManual(lat: Double, lon: Double) =
        update { it.copy(locationMode = LocationMode.MANUAL, manual = Coordinates(lat, lon)) }

    // ---------- Réglages ----------

    fun setLang(l: Lang) = update { it.copy(lang = l) }
    fun completeOnboarding() = update { it.copy(onboardingDone = true) }
    fun setMethod(m: CalcMethod) = update { it.copy(method = m) }
    fun setAsr(a: AsrSchool) = update { it.copy(asrSchool = a) }

    fun adjust(p: Prayer, delta: Int) = update {
        val v = (it.adjustment(p) + delta).coerceIn(-AppSettings.MAX_ADJUST, AppSettings.MAX_ADJUST)
        it.copy(adjustments = it.adjustments + (p to v))
    }

    fun resetAdjustments() = update { it.copy(adjustments = emptyMap()) }
    fun setNotifications(on: Boolean) = update { it.copy(notificationsEnabled = on) }

    fun togglePrayerNotif(p: Prayer) = update {
        val set = if (p in it.notifPrayers) it.notifPrayers - p else it.notifPrayers + p
        it.copy(notifPrayers = set)
    }

    fun setReminder(min: Int) = update { it.copy(reminderMinutes = min) }
    fun setVibration(on: Boolean) = update { it.copy(vibration = on) }
    fun setAdhan(on: Boolean) = update { it.copy(adhanEnabled = on) }
    fun setTheme(t: ThemeMode) = update { it.copy(theme = t) }
    fun setTimeMode(t: TimeMode) = update { it.copy(timeMode = t) }
    fun setHijriOffset(d: Int) = update { it.copy(hijriOffset = d.coerceIn(-2, 2)) }
    fun dismissTzWarning() = update { it.copy(tzWarningDismissed = true) }

    fun rescheduleNow() {
        val s = settings.value ?: return
        viewModelScope.launch(Dispatchers.Default) { PrayerScheduler.schedule(getApplication(), s) }
    }

    /** « Effacer mes données » : alarmes, notifications, réglages et position en mémoire. */
    fun clearAllData() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            PrayerScheduler.cancelAll(ctx)
            AdhanService.stop(ctx)
            Notifications.cancelAll(ctx)
            liveGps.value = null
            _gpsStatus.value = GpsStatus.IDLE
            repo.clearAll()
        }
    }
}
