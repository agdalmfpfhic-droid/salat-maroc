# Salat Maroc · صلاة المغرب

Application Android **native** (Kotlin + Jetpack Compose) : horaires des cinq prières selon le Maroc et boussole Qibla.
**100 % hors ligne**, sans publicité, sans compte, sans suivi, sans permission Internet.

---

## 1. Générer l'APK dans Android Studio

1. Installez **Android Studio** (version Ladybug 2024.2 ou plus récente) : https://developer.android.com/studio
2. Décompressez `SalatMaroc.zip`, puis dans Android Studio : **File › Open** › choisissez le dossier `SalatMaroc`.
3. Attendez la fin de la « synchronisation Gradle » (barre en bas ; la première fois, Android Studio télécharge Gradle et les bibliothèques, il faut Internet **sur l'ordinateur** — pas sur le téléphone).
   Si Android Studio propose de mettre à jour le plugin Gradle (AGP), vous pouvez accepter.
4. **APK de test (rapide)** : menu **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.
   Le fichier est créé ici : `app/build/outputs/apk/debug/app-debug.apk` (cliquez « locate » dans la notification).
5. **APK final signé (pour distribuer)** : **Build › Generate Signed App Bundle / APK…** › **APK** › *Create new…* pour créer votre clé (gardez bien le fichier `.jks` et le mot de passe) › variante **release** › *Create*.
   Résultat : `app/release/app-release.apk`.
6. **Installer sur un téléphone** : branchez le téléphone en USB avec le « débogage USB » activé et cliquez ▶ *Run*, ou copiez l'APK sur le téléphone et ouvrez-le (autorisez « installer des applications inconnues »).

En ligne de commande (facultatif) : `./gradlew assembleDebug` ou `./gradlew testDebugUnitTest` pour lancer les tests des calculs.

---

## 2. Ce que fait l'application

**Écran principal unique, deux onglets en bas : « Prières » et « Qibla ».** Bouton « Réglages » en haut.

### Prières
- Fajr, (lever du soleil), Dhouhr, Asr, Maghrib, Isha ; prochaine prière en très grand avec compte à rebours.
- Dates grégorienne et hégirienne (calendrier intégré à Android, décalage ±2 jours réglable car le Maroc suit l'observation du croissant).
- Une cloche par prière pour activer/désactiver sa notification d'un geste.
- 56 villes marocaines préchargées, position GPS, ou latitude/longitude saisies à la main.

### Qibla
- Boussole magnétomètre + accéléromètre, correction de la déclinaison magnétique (modèle intégré à Android).
- Grande flèche avec une petite Kaaba, angle (« Qibla : 94° »), direction simple (Est…), consigne « Tournez à droite / à gauche », message vert + petite vibration quand on est aligné.
- Détection du besoin de calibrage et des perturbations magnétiques ; bouton « Recalibrer la boussole » avec animation du mouvement en 8.
- Distance jusqu'à La Mecque.

### Réglages
Langue (arabe / français, interface de droite à gauche en arabe), lieu, méthode de calcul (6 méthodes), école Asr (Maliki/Standard ou Hanafi), ajustement ±30 min par prière, notifications (générales, par prière, rappel 5/10/15/20/30 min avant, vibration), thème (auto/clair/sombre), heure (téléphone ou GMT forcé), décalage hégirien, page Confidentialité, **Effacer mes données**.

---

## 3. Points importants pour le Maroc

### Méthode par défaut : Ministère des Habous
La méthode par défaut reproduit celle du **Ministère des Habous et des Affaires islamiques** : Fajr **19°**, Isha **17°**, avec les corrections officielles (Chourouq −3 min, Dhouhr +5 min, Maghrib +5 min).
Elle a été vérifiée contre les horaires publiés pour Rabat et Casablanca (octobre 2026) : **identiques à la minute près** (voir `app/src/test/.../PrayerCalculatorTest.kt`).
La méthode Fajr 18° / Isha 17° (Ligue islamique mondiale) reste disponible dans les réglages ; au Maroc elle donne un Fajr environ 5 minutes plus tard que l'horaire officiel.

### Heure légale : retour à GMT le 20 septembre 2026
Le Maroc est revenu définitivement à l'heure GMT (UTC+0) le 20/09/2026 (décret n° 2.26.530), sans changement pendant le Ramadan.
- Les calculs sont faits en temps universel puis affichés dans l'heure du téléphone : si le téléphone est à jour, tout est automatique.
- Si un téléphone ancien n'a pas reçu la mise à jour et reste à GMT+1, l'application l'affiche avec un bouton **« Utiliser l'heure GMT »** (aussi dans Réglages › Heure).

### Notifications à la minute exacte
Sur Android 12 et plus, il faut autoriser les « Alarmes et rappels ». L'application affiche un bouton pour l'autoriser si besoin. Sans cette autorisation, Android peut retarder la notification de quelques minutes.
Sans fichier d'adhan, le son est celui des notifications du téléphone.

### Ajouter l'adhan (2 minutes, sans toucher au code)
1. Procurez-vous un enregistrement d'adhan **libre de droits** (par exemple sur Wikimedia Commons, en vérifiant la licence indiquée sur la page du fichier) ou un enregistrement dont vous avez l'autorisation.
2. Renommez-le **`adhan.mp3`** (minuscules, sans espace ni accent ; `.ogg` fonctionne aussi).
   Facultatif : un adhan spécial pour le Fajr nommé **`adhan_fajr.mp3`**.
3. Dans Android Studio, copiez le(s) fichier(s) dans **`app/src/main/res/raw/`** (le dossier existe déjà), puis regénérez l'APK.

L'application détecte le fichier automatiquement. Dans **Réglages › Notifications** apparaissent alors :
- « Jouer l'adhan à l'heure de la prière » (activé par défaut) ;
- un bouton « Écouter l'adhan » pour tester le volume.

Fonctionnement :
- L'adhan est joué à l'heure de la prière, et non pour les rappels « X minutes avant ».
- La notification affiche un bouton **« Arrêter l'adhan »**. Balayer la notification arrête aussi le son.
- Le volume utilisé est celui des **alarmes**.
- L'adhan **ne joue pas** si le téléphone est en mode silencieux ou vibreur, par exemple à la mosquée. Une notification normale s'affiche alors.
- Conseil : un fichier MP3 mono de 64 à 96 kbit/s pèse environ 1 à 2 Mo et garde l'application légère.

---


### Adhan inclus
La fabrication automatique sur GitHub ajoute l'enregistrement **« Beautiful adhan.ogg »** (Adam-synagda, Wikimedia Commons, licence **CC0** – domaine public, 2 min 34). Pour utiliser un autre adhan, déposez votre propre `adhan.mp3` ou `adhan.ogg` dans `app/src/main/res/raw/` : il sera utilisé à la place.

## 4. Confidentialité (vérifiable dans le code)

| Garantie | Où |
|---|---|
| Aucune permission réseau (INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE explicitement **retirées**, même si une bibliothèque tentait de les ajouter) | `AndroidManifest.xml` |
| Aucun SDK tiers : uniquement des bibliothèques officielles AndroidX/Compose | `app/build.gradle.kts` |
| Pas de sauvegarde cloud ni de transfert vers un autre appareil | `allowBackup="false"` + `res/xml/data_extraction_rules.xml` |
| Position GPS précise gardée **uniquement en mémoire** ; seule une valeur arrondie (~1 km) est conservée localement, écrasée à chaque fois (pas d'historique), pour que les notifications continuent après un redémarrage | `MainViewModel.kt`, `AppSettings.kt` |
| GPS via le `LocationManager` d'Android (pas de Google Play Services) — fonctionne sans Internet | `sensors/LocationHelper.kt` |
| « Effacer mes données » : supprime réglages, position, alarmes et notifications | `MainViewModel.clearAllData()` |

---

## 5. Structure du projet

```
SalatMaroc/
├── settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradle/wrapper/
└── app/
    ├── build.gradle.kts                     minSdk 26 (Android 8.0), targetSdk 35
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/ma/salat/maroc/
        │   │   ├── MainActivity.kt          thème, langue, sens RTL
        │   │   ├── SalatApp.kt
        │   │   ├── astro/
        │   │   │   ├── PrayerCalculator.kt  calcul astronomique des horaires + méthodes
        │   │   │   ├── Qibla.kt             grand cercle, distance, points cardinaux
        │   │   │   └── HijriHelper.kt       date hégirienne hors ligne
        │   │   ├── data/
        │   │   │   ├── Cities.kt            56 villes marocaines (FR + AR)
        │   │   │   ├── AppSettings.kt       modèle des réglages
        │   │   │   └── SettingsRepository.kt DataStore local
        │   │   ├── notif/
        │   │   │   ├── PrayerScheduler.kt   AlarmManager (prochaine prière / rappel)
        │   │   │   ├── Notifications.kt     canaux + affichage
        │   │   │   ├── AdhanService.kt      lecture locale de l'adhan + bouton Arrêter
        │   │   │   └── Receivers.kt         alarme, redémarrage, changement d'heure
        │   │   ├── sensors/
        │   │   │   ├── CompassSensor.kt     magnétomètre + accéléromètre
        │   │   │   └── LocationHelper.kt    GPS local
        │   │   └── ui/
        │   │       ├── MainViewModel.kt     MVVM
        │   │       ├── AppRoot.kt           onglets Prières / Qibla + navigation
        │   │       ├── Permissions.kt
        │   │       ├── i18n/Strings.kt      tous les textes FR + AR
        │   │       ├── theme/Theme.kt       couleurs, gros textes, mode sombre
        │   │       ├── components/Components.kt  gros boutons, cartes
        │   │       └── screens/             Prières, Qibla, Réglages, Lieu, Confidentialité, Accueil
        │   └── res/                         icônes vectorielles, thèmes, règles de sauvegarde
        │       └── raw/                     ← déposez ici adhan.mp3 (et adhan_fajr.mp3)
        └── test/…/PrayerCalculatorTest.kt   tests contre les horaires officiels
```

## 6. Accessibilité
- Toutes les tailles en `sp` : l'application suit la taille de police du téléphone ; les écrans défilent si le texte est très grand.
- Zones tactiles ≥ 56–64 dp, contrastes élevés, mode sombre automatique.
- TalkBack : descriptions pour la cloche de chaque prière, la carte « prochaine prière » (lue en heures et minutes), la flèche Qibla et les boutons + / −.
