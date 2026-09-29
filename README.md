![NextSet – Rest. Set. Go.](Branding/banner.png)

# NextSet

**Satzpausen-Timer für iPhone & Apple Watch, Android & Wear OS.** Ein Tipp auf einen deiner ein bis zwei Schnell-Timer, und die Pause läuft. Die letzten Sekunden spürst du am Handgelenk oder am Smartphone und hörst sie auch. Danach geht's in den nächsten Satz.

Das Repository ist ein Monorepo: `ios/` enthält die Swift-Apps für iPhone und Apple Watch, `android/` die native Android-Version (Kotlin, Jetpack Compose) für Smartphone und Wear OS. Beide Versionen verhalten sich gleich; die Timer-Logik ist Zeile für Zeile übertragen und mit denselben Tests abgesichert.

## Name & Logo

**NextSet** sagt, worum es geht: Die App sagt dir, wann dein *nächster Satz* startet. Der Claim **„Rest. Set. Go.“** ist ein Wortspiel auf „Ready, Set, Go“.

Das **Logo** ist eine Kettlebell, deren Körper gleichzeitig ein Timer-Zifferblatt ist. Griff und Kugel ergeben die Silhouette einer Stoppuhr, der Ring in Koralle zeigt die ablaufende Pause. So steht das Logo zugleich für Gym und Timer.

**Farben:** Graphit, Weiß und Glas prägen die App, sie bleibt dadurch ruhig und schlicht. Farbe gibt es nur, wenn es drauf ankommt: In den letzten Sekunden leuchten Ring und Ziffern in Koralle. Koralle ist außerdem die Farbe des Logos und der Schalter, auf der Watch auch die Akzentfarbe.

| iPhone | iPhone (dunkel) | iPhone (getönt) | Apple Watch |
|:---:|:---:|:---:|:---:|
| <img src="ios/NextSet/Assets.xcassets/AppIcon.appiconset/AppIcon.png" width="120"> | <img src="ios/NextSet/Assets.xcassets/AppIcon.appiconset/AppIcon-Dark.png" width="120"> | <img src="ios/NextSet/Assets.xcassets/AppIcon.appiconset/AppIcon-Tinted.png" width="120"> | <img src="ios/NextSetWatch/Assets.xcassets/AppIcon.appiconset/AppIcon-Watch.png" width="120"> |

Die Icons werden aus `Branding/build-icons.mjs` erzeugt (siehe unten).

## Screenshots

| Start | Satzpause | Letzte Sekunden | Los! | Dunkelmodus |
|:---:|:---:|:---:|:---:|:---:|
| <img src="Branding/screenshots/iphone-start.png" width="170"> | <img src="Branding/screenshots/iphone-rest.png" width="170"> | <img src="Branding/screenshots/iphone-countdown-de.png" width="170"> | <img src="Branding/screenshots/iphone-go.png" width="170"> | <img src="Branding/screenshots/iphone-rest-dark.png" width="170"> |

| Einstellungen | Watch: Start | Watch: Satzpause | Watch: Letzte Sekunden | Watch: Los! |
|:---:|:---:|:---:|:---:|:---:|
| <img src="Branding/screenshots/iphone-settings.png" width="170"> | <img src="Branding/screenshots/watch-start.png" width="150"> | <img src="Branding/screenshots/watch-rest.png" width="150"> | <img src="Branding/screenshots/watch-countdown.png" width="150"> | <img src="Branding/screenshots/watch-go.png" width="150"> |

Die Screenshots stammen aus dem iOS-26- bzw. watchOS-26-Simulator (CI-Workflow mit `-demo`-Startargumenten).

## Funktionen

**Schnell starten**
- 1 oder 2 **Schnell-Timer** als große Tasten, ein Tipp startet die Pause (Standard: 1:30 und 3:00)
- **Weitere Timer** als Raster darunter, ebenfalls mit einem Tipp startbar (Standard: 0:30 bis 5:00, bis zu 12 Stück)
- Bei laufender Pause einfach einen anderen Timer antippen, um direkt zu wechseln
- **±15 s** während der Pause, Pausieren/Fortsetzen, Pause vorzeitig beenden
- Nach dem Ende zeigt die App „LOS!“ und zählt die überzogene Zeit hoch (`+0:12`). „Wiederholen“ startet dieselbe Pause erneut.

**Countdown mit Haptik & Ton** (in den Einstellungen anpassbar)
- Signal in den letzten **3, 5 oder 10 Sekunden** oder aus
- **Haptik** an/aus. iPhone: kräftige Core-Haptics-Muster. Watch: Taptic Engine.
- **Töne** an/aus, drei Klänge (**Piep**, **Glocke**, **Digital**). Sie werden zur Laufzeit synthetisiert. Auf dem iPhone laufen sie über deiner Musik, die dafür kurz leiser wird, und auch im Stumm-Modus. Stummschalten geht direkt über den Lautsprecher-Button oben links.

**iPhone**
- Liquid-Glass-Design (iOS 26): Zifferblatt als Glasscheibe, Schnell-Timer als getönte Glas-Kacheln, Glas-Buttons und -Toolbar über einem weichen Mesh-Gradient, der sich im Glas bricht
- Folgt Hell- und Dunkelmodus des Systems; Graphit als Akzent, Koralle für die letzten Sekunden, große Ziffern in SF Rounded
- **Live-Aktivität** auf dem Sperrbildschirm, in der Dynamic Island und im Smart Stack der Apple Watch
- **Mitteilung** zum Pausenende, falls die App im Hintergrund ist
- **Display bleibt an**, solange eine Pause läuft (abschaltbar)
- **Siri, Kurzbefehle & Action-Button**: „Schnell-Timer starten“, „Pausen-Timer starten“ (beliebige Sekunden), „Pause beenden“. Per Sprache z. B. „Siri, starte eine Pause in NextSet“, „Starte Schnell-Timer 2 in NextSet“ oder „NextSet stoppen“.
- Timer umbenennen (z. B. „Kniebeugen“), Reihenfolge ändern, löschen. Lange drücken auf einen Timer öffnet das Kontextmenü.

**Apple Watch**
- Große Schnell-Timer direkt auf dem Startbildschirm, weitere Timer darunter
- Vollbild-Ring mit Countdown, ±15 s, Pause und „Wiederholen“; Always-On-Darstellung
- Läuft dank *Extended Runtime Session* auch bei gesenktem Arm weiter: Countdown-Taps und Ende-Signal kommen zuverlässig am Handgelenk an. Endet die Session, springt eine Mitteilung ein.
- Timer werden per WatchConnectivity mit dem iPhone **synchronisiert** und lassen sich auf beiden Geräten bearbeiten

**Android & Wear OS** (gleiche Funktionen, Android-typisch umgesetzt)
- Material 3 in Graphit & Koralle, Hell- und Dunkelmodus, Themed Icon ab Android 13
- **Live-Benachrichtigung** mit Countdown auf dem Sperrbildschirm und in der Statusleiste (ab Android 16 als Live Update), mit Pausieren, +15 s und Beenden – das Gegenstück zur Live-Aktivität
- **Benachrichtigung** zum Pausenende, auch wenn die App im Hintergrund ist oder beendet wurde (exakter Wecker)
- **App-Verknüpfungen** statt Siri: langes Drücken aufs App-Icon startet die Schnell-Timer oder beendet die Pause; die Verknüpfungen lassen sich auch auf den Startbildschirm legen
- **Wear OS:** Schnell-Timer und weitere Timer auf der Uhr, Vollbild-Ring, Always-On; ein Vordergrunddienst mit *Ongoing Activity* hält den Countdown bei gesenktem Arm am Laufen, die Pause erscheint auch auf dem Zifferblatt. Wegwischen beendet die Pause.
- Timer werden über den *Wearable Data Layer* zwischen Smartphone und Uhr synchronisiert
- Sprache der App in den Android-Einstellungen wählbar (Englisch oder Deutsch)

Die Apps sind auf Englisch und Deutsch lokalisiert.

## Loslegen

### iOS

Voraussetzungen: **Xcode 26** oder neuer, **iOS 26** / **watchOS 26**.

1. `ios/NextSet.xcodeproj` öffnen
2. Das Team ist im Projekt hinterlegt. Wer mit einem anderen Developer-Account baut, wählt in Xcode unter *Signing & Capabilities* sein Team. Mit `BUNDLE_ID_PREFIX` in `ios/Config/Signing.xcconfig` passt du die Bundle-IDs an (aktuell `com.mariokernich.nextset`).
3. Scheme **NextSet** auf dem iPhone starten. Die Watch-App wird mitinstalliert. Für die Watch allein gibt es das Scheme **NextSetWatch**.

> Hinweis: Auf der Watch nutzt die App eine Extended Runtime Session vom Typ *Physical Therapy* (`WKBackgroundModes`). So laufen die Haptik-Signale auch bei gesenktem Handgelenk. Die Session braucht einen signierten Build, also mit gesetztem Team. Unsignierte Builds, etwa im CI, lehnt watchOS ab. Dann springt die Mitteilung am Pausenende ein.

### Android

Voraussetzungen: **Android Studio** (2026.1 oder neuer) mit Android-SDK 37. Die Apps laufen ab **Android 10** (Smartphone) bzw. **Wear OS 3** (Uhr) und zielen auf Android 16.

1. Den Ordner `android/` in Android Studio öffnen
2. Run-Konfiguration **app** auf einem Smartphone oder Emulator starten, **wear** auf einer Uhr oder einem Wear-OS-Emulator
3. Für den Sync zwischen Smartphone und Uhr brauchen beide Google-Play-Dienste und dieselbe Signatur (Debug-Builds vom selben Rechner passen zusammen)

## Projektstruktur

```
ios/                    Xcode-Projekt (iPhone, Apple Watch, Widget)
  NextSet/              iPhone-App (Bildschirme, Live-Aktivität, App Intents)
  NextSetWatch/         Apple-Watch-App
  NextSetWidgets/       Widget-Extension für die Live-Aktivität
  Core/                 Gemeinsam für iPhone & Watch: Timer-Logik, Store, Haptik, Töne, Sync
  Shared/               Gemeinsam für alle Targets: Farben, Logo, Ring, Formatierung, Texte (de/en)
  NextSetTests/         Unit-Tests (Swift Testing)
  Config/               Info.plist-Ergänzungen, Signing.xcconfig und Export-Optionen für den App Store
  scripts/              Screenshot-Skript für den Simulator, Einreichen im App Store
android/                Gradle-Projekt (Smartphone, Wear OS)
  core/                 Gemeinsam für Smartphone & Uhr: Timer-Logik, Store, Haptik, Töne, Sync, Wecker, Ring & Logo, Texte (de/en)
  app/                  Smartphone-App (Bildschirme, Live-Benachrichtigung, App-Verknüpfungen)
  wear/                 Wear-OS-App (Bildschirme, Vordergrunddienst mit Ongoing Activity)
Branding/               Logo, Icons und Banner samt Generator
website/                Support-Seite und Datenschutzerklärung (apps.kernich.de/nextset)
```

Der Timer rechnet auf beiden Plattformen immer mit absoluten Zeitpunkten (`endDate` bzw. `endAt`), nicht mit einem mitlaufenden Zähler. Dadurch stimmt er auch dann, wenn die App im Hintergrund pausiert oder neu gestartet wird. Die Countdown-Ereignisse plant `CountdownPlan`, `TimerStore` spielt sie ab.

## Entwicklung

- **Tests:** in `ios/` `xcodebuild test -project NextSet.xcodeproj -scheme NextSet -destination 'platform=iOS Simulator,name=iPhone 17'`
- **Screenshots:** nach dem Testlauf mit `-derivedDataPath build` in `ios/` einfach `scripts/screenshots.sh build screenshots` ausführen. Die Zustände lassen sich per Startargument wählen, z. B. `-demo running`.
- **Android-Tests und -Builds:** in `android/` `./gradlew :core:testDebugUnitTest :app:assembleDebug :wear:assembleDebug`. Die Unit-Tests in `core` laufen auf der JVM, ohne Emulator.
- **Android-Screenshots:** Debug-Builds nehmen dieselben Zustände wie iOS entgegen, z. B. `adb shell am start -n com.mariokernich.nextset/.MainActivity --es demo running` (Uhr: `…/com.mariokernich.nextset.wear.MainActivity`). Werte: `idle`, `running`, `countdown` (mit `--el demoEnd <Unix-Sekunden>`), `paused`, `finished`, `settings`.
- **Icons neu erzeugen:** `cd Branding && npm install --no-save playwright && node build-icons.mjs`
- **Webseite:** `website/` enthält die Support-Seite und die Datenschutzerklärung, auf Englisch und unter `de/` auf Deutsch. `.github/workflows/website.yml` lädt sie bei Pushes auf `main` per FTPS nach [apps.kernich.de/nextset](https://apps.kernich.de/nextset/) hoch, manuell geht es über *Actions → Website → Run workflow*. Nötig sind die Repository-Secrets `FTP_USERNAME` und `FTP_PASSWORD`.
- **CI:** `.github/workflows/ios.yml` baut bei Pushes auf `main` und bei Pull Requests alle Targets auf einem macOS-26-Runner und führt die Tests aus, sobald sich etwas unter `ios/` ändert. Er nimmt das neueste Xcode 27 des Runner-Images, solange es keines gibt, dessen Standard-Xcode. Screenshots vom Simulator gibt es auf Knopfdruck: *Actions → iOS → Run workflow* (alle, nur iPhone oder nur Watch). Sie landen als Artefakt am Lauf. Hinweis: Bei privaten Repos zählen macOS-Minuten zehnfach zum Actions-Kontingent. `.github/workflows/android.yml` testet und baut die Android-Apps auf einem Linux-Runner, sobald sich etwas unter `android/` ändert.

## App Store

`.github/workflows/app-store.yml` archiviert die App samt Watch-App und Widget, lädt den Build zu App Store Connect hoch und reicht ihn auf Wunsch zur Prüfung ein: *Actions → App Store → Run workflow*, fürs Einreichen mit Häkchen bei *Submit for App Review*. Die Build-Nummer ist die Laufnummer des Workflows plus 1, weil Build 1 aus Xcode kam.

Dafür braucht der Workflow einmalig einen API-Schlüssel für App Store Connect:

1. In App Store Connect unter *Benutzer und Zugriff → Integrationen → App Store Connect API → Team-Schlüssel* einen Schlüssel mit der Rolle **Admin** erzeugen, damit Xcode Zertifikate und Profile anlegen darf, und die `.p8`-Datei herunterladen. Sie lässt sich nur einmal laden.
2. Im Repo unter *Settings → Secrets and variables → Actions* drei Secrets anlegen: `ASC_KEY_ID` (Schlüssel-ID), `ASC_ISSUER_ID` (Aussteller-ID über der Schlüsselliste) und `ASC_KEY_P8` (der komplette Inhalt der `.p8`-Datei).

Einreichen klappt erst, wenn in App Store Connect alles Übrige erledigt ist: Verträge, Steuer- und Bankdaten, Händlerstatus nach dem Digital Services Act, App-Datenschutz, Altersfreigabe, Texte und Screenshots. Fehlt etwas, listet der Workflow es im Log auf.

## Google Play

Smartphone- und Uhr-App sind **ein** Eintrag in der Play Console (Paketname `com.mariokernich.nextset`); die Wear-OS-App kommt über den Formfaktor *Wear OS* in einen eigenen Release-Track. Die Versionsnummern stehen in `android/gradle.properties` (`nextset.versionCode` bei jedem Release erhöhen, `nextset.versionName` wie unter iOS).

**Upload-Schlüssel anlegen** (einmalig, gut sichern – ohne ihn lassen sich keine Updates hochladen):

```
keytool -genkeypair -v -keystore nextset-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

Lokal liest Gradle den Schlüssel aus `android/keystore.properties` (steht in `.gitignore`):

```
storeFile=/Pfad/zu/nextset-upload.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

`./gradlew :app:bundleRelease :wear:bundleRelease` erzeugt dann die signierten Bundles unter `android/*/build/outputs/bundle/release/`. Alternativ im Repo die Secrets `NEXTSET_KEYSTORE_BASE64` (`base64 -i nextset-upload.jks`), `NEXTSET_KEYSTORE_PASSWORD`, `NEXTSET_KEY_ALIAS` und `NEXTSET_KEY_PASSWORD` anlegen und *Actions → Android → Run workflow* mit Häkchen bei *Signed release bundles* starten; die `.aab`-Dateien hängen dann am Lauf. Die erste Version muss von Hand in der Play Console hochgeladen werden, dabei *Play App Signing* aktivieren.

In der Play Console abzugeben:
- **Exakte Wecker:** Die App nutzt `USE_EXACT_ALARM` als Timer-App (Pausenende auf die Sekunde).
- **Vordergrunddienst (Wear OS):** Typ *special use* – hält Countdown-Vibration und Ende-Signal während einer Pause am Handgelenk; am besten mit kurzem Video.
- **Datensicherheit:** Es werden keine Daten erhoben oder geteilt. Datenschutzerklärung: [apps.kernich.de/nextset/privacy](https://apps.kernich.de/nextset/privacy/).
- **Zielgruppe & Inhalt, Einstufung:** wie im App Store (keine Werbung, kein Login, Altersfreigabe für alle).
