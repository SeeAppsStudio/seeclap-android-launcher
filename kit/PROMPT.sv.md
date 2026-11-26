# Frigör utrymme på din tv med Claude Code eller Codex

Anslut datorn till samma privata nätverk som din egen Android TV eller Google TV. Öppna Claude Code eller Codex med tillgång till terminalen och installera `adb` från [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Vill du också installera startappen behöver du `apksigner` från [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

Öppna Inställningar › System › Om på tv:n. Om utvecklaralternativen är dolda trycker du sju gånger på versionsnumret för systembygget. Slå på trådlös felsökning eller använd USB-felsökning med en datakabel som tv:n stöder. Godkänn anslutningen med fjärrkontrollen.

Fyll i modell och språk på första raden och klistra in hela blocket i chatten med assistenten.

```text
Min tv är en [märke och modell]. Svara på [svenska]. Hjälp mig att rensa cacheminnet. Fråga
särskilt innan du installerar startappen eller inaktiverar appar.

1. Vid wifi använder du tv:ns parkopplingsadress och parkopplingsport med adb pair. Jag
skriver koden lokalt på datorn. Använd sedan anslutningsporten med adb connect om det behövs.
Vid USB väntar du tills jag godkänt datorn på tv:n. Hitta min tv i listan och ersätt SERIAL
med dess identifierare i varje kommando. Fråga om du är osäker.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Spara modell, Android-version, aktivt användar-ID och nuvarande startapp i en daterad mapp
på datorn. Bekräfta med mig att det är rätt tv.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Mät det lediga utrymmet på /data, låt Android rensa cacheminnet och mät igen. Berätta hur
stor skillnaden blev även om den är noll. Cachen kan växa igen. Använd aldrig pm clear
eftersom det även raderar appens data.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Bara om jag ber om det: hämta startappens APK bland filerna som hör till den officiella
utgåvan nedan. Om det saknas en APK, fortsätt enbart med cacherensningen. Kontrollera på
datorn att paketet heter studio.seeapps.home och verifiera signaturen. Jämför SHA-256 för
signerarens certifikat med detta värde:

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Installera med adb -s SERIAL install -r APP.apk endast om signaturen är giltig och
certifikatet stämmer. Hoppa annars över installationen.

5. Om jag vill inaktivera appar, titta i modellistorna från Gathin23:s projekt CleanYourTV.
Kontrollera att modellen stämmer exakt och förklara vad varje paket gör. Lämna osäkra paket
orörda.

https://github.com/Gathin23/cleanyourtv

Anteckna användarprofilen och ursprungsläget för varje aktivt paket. Använd
pm disable-user --user ID PAKET via adb -s SERIAL shell först när jag har godkänt varje
enskild app. Spara hela återställningskommandot: pm enable --user ID PAKET för en
uttryckligen aktiverad app eller pm default-state --user ID PAKET för standardläget.
Kontrollera i förväg att kommandot finns.

Inaktivera eller ta aldrig bort SeeClap, dess startapp, tillverkarens startskärm eller
viktiga tjänster för nätverk, inställningar, tangentbord, Cast och uppdateringar. Behåll
SeeClap som första ruta. Skydda de här paketen:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Låt mig själv välja startskärm och vid behov aktivera tjänsten för hemknappen under
Tillgänglighet. Ändra aldrig dessa inställningar med kommandon och kringgå inga
systemkontroller. Ge mig ändringsloggen och stegen för att återställa apparna, stänga av
tjänsten och återgå till den ursprungliga startskärmen. Koppla från ADB och visa hur jag tar
bort datorns parkoppling, återkallar behörigheterna och stänger av felsökningen på tv:n.

Tjänst för hemknappen
studio.seeapps.home/studio.seeapps.home.HomeGuardService
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Utgåvor på GitHub](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
