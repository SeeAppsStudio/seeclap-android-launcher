# Ruimte vrijmaken op je tv met Claude Code of Codex

Verbind je computer met hetzelfde privénetwerk als je eigen Android TV of Google TV. Open Claude Code of Codex met toegang tot de terminal en installeer `adb` uit [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Om ook de launcher te installeren heb je `apksigner` uit [Android SDK Build-Tools](https://developer.android.com/tools/apksigner) nodig.

Open op de tv Instellingen › Systeem › Over. Zijn de ontwikkelaarsopties verborgen, druk dan zeven keer op het buildnummer. Schakel draadloze foutopsporing in, of gebruik USB-foutopsporing met een geschikte datakabel. Houd de afstandsbediening bij de hand om de verbinding goed te keuren.

Vul op de eerste regel je model en taal in en plak het hele blok in het gesprek met de assistent.

```text
Mijn tv is een [merk en model]. Antwoord in het [Nederlands]. Help me het cachegeheugen op te
schonen. Vraag apart toestemming om de launcher te installeren of apps uit te schakelen.

1. Gebruik bij wifi het koppeladres en de koppelpoort op mijn tv voor adb pair. De code voer
ik zelf op de computer in. Gebruik zo nodig daarna de verbindingspoort op de tv voor
adb connect. Wacht bij USB tot ik de computer op de tv heb toegestaan. Zoek mijn tv in de
lijst en gebruik de apparaat-ID in plaats van SERIAL bij elke opdracht. Vraag het me bij
twijfel.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Noteer model, Android-versie, actieve gebruikers-ID en de huidige startscherm-app in een
map met datum op mijn computer. Controleer samen met mij of dit de juiste tv is.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Meet de vrije ruimte op /data, vraag Android zijn caches op te schonen en meet opnieuw.
Geef het gemeten verschil, ook als het nul is. Caches kunnen weer aangroeien. Gebruik nooit
pm clear, want dat wist ook de appgegevens.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Download de APK van de launcher alleen op mijn verzoek en alleen uit de bestanden bij de
officiële release hieronder. Staat er geen APK, ga dan alleen verder met de cache. Controleer
op de computer of de pakketnaam studio.seeapps.home is en of de handtekening geldig is.
Vergelijk de SHA-256 van het certificaat van de ondertekenaar met deze waarde:

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Installeer alleen bij een geldige handtekening en een overeenkomend certificaat met
adb -s SERIAL install -r APP.apk. Sla de installatie anders over.

5. Wil ik apps uitschakelen, gebruik dan de modellijsten van CleanYourTV van Gathin23.
Controleer of het exacte model overeenkomt en leg uit wat elk pakket doet. Sla
twijfelgevallen over.

https://github.com/Gathin23/cleanyourtv

Noteer het gebruikersprofiel en de oorspronkelijke staat van elk ingeschakeld pakket. Gebruik
pas na mijn toestemming per app pm disable-user --user ID PAKKET via adb -s SERIAL shell.
Bewaar de volledige herstelopdracht: pm enable --user ID PAKKET voor een uitdrukkelijk
ingeschakelde app of pm default-state --user ID PAKKET voor de standaardinstelling.
Controleer vooraf of die opdracht beschikbaar is.

Schakel SeeClap, de launcher, het startscherm van de fabrikant en essentiële diensten zoals
netwerk, instellingen, toetsenbord, Cast en updates nooit uit en verwijder ze ook niet. Laat
SeeClap als eerste tegel staan. Bescherm deze pakketten:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Laat mij zelf het startscherm kiezen en desgewenst de dienst voor de Home-knop aanzetten
bij Toegankelijkheid. Wijzig daar niets via opdrachten en omzeil geen systeembeveiliging.
Geef me het logboek en de stappen om de apps te herstellen, de dienst uit te zetten en het
oorspronkelijke startscherm terug te kiezen. Verbreek ADB en laat zien hoe ik de computer
vergeet, de machtigingen voor foutopsporing intrek en foutopsporing op de tv uitzet.

Dienst voor de Home-knop
studio.seeapps.home/studio.seeapps.home.HomeGuardService
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Versies op GitHub](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
