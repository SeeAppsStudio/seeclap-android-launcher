# Mit Claude Code oder Codex Speicherplatz auf dem Fernseher freigeben

Verbinde den Computer mit demselben privaten Netzwerk wie deinen eigenen Android-TV oder Google-TV. Öffne Claude Code oder Codex mit Terminalzugriff. Installiere `adb` aus den [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Für die optionale Installation des Launchers brauchst du auch `apksigner` aus den [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

Öffne am Fernseher Einstellungen › System › Info. Sind die Entwickleroptionen ausgeblendet, drücke siebenmal auf die Build-Nummer. Aktiviere das WLAN-Debugging oder verwende USB-Debugging mit einem geeigneten Datenkabel. Bestätige die Verbindung mit der Fernbedienung.

Trage in der ersten Zeile Modell und Sprache ein und füge den ganzen Block in den Chat des Assistenten ein.

```text
Mein Fernseher: [Marke und Modell]. Antworte auf [Deutsch]. Hilf mir, den Cache zu leeren.
Frage mich gesondert, bevor du den Launcher installierst oder Apps deaktivierst.

1. Nutze bei WLAN die angezeigte Kopplungsadresse mit Kopplungsport für adb pair. Den Code
gebe ich lokal ein. Verwende bei Bedarf den angezeigten Verbindungsport für adb connect.
Warte bei USB auf meine Bestätigung am Fernseher. Ermittle die Gerätekennung und ersetze
SERIAL in jedem Befehl damit. Frage nach, falls das Gerät nicht eindeutig ist.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Notiere Modell, Android-Version, aktive Benutzer-ID und aktuelle Startbildschirm-App in
einem datierten Ordner auf meinem Computer. Kläre mit mir, ob es der richtige Fernseher ist.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Miss den freien Platz auf /data. Lass Android seine Caches verkleinern und miss noch
einmal. Nenne die beobachtete Differenz, auch wenn sie null ist. Caches können wieder
wachsen. Verwende niemals pm clear, denn damit werden auch App-Daten gelöscht.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Nur auf meinen Wunsch: Lade die Launcher-APK aus den Anhängen der offiziellen
Veröffentlichung unten. Gibt es keine APK, fahre allein mit der Cache-Bereinigung fort. Prüfe
auf meinem Computer den Paketnamen studio.seeapps.home und die Signatur. Vergleiche den
SHA-256-Fingerabdruck des Signaturzertifikats mit diesem Wert:

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Installiere nur bei gültiger Signatur und passendem Zertifikat mit
adb -s SERIAL install -r APP.apk. Andernfalls überspringe die Installation.

5. Wenn ich Apps deaktivieren möchte, ziehe die Modelllisten von CleanYourTV (Gathin23)
heran. Prüfe das genaue Modell und erkläre die Aufgabe jedes Pakets. Lass unklare Fälle aus.

https://github.com/Gathin23/cleanyourtv

Notiere Benutzer-ID und ursprünglichen Zustand jeder aktiven App. Erst nach meiner Zustimmung
pro App: pm disable-user --user ID PAKET über adb -s SERIAL shell. Notiere den vollständigen
Rückweg: pm enable --user ID PAKET bei ausdrücklich aktivierten Apps,
pm default-state --user ID PAKET beim Standardzustand. Prüfe vorher, ob der Rücksetzbefehl
verfügbar ist.

Deaktiviere oder entferne niemals SeeClap, den SeeClap Launcher, den Startbildschirm des
Herstellers oder wichtige Dienste wie Netzwerk, Einstellungen, Tastatur, Cast und Updates.
SeeClap bleibt die erste Kachel. Schütze diese Pakete:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Die Start-App wähle ich in den Android-Einstellungen. Falls die Auswahl fehlt, zeige mir, wie ich SeeClap Launcher aus der App-Liste öffne. Melde Installationsblockaden, führe das Änderungsprotokoll mit Rückgängig-Schritten und trenne danach ADB.
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Versionen auf GitHub](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
