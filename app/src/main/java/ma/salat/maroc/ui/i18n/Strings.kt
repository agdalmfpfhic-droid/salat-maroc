package ma.salat.maroc.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import ma.salat.maroc.astro.AsrSchool
import ma.salat.maroc.astro.CalcMethod
import ma.salat.maroc.astro.HijriDay
import ma.salat.maroc.astro.Prayer
import ma.salat.maroc.data.City
import ma.salat.maroc.data.Lang
import java.time.LocalDate
import java.util.Locale

/**
 * Tous les textes de l'application, en français et en arabe.
 * Le choix se fait dans l'application (indépendamment de la langue du téléphone).
 */
abstract class Strings {
    abstract val lang: Lang
    val isRtl: Boolean get() = lang == Lang.AR

    // Général
    abstract val appName: String
    abstract val tabPrayers: String
    abstract val tabQibla: String
    abstract val settings: String
    abstract val back: String
    abstract val cancel: String
    abstract val ok: String
    abstract val change: String
    abstract val minutesShort: String

    // Prières
    abstract fun prayer(p: Prayer): String
    abstract val nextPrayer: String
    abstract val inLabel: String
    abstract fun countdownA11y(p: String, time: String, hours: Long, minutes: Long): String
    abstract fun notifOnA11y(p: String): String
    abstract fun notifOffA11y(p: String): String
    abstract fun methodFooter(method: String): String
    abstract val sunriseNote: String

    // Lieu
    abstract fun city(c: City): String
    abstract val myPosition: String
    abstract val manualCoordinates: String
    abstract val gpsSearching: String
    abstract val gpsFailed: String
    abstract val gpsDisabled: String
    abstract val gpsDenied: String
    abstract fun gpsFallback(city: String): String
    abstract val chooseCity: String
    abstract val useMyLocation: String
    abstract val searchCity: String
    abstract val advanced: String
    abstract val latitude: String
    abstract val longitude: String
    abstract val invalidCoordinates: String
    abstract val coordinatesHelp: String
    abstract val refreshPosition: String

    // Dates
    abstract fun formatGregorian(d: LocalDate): String
    abstract fun formatHijri(h: HijriDay): String

    // Qibla
    abstract fun qiblaDegrees(deg: Int): String
    abstract fun direction(index: Int): String
    abstract val northLetter: String
    abstract val eastLetter: String
    abstract val southLetter: String
    abstract val westLetter: String
    abstract fun turnRight(deg: Int): String
    abstract fun turnLeft(deg: Int): String
    abstract val facingQibla: String
    abstract val holdFlat: String
    abstract val calibrationTitle: String
    abstract val calibrationText: String
    abstract val interference: String
    abstract val recalibrate: String
    abstract val noCompass: String
    abstract fun fromNorth(deg: Int): String
    abstract fun distanceToMecca(km: Long): String
    abstract val calibrationDone: String

    // Réglages
    abstract val secLanguage: String
    abstract val secLocation: String
    abstract val secMethod: String
    abstract fun methodName(m: CalcMethod): String
    abstract fun methodDetail(m: CalcMethod): String
    abstract val secAsr: String
    abstract fun asrName(s: AsrSchool): String
    abstract val secAdjust: String
    abstract val adjustHelp: String
    abstract val resetAdjust: String
    abstract fun increaseA11y(p: String): String
    abstract fun decreaseA11y(p: String): String
    abstract val secNotifications: String
    abstract val notifMaster: String
    abstract val notifWhich: String
    abstract val reminderBefore: String
    abstract val reminderOff: String
    abstract val vibration: String
    abstract val notifPermissionNeeded: String
    abstract val grantNotif: String
    abstract val exactAlarmNeeded: String
    abstract val grantExact: String
    abstract val secDisplay: String
    abstract val themeAuto: String
    abstract val themeLight: String
    abstract val themeDark: String
    abstract val secTime: String
    abstract val timeAuto: String
    abstract val timeGmt: String
    abstract val timeHelp: String
    abstract val secHijri: String
    abstract val hijriHelp: String
    abstract fun days(n: Int): String
    abstract val secPrivacy: String
    abstract val privacyOpen: String
    abstract val privacyTitle: String
    abstract val privacyMain: String
    abstract val privacyPoints: List<String>
    abstract val clearData: String
    abstract val clearConfirmTitle: String
    abstract val clearConfirmText: String
    abstract val clearConfirm: String

    // Accueil (premier lancement)
    abstract val welcome: String
    abstract val chooseLanguage: String
    abstract val whereAreYou: String
    abstract val onboardingPrivacy: String

    // Avertissement heure
    abstract val tzWarningTitle: String
    abstract val tzWarningText: String
    abstract val useGmt: String
    abstract val ignore: String

    // Notifications
    abstract fun notifTitleAt(p: String): String
    abstract fun notifTextAt(time: String, place: String): String
    abstract fun notifTitleBefore(p: String, minutes: Int): String
    abstract fun notifTextBefore(p: String, time: String): String
    abstract val channelVibrate: String
    abstract val channelSilent: String
    abstract val channelDesc: String
    abstract val channelAdhan: String

    // Adhan
    abstract val adhanSwitch: String
    abstract val adhanSilentNote: String
    abstract val adhanListen: String
    abstract val adhanTest: String
    abstract val stopAdhan: String

    companion object {
        fun of(lang: Lang): Strings = if (lang == Lang.AR) ArStrings else FrStrings
    }
}

val LocalStrings = staticCompositionLocalOf<Strings> { FrStrings }

// =====================================================================
// Français
// =====================================================================
object FrStrings : Strings() {
    override val lang = Lang.FR
    override val appName = "Salat Maroc"
    override val tabPrayers = "Prières"
    override val tabQibla = "Qibla"
    override val settings = "Réglages"
    override val back = "Retour"
    override val cancel = "Annuler"
    override val ok = "OK"
    override val change = "Changer"
    override val minutesShort = "min"

    override fun prayer(p: Prayer) = when (p) {
        Prayer.FAJR -> "Fajr"
        Prayer.SUNRISE -> "Lever du soleil"
        Prayer.DHUHR -> "Dhouhr"
        Prayer.ASR -> "Asr"
        Prayer.MAGHRIB -> "Maghrib"
        Prayer.ISHA -> "Isha"
    }
    override val nextPrayer = "Prochaine prière"
    override val inLabel = "dans"
    override fun countdownA11y(p: String, time: String, hours: Long, minutes: Long) =
        "Prochaine prière : $p à $time, dans $hours heure${if (hours > 1) "s" else ""} et $minutes minute${if (minutes > 1) "s" else ""}"
    override fun notifOnA11y(p: String) = "Notification de $p activée. Appuyez pour désactiver."
    override fun notifOffA11y(p: String) = "Notification de $p désactivée. Appuyez pour activer."
    override fun methodFooter(method: String) = "Méthode : $method"
    override val sunriseNote = "Fin du temps de Fajr"

    override fun city(c: City) = c.nameFr
    override val myPosition = "Ma position (GPS)"
    override val manualCoordinates = "Coordonnées manuelles"
    override val gpsSearching = "Recherche de votre position…"
    override val gpsFailed = "Position introuvable. Sortez près d'une fenêtre ou choisissez une ville."
    override val gpsDisabled = "La localisation du téléphone est désactivée. Activez-la ou choisissez une ville."
    override val gpsDenied = "Localisation refusée. Vous pouvez choisir votre ville dans la liste."
    override fun gpsFallback(city: String) = "En attendant la position, calcul pour $city."
    override val chooseCity = "Choisir ma ville"
    override val useMyLocation = "Utiliser ma position"
    override val searchCity = "Rechercher une ville"
    override val advanced = "Avancé : saisir latitude et longitude"
    override val latitude = "Latitude (ex. 33.57)"
    override val longitude = "Longitude (ex. -7.59)"
    override val invalidCoordinates = "Coordonnées invalides. Latitude entre -90 et 90, longitude entre -180 et 180."
    override val coordinatesHelp = "Pour l'ouest du Maroc, la longitude est négative (ex. -7.59)."
    override val refreshPosition = "Actualiser ma position"

    private val months = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    )
    private val weekdays = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    private val hijriMonths = listOf(
        "Mouharram", "Safar", "Rabia al-awal", "Rabia ath-thani", "Joumada al-oula", "Joumada ath-thania",
        "Rajab", "Chaabane", "Ramadan", "Chawwal", "Dhou al-qi'da", "Dhou al-hijja",
    )
    override fun formatGregorian(d: LocalDate) =
        "${weekdays[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${months[d.monthValue - 1]} ${d.year}"
    override fun formatHijri(h: HijriDay) = "${h.day} ${hijriMonths[h.month - 1]} ${h.year}"

    override fun qiblaDegrees(deg: Int) = "Qibla : $deg°"
    private val directions = listOf("Nord", "Nord-Est", "Est", "Sud-Est", "Sud", "Sud-Ouest", "Ouest", "Nord-Ouest")
    override fun direction(index: Int) = directions[index]
    override val northLetter = "N"
    override val eastLetter = "E"
    override val southLetter = "S"
    override val westLetter = "O"
    override fun turnRight(deg: Int) = "Tournez à droite ($deg°)"
    override fun turnLeft(deg: Int) = "Tournez à gauche ($deg°)"
    override val facingQibla = "Vous êtes face à la Qibla ✓"
    override val holdFlat = "Tenez le téléphone bien à plat."
    override val calibrationTitle = "La boussole doit être calibrée"
    override val calibrationText = "Faites un mouvement en forme de 8 avec le téléphone, loin des objets métalliques."
    override val interference = "Objet métallique ou aimant à proximité : éloignez-vous-en."
    override val recalibrate = "Recalibrer la boussole"
    override val noCompass = "Ce téléphone n'a pas de boussole. Utilisez l'angle ci-dessus avec une boussole classique."
    override fun fromNorth(deg: Int) = "$deg° depuis le Nord"
    override fun distanceToMecca(km: Long) = "La Mecque : ${String.format(Locale.ROOT, "%,d", km).replace(',', ' ')} km"
    override val calibrationDone = "Terminé"

    override val secLanguage = "Langue"
    override val secLocation = "Lieu"
    override val secMethod = "Méthode de calcul"
    override fun methodName(m: CalcMethod) = when (m) {
        CalcMethod.MOROCCO -> "Maroc – Ministère des Habous"
        CalcMethod.MWL -> "Ligue islamique mondiale"
        CalcMethod.EGYPT -> "Égypte"
        CalcMethod.UMM_AL_QURA -> "Umm al-Qura (La Mecque)"
        CalcMethod.ISNA -> "ISNA (Amérique du Nord)"
        CalcMethod.FRANCE_UOIF -> "France (UOIF)"
    }
    override fun methodDetail(m: CalcMethod) = when (m) {
        CalcMethod.MOROCCO -> "Fajr 19°, Isha 17° (recommandé au Maroc)"
        CalcMethod.MWL -> "Fajr 18°, Isha 17°"
        CalcMethod.EGYPT -> "Fajr 19,5°, Isha 17,5°"
        CalcMethod.UMM_AL_QURA -> "Fajr 18,5°, Isha 90 min après Maghrib"
        CalcMethod.ISNA -> "Fajr 15°, Isha 15°"
        CalcMethod.FRANCE_UOIF -> "Fajr 12°, Isha 12°"
    }
    override val secAsr = "École pour l'Asr"
    override fun asrName(s: AsrSchool) = when (s) {
        AsrSchool.STANDARD -> "Maliki / Standard"
        AsrSchool.HANAFI -> "Hanafi"
    }
    override val secAdjust = "Ajuster les horaires"
    override val adjustHelp = "Si votre mosquée appelle un peu plus tôt ou plus tard, ajoutez ou retirez des minutes."
    override val resetAdjust = "Remettre à zéro"
    override fun increaseA11y(p: String) = "Ajouter une minute à $p"
    override fun decreaseA11y(p: String) = "Retirer une minute à $p"
    override val secNotifications = "Notifications"
    override val notifMaster = "Activer les notifications"
    override val notifWhich = "Pour quelles prières ?"
    override val reminderBefore = "Rappel avant la prière"
    override val reminderOff = "Non"
    override val vibration = "Vibration"
    override val notifPermissionNeeded = "Les notifications sont bloquées pour cette application."
    override val grantNotif = "Autoriser les notifications"
    override val exactAlarmNeeded = "Pour être prévenu à la minute exacte, autorisez les « alarmes et rappels »."
    override val grantExact = "Autoriser les alarmes"
    override val secDisplay = "Affichage"
    override val themeAuto = "Automatique"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val secTime = "Heure"
    override val timeAuto = "Heure du téléphone"
    override val timeGmt = "Forcer GMT (Maroc)"
    override val timeHelp = "Le Maroc est revenu à l'heure GMT le 20 septembre 2026. Si l'heure de votre téléphone a une heure d'avance, choisissez « Forcer GMT » ou mettez à jour le téléphone."
    override val secHijri = "Date hégirienne"
    override val hijriHelp = "Au Maroc, le mois commence après l'observation du croissant. Corrigez d'un ou deux jours si besoin."
    override fun days(n: Int) = when {
        n == 0 -> "0 jour"
        n > 0 -> "+$n jour${if (n > 1) "s" else ""}"
        else -> "$n jour${if (n < -1) "s" else ""}"
    }
    override val secPrivacy = "Confidentialité"
    override val privacyOpen = "Voir la confidentialité"
    override val privacyTitle = "Confidentialité"
    override val privacyMain = "Cette application fonctionne hors ligne et ne collecte ni ne partage aucune donnée."
    override val privacyPoints = listOf(
        "Aucune connexion Internet : l'application n'a pas la permission d'accéder au réseau.",
        "Aucune publicité, aucun compte, aucun suivi, aucune statistique.",
        "Votre position sert uniquement à calculer les horaires et la Qibla sur le téléphone. Elle n'est jamais envoyée ni partagée, et aucun historique n'est gardé.",
        "Seuls vos réglages sont enregistrés, dans la mémoire privée de l'application. Ils ne sont pas sauvegardés dans le cloud.",
        "Vous pouvez tout effacer à tout moment avec le bouton « Effacer mes données ».",
    )
    override val clearData = "Effacer mes données"
    override val clearConfirmTitle = "Effacer mes données ?"
    override val clearConfirmText = "Tous vos réglages (ville, position, ajustements, notifications) seront supprimés de ce téléphone."
    override val clearConfirm = "Oui, effacer"

    override val welcome = "Bienvenue"
    override val chooseLanguage = "Choisissez votre langue"
    override val whereAreYou = "Où êtes-vous ?"
    override val onboardingPrivacy = "Tout reste sur votre téléphone. Aucune donnée n'est envoyée."

    override val tzWarningTitle = "L'heure du téléphone semble en avance"
    override val tzWarningText = "Le Maroc est à l'heure GMT depuis le 20 septembre 2026, mais votre téléphone est réglé sur GMT+1."
    override val useGmt = "Utiliser l'heure GMT"
    override val ignore = "Ignorer"

    override fun notifTitleAt(p: String) = "C'est l'heure de $p"
    override fun notifTextAt(time: String, place: String) = "$time — $place"
    override fun notifTitleBefore(p: String, minutes: Int) = "$p dans $minutes minutes"
    override fun notifTextBefore(p: String, time: String) = "$p à $time"
    override val channelVibrate = "Prières (avec vibration)"
    override val channelSilent = "Prières (sans vibration)"
    override val channelDesc = "Heure de la prière et rappels"
    override val channelAdhan = "Adhan"
    override val adhanSwitch = "Jouer l'adhan à l'heure de la prière"
    override val adhanSilentNote = "L'adhan ne joue pas si le téléphone est en mode silencieux ou vibreur (par exemple à la mosquée). Le volume est celui des alarmes."
    override val adhanListen = "Écouter l'adhan"
    override val adhanTest = "Adhan"
    override val stopAdhan = "Arrêter l'adhan"
}

// =====================================================================
// العربية
// =====================================================================
object ArStrings : Strings() {
    override val lang = Lang.AR
    override val appName = "صلاة المغرب"
    override val tabPrayers = "الصلوات"
    override val tabQibla = "القبلة"
    override val settings = "الإعدادات"
    override val back = "رجوع"
    override val cancel = "إلغاء"
    override val ok = "حسنا"
    override val change = "تغيير"
    override val minutesShort = "د"

    override fun prayer(p: Prayer) = when (p) {
        Prayer.FAJR -> "الفجر"
        Prayer.SUNRISE -> "الشروق"
        Prayer.DHUHR -> "الظهر"
        Prayer.ASR -> "العصر"
        Prayer.MAGHRIB -> "المغرب"
        Prayer.ISHA -> "العشاء"
    }
    override val nextPrayer = "الصلاة القادمة"
    override val inLabel = "بعد"
    override fun countdownA11y(p: String, time: String, hours: Long, minutes: Long) =
        "الصلاة القادمة: $p على الساعة $time، بعد $hours ساعة و $minutes دقيقة"
    override fun notifOnA11y(p: String) = "تنبيه $p مفعل. اضغط لإيقافه."
    override fun notifOffA11y(p: String) = "تنبيه $p متوقف. اضغط لتفعيله."
    override fun methodFooter(method: String) = "طريقة الحساب: $method"
    override val sunriseNote = "نهاية وقت الفجر"

    override fun city(c: City) = c.nameAr
    override val myPosition = "موقعي (GPS)"
    override val manualCoordinates = "إحداثيات يدوية"
    override val gpsSearching = "جارٍ تحديد موقعك…"
    override val gpsFailed = "تعذر تحديد الموقع. اقترب من نافذة أو اختر مدينتك."
    override val gpsDisabled = "خدمة الموقع متوقفة في الهاتف. فعّلها أو اختر مدينتك."
    override val gpsDenied = "تم رفض الوصول إلى الموقع. يمكنك اختيار مدينتك من القائمة."
    override fun gpsFallback(city: String) = "في انتظار الموقع، الحساب حسب مدينة $city."
    override val chooseCity = "اختيار مدينتي"
    override val useMyLocation = "استعمال موقعي"
    override val searchCity = "البحث عن مدينة"
    override val advanced = "متقدم: إدخال خط العرض وخط الطول"
    override val latitude = "خط العرض (مثال 33.57)"
    override val longitude = "خط الطول (مثال -7.59)"
    override val invalidCoordinates = "إحداثيات غير صحيحة. خط العرض بين -90 و 90، وخط الطول بين -180 و 180."
    override val coordinatesHelp = "في المغرب يكون خط الطول سالبا (مثال -7.59)."
    override val refreshPosition = "تحديث موقعي"

    private val months = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو",
        "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر",
    )
    private val weekdays = listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    private val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )
    override fun formatGregorian(d: LocalDate) =
        "${weekdays[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${months[d.monthValue - 1]} ${d.year}"
    override fun formatHijri(h: HijriDay) = "${h.day} ${hijriMonths[h.month - 1]} ${h.year} هـ"

    override fun qiblaDegrees(deg: Int) = "القبلة: $deg°"
    private val directions = listOf("الشمال", "الشمال الشرقي", "الشرق", "الجنوب الشرقي", "الجنوب", "الجنوب الغربي", "الغرب", "الشمال الغربي")
    override fun direction(index: Int) = directions[index]
    override val northLetter = "ش"
    override val eastLetter = "ق"
    override val southLetter = "ج"
    override val westLetter = "غ"
    override fun turnRight(deg: Int) = "استدر نحو اليمين ($deg°)"
    override fun turnLeft(deg: Int) = "استدر نحو اليسار ($deg°)"
    override val facingQibla = "أنت في اتجاه القبلة ✓"
    override val holdFlat = "ضع الهاتف بشكل مستوٍ."
    override val calibrationTitle = "البوصلة تحتاج إلى معايرة"
    override val calibrationText = "حرّك الهاتف على شكل الرقم 8، بعيدا عن الأشياء المعدنية."
    override val interference = "يوجد معدن أو مغناطيس قريب: ابتعد عنه."
    override val recalibrate = "إعادة معايرة البوصلة"
    override val noCompass = "هذا الهاتف لا يتوفر على بوصلة. استعمل الزاوية أعلاه مع بوصلة عادية."
    override fun fromNorth(deg: Int) = "$deg° انطلاقا من الشمال"
    override fun distanceToMecca(km: Long) = "مكة المكرمة: ${String.format(Locale.ROOT, "%,d", km).replace(',', ' ')} كلم"
    override val calibrationDone = "تم"

    override val secLanguage = "اللغة"
    override val secLocation = "المكان"
    override val secMethod = "طريقة الحساب"
    override fun methodName(m: CalcMethod) = when (m) {
        CalcMethod.MOROCCO -> "المغرب – وزارة الأوقاف"
        CalcMethod.MWL -> "رابطة العالم الإسلامي"
        CalcMethod.EGYPT -> "مصر"
        CalcMethod.UMM_AL_QURA -> "أم القرى (مكة)"
        CalcMethod.ISNA -> "أمريكا الشمالية (ISNA)"
        CalcMethod.FRANCE_UOIF -> "فرنسا (UOIF)"
    }
    override fun methodDetail(m: CalcMethod) = when (m) {
        CalcMethod.MOROCCO -> "الفجر 19°، العشاء 17° (موصى بها في المغرب)"
        CalcMethod.MWL -> "الفجر 18°، العشاء 17°"
        CalcMethod.EGYPT -> "الفجر 19.5°، العشاء 17.5°"
        CalcMethod.UMM_AL_QURA -> "الفجر 18.5°، العشاء 90 دقيقة بعد المغرب"
        CalcMethod.ISNA -> "الفجر 15°، العشاء 15°"
        CalcMethod.FRANCE_UOIF -> "الفجر 12°، العشاء 12°"
    }
    override val secAsr = "مذهب العصر"
    override fun asrName(s: AsrSchool) = when (s) {
        AsrSchool.STANDARD -> "مالكي / الجمهور"
        AsrSchool.HANAFI -> "حنفي"
    }
    override val secAdjust = "تعديل الأوقات"
    override val adjustHelp = "إذا كان مسجدك يؤذن قبل أو بعد قليلا، أضف أو أنقص دقائق."
    override val resetAdjust = "إرجاع إلى الصفر"
    override fun increaseA11y(p: String) = "إضافة دقيقة إلى $p"
    override fun decreaseA11y(p: String) = "إنقاص دقيقة من $p"
    override val secNotifications = "التنبيهات"
    override val notifMaster = "تفعيل التنبيهات"
    override val notifWhich = "لأي صلاة؟"
    override val reminderBefore = "تذكير قبل الصلاة"
    override val reminderOff = "لا"
    override val vibration = "الاهتزاز"
    override val notifPermissionNeeded = "التنبيهات ممنوعة لهذا التطبيق."
    override val grantNotif = "السماح بالتنبيهات"
    override val exactAlarmNeeded = "للتنبيه في الدقيقة المضبوطة، اسمح بـ«المنبهات والتذكيرات»."
    override val grantExact = "السماح بالمنبهات"
    override val secDisplay = "العرض"
    override val themeAuto = "تلقائي"
    override val themeLight = "فاتح"
    override val themeDark = "داكن"
    override val secTime = "الساعة"
    override val timeAuto = "ساعة الهاتف"
    override val timeGmt = "فرض توقيت غرينتش (المغرب)"
    override val timeHelp = "عاد المغرب إلى توقيت غرينتش يوم 20 شتنبر 2026. إذا كانت ساعة هاتفك متقدمة بساعة، اختر «فرض توقيت غرينتش» أو حدّث الهاتف."
    override val secHijri = "التاريخ الهجري"
    override val hijriHelp = "في المغرب يبدأ الشهر بعد رؤية الهلال. صحّح بيوم أو يومين عند الحاجة."
    override fun days(n: Int) = when {
        n == 0 -> "0 يوم"
        n > 0 -> "+$n يوم"
        else -> "$n يوم"
    }
    override val secPrivacy = "الخصوصية"
    override val privacyOpen = "عرض سياسة الخصوصية"
    override val privacyTitle = "الخصوصية"
    override val privacyMain = "يعمل هذا التطبيق بدون إنترنت ولا يجمع أو يشارك أي بيانات."
    override val privacyPoints = listOf(
        "لا اتصال بالإنترنت: التطبيق لا يملك إذن الوصول إلى الشبكة.",
        "لا إشهار، لا حساب، لا تتبع، لا إحصائيات.",
        "موقعك يُستعمل فقط لحساب الأوقات والقبلة داخل الهاتف. لا يُرسل ولا يُشارك أبدا، ولا يُحفظ أي سجل.",
        "تُحفظ إعداداتك فقط في الذاكرة الخاصة بالتطبيق، ولا تُنسخ إلى السحابة.",
        "يمكنك حذف كل شيء في أي وقت بزر «حذف بياناتي».",
    )
    override val clearData = "حذف بياناتي"
    override val clearConfirmTitle = "حذف بياناتي؟"
    override val clearConfirmText = "سيتم حذف جميع إعداداتك (المدينة، الموقع، التعديلات، التنبيهات) من هذا الهاتف."
    override val clearConfirm = "نعم، احذف"

    override val welcome = "مرحبا"
    override val chooseLanguage = "اختر لغتك"
    override val whereAreYou = "أين أنت؟"
    override val onboardingPrivacy = "كل شيء يبقى في هاتفك. لا تُرسل أي بيانات."

    override val tzWarningTitle = "يبدو أن ساعة الهاتف متقدمة"
    override val tzWarningText = "المغرب على توقيت غرينتش منذ 20 شتنبر 2026، لكن هاتفك مضبوط على غرينتش+1."
    override val useGmt = "استعمال توقيت غرينتش"
    override val ignore = "تجاهل"

    override fun notifTitleAt(p: String) = "حان وقت صلاة $p"
    override fun notifTextAt(time: String, place: String) = "$time — $place"
    override fun notifTitleBefore(p: String, minutes: Int) = "صلاة $p بعد $minutes دقيقة"
    override fun notifTextBefore(p: String, time: String) = "$p على الساعة $time"
    override val channelVibrate = "الصلوات (مع اهتزاز)"
    override val channelSilent = "الصلوات (بدون اهتزاز)"
    override val channelDesc = "وقت الصلاة والتذكيرات"
    override val channelAdhan = "الأذان"
    override val adhanSwitch = "تشغيل الأذان عند وقت الصلاة"
    override val adhanSilentNote = "لا يُشغَّل الأذان إذا كان الهاتف في الوضع الصامت أو الاهتزاز (مثلا في المسجد). مستوى الصوت هو صوت المنبه."
    override val adhanListen = "الاستماع إلى الأذان"
    override val adhanTest = "الأذان"
    override val stopAdhan = "إيقاف الأذان"
}
