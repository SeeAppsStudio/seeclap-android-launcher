# SeeClap Launcher installieren und einrichten

Auf einem Fernseher oder einer Box mit Android TV ab Version 7 zeigt SeeClap Launcher deine Lieblings-Apps und die von ihnen freigegebenen Programme. Die erste Kachel öffnet SeeClap oder bietet die Installation an.

## Mit einem Computer und einem KI-Assistenten

Du kannst deinen eigenen Fernseher prüfen und seine Caches leeren, schon bevor die erste APK erscheint. Das Kit verwendet die Methode von [CleanYourTV](https://github.com/Gathin23/cleanyourtv). Sobald eine offizielle APK verfügbar ist, hilft es dir auch bei der Installation des Launchers.

1. Verbinde Fernseher und Computer mit demselben privaten Netzwerk. Richte auf dem Computer Claude Code oder Codex mit Terminalzugriff und die Android SDK Platform-Tools ein. Sie enthalten `adb`.
2. Öffne am Fernseher **Einstellungen › System › Info** und drücke siebenmal auf die Build-Nummer, um die Entwickleroptionen einzublenden. Aktiviere vorübergehend das WLAN- oder Netzwerk-Debugging, falls dein Modell es anbietet. Folge den Hinweisen zur Kopplung und bestätige die Verbindung deines Computers am Fernseher. Die Menünamen können abweichen.
3. Kopiere die Anweisungen aus dem [vollständigen deutschen KI-Kit](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/blob/main/kit/PROMPT.de.md) in deinen Assistenten und füge deinen Auftrag hinzu. Diesen Text kannst du kopieren.

> Prüfe das Modell meines Fernsehers und miss den freien Speicherplatz vor und nach dem Leeren der Caches. Behalte meine App-Daten und Anmeldungen bei. Verwende kein `pm clear`, denn das löscht die Daten einer App. Zeig mir vor jedem Deaktivieren oder Deinstallieren die betreffende App und warte auf meine Entscheidung.

Wie viel Speicher frei wird, hängt von den Caches ab. Es kann auch gar kein Platz frei werden. Schalte das Debugging zum Schluss im TV-Menü aus. Ist ADB nicht verfügbar, nutze die manuelle Methode im nächsten Abschnitt. Für die Installation des Launchers brauchst du eine offizielle APK. Sie ist ab der ersten Veröffentlichung verfügbar.

## Launcher installieren

1. Öffne im offiziellen GitHub-Repository von SeeClap Launcher den Bereich **Releases**. Wähle die neueste Version und klappe **Assets** auf. Vor der ersten Veröffentlichung gibt es dort keine APK-Datei. Eine Ausgabe für Google Play ist geplant.
2. Lade `seeclap-launcher-<code>.apk` herunter. `<code>` steht für die Versionsnummer. Nutze den Browser des Fernsehers oder kopiere die APK vom Computer auf einen USB-Stick und öffne sie am Fernseher mit einem Dateimanager.
3. Blockiert Android die Installation, öffne die angebotenen Einstellungen. Erlaube unter Android 8 oder neuer der App, die die Datei öffnet, **unbekannte Apps zu installieren**. Aktiviere unter Android 7 vorübergehend **Unbekannte Quellen** in den Sicherheitseinstellungen.
4. Kehre zur Datei zurück, bestätige **Installieren** und schalte dieselbe Berechtigung anschließend wieder aus. Öffne **SeeClap Launcher** über die App-Liste des Fernsehers.

## Erster Start

1. Wähle **Los geht's**. Sieh dir mit Links und Rechts die Designs an und bestätige mit OK. **Automatisch** übernimmt das Design des Fernsehers. Zur Auswahl stehen auch **Hell** und **Dunkel**.
2. Fehlt SeeClap, wähle **Installieren** oder **Später**. Der Launcher öffnet den verfügbaren Store (Google Play oder Amazon auf Fire TV). Die APK-Ausgabe kann SeeClap auch von der offiziellen Website laden und die Signatur prüfen. Bestätige die Installation in Android. Die Play-Ausgabe nutzt für Installationen und Updates den Store.
3. In der Standardeinstellung kannst du bis zu fünf weitere Favoriten wählen, mit großen Kacheln bis zu vier. Mit **Weiter** speicherst du deine Auswahl.
4. Fragt Android nach der Standard-Start-App für die Home-Taste, wähle SeeClap Launcher. Bietet der Assistent **Bedienungshilfen öffnen** an, lies die Beschreibung des Dienstes. Wähle **Ein**, um fortzufahren, und aktiviere dann in Android **SeeClap Launcher – Home-Taste**. **Abbrechen** schließt die Anfrage. Mit **Später** überspringst du diesen Schritt.
5. Drücke bei aktivem Dienst die gewünschte Taste der Fernbedienung. Ein kurzer Druck öffnet dann SeeClap. Langes Drücken kann die ursprüngliche App öffnen, wenn sie erkannt wird, installiert ist und die Option unter **Einstellungen › Fernbedienung** eingeschaltet ist.
6. Starte auf Wunsch die Bereinigung oder wähle **Weiter** und anschließend **Zum Startbildschirm**. Unter **Einstellungen › Einrichtung neu starten** kannst du deine Auswahl erneut durchgehen.

## Fernbedienung und Apps bedienen

- Mit Links und Rechts wechselst du zwischen den Favoriten. OK öffnet eine App. Mit Hoch erreichst du Programme oder Wiedergabetasten, falls verfügbar, sonst das obere Menü. Mit Runter kommst du zum App-Raster.
- Halte auf einer anderen App OK gedrückt oder drücke Menü, um sie zu den Favoriten hinzuzufügen, innerhalb der Favoriten zu verschieben oder auszublenden. Fragt Android nach Zugriff auf die TV-Programme, erteile die Berechtigung, wenn du die Programmkarten der ausgewählten App sehen möchtest.

## Speicherplatz freigeben

Unter **Einstellungen › Aufräumen und beschleunigen** listet **Schnell aufräumen** deine Apps nach Dateigröße auf. Wähle eine App aus und bestätige die Deinstallation in Android.

**Gründlich aufräumen** nutzt die lokale Debugging-Verbindung des Fernsehers. Prüfe die Optionen vor dem Start. Du kannst unter anderem Caches leeren und ausgewählte vorinstallierte Apps deaktivieren. Unter **Ausgeschaltete Apps** lassen sich vom Launcher deaktivierte Apps wieder einschalten. Gelöschte Cache-Daten lassen sich nicht wiederherstellen.

## Zum ursprünglichen Startbildschirm zurückkehren

Nutzt du den Dienst für Bedienungshilfen, wähle **Einstellungen › Home-Taste › Pausieren** und drücke die Home-Taste. **Ursprünglichen Start öffnen** ist ebenfalls verfügbar, wenn eine andere Start-App als Standard festgelegt ist. Ist SeeClap Launcher der Standard, wähle in den Android-Einstellungen für Standard-Apps die Start-App des Herstellers. Die Menünamen hängen vom Fernseher ab.

## Datenschutz und Nutzungsbedingungen

Deine Einstellungen werden auf dem Fernseher gespeichert. Android kann sie in seine Sicherung aufnehmen. Der Launcher liest die Programme auf dem Gerät, kann Bilder über HTTPS abrufen und sie zwischenspeichern. Die APK-Ausgabe kontaktiert für Installationen und Updates außerdem `downloads.seeapps.studio`. Der Launcher ist werbefrei und erfasst keine Nutzungsstatistiken. Der Dienst für Bedienungshilfen erkennt Tastendrücke und das Öffnen des ursprünglichen Startbildschirms, ohne Bildschirminhalte zu lesen.

Die Lizenz erlaubt die persönliche, nicht kommerzielle Nutzung der unveränderten Software. Den offiziellen Projektlink darfst du teilen. Die Weiterverbreitung der Software erfordert die vorherige schriftliche Zustimmung von See Apps Studio. SeeClap bleibt an erster Stelle. Die Kachel lässt sich weder verschieben noch ausblenden. SeeClap kann im Launcher weder entfernt noch deinstalliert werden. Lass SeeClap nach der Installation installiert, solange du den Launcher nutzt.

Hilfe bekommst du unter [support@seeapps.studio](mailto:support@seeapps.studio).
