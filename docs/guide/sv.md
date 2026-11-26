# Installationsguide för SeeClap Launcher

På en tv eller box med Android TV 7 eller senare visar SeeClap Launcher dina favoritappar och de program som apparna delar. Den första rutan öppnar SeeClap eller erbjuder dig att installera appen.

## Med en dator och en AI-assistent

Du kan kontrollera din egen tv och tömma cacheminnena redan innan den första APK-filen publiceras. Kitet bygger på metoden från [CleanYourTV](https://github.com/Gathin23/cleanyourtv). När en officiell APK-fil finns kan kitet också hjälpa dig att installera startskärmen.

1. Anslut tv:n och datorn till samma privata nätverk. Förbered Claude Code eller Codex med terminalåtkomst på datorn samt Android SDK Platform-Tools, som innehåller `adb`.
2. Öppna **Inställningar › System › Om** på tv:n och tryck sju gånger på versionsnumret för systembygget för att visa utvecklaralternativen. Aktivera trådlös felsökning eller nätverksfelsökning tillfälligt om tv:n erbjuder det. Följ anvisningarna för parkoppling och bekräfta datorns anslutning på tv:n. Menynamnen varierar mellan olika modeller.
3. Kopiera instruktionerna från det [fullständiga AI-kitet på svenska](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/blob/main/kit/PROMPT.sv.md) till assistenten och lägg till din begäran. Du kan kopiera det här exemplet.

> Kontrollera min tv-modell och mät det lediga utrymmet före och efter tömningen av cacheminnena. Behåll mina appdata och inloggningar. Använd inte `pm clear`, som raderar en apps data. Visa mig vilken app det gäller innan du stänger av eller avinstallerar något och invänta mitt val.

Hur mycket utrymme som frigörs beror på cacheminnena. Det kan bli noll. Stäng av felsökningen i tv:ns menyer när du är klar. Om ADB inte är tillgängligt kan du följa den manuella metoden nedan. För att installera startskärmen behövs en officiell APK-fil, som blir tillgänglig från den första utgåvan.

## Installera startskärmen

1. Öppna **Releases** i det officiella GitHub-projektet för SeeClap Launcher. Välj den senaste utgåvan och fäll ut **Assets**. Före den första utgåvan finns det ingen APK-fil där. En utgåva för Google Play är planerad.
2. Hämta `seeclap-launcher-<code>.apk`, där `<code>` är versionsnumret. Använd tv:ns webbläsare eller kopiera APK-filen från en dator till ett USB-minne och öppna den på tv:n med en filhanterare.
3. Om Android blockerar installationen öppnar du inställningarna som visas. På Android 8 eller senare ger du appen som öppnar filen tillstånd att **installera okända appar**. På Android 7 aktiverar du tillfälligt **Okända källor** i säkerhetsinställningarna.
4. Gå tillbaka till filen, bekräfta med **Installera** och stäng sedan av samma behörighet. Öppna **SeeClap Launcher** bland tv:ns appar.

## Första starten

1. Välj **Kom igång**. Bläddra bland temana med Vänster och Höger och tryck OK för att välja. **Automatiskt** följer tv:ns tema. Du kan också välja **Ljust** eller **Mörkt**.
2. Om SeeClap saknas väljer du **Installera** eller **Senare**. Startskärmen öppnar den tillgängliga butiken (Google Play eller Amazon på Fire TV). APK-utgåvan kan också hämta SeeClap från den officiella webbplatsen och verifiera signaturen. Bekräfta installationen i Android. Play-utgåvan använder butiken för installationer och uppdateringar.
3. Med standardlayouten kan du välja upp till fem andra favoriter, eller fyra med stora rutor. Välj **Fortsätt** för att spara dina val.
4. Om Android frågar vilken hemapp som ska vara standard för hemknappen väljer du SeeClap Launcher. Om guiden erbjuder **Öppna Tillgänglighet**, läs beskrivningen av tjänsten. Välj **På** för att fortsätta och aktivera sedan **SeeClap Launcher – hemknappen** i Android. **Avbryt** stänger frågan. Välj **Senare** för att hoppa över steget.
5. När tjänsten är aktiv trycker du på den knapp på fjärrkontrollen som du vill använda. Ett kort tryck öppnar sedan SeeClap. Ett långt tryck kan öppna den ursprungliga appen om den känns igen, är installerad och alternativet är på under **Inställningar › Fjärrkontroll**.
6. Kör rensningen om du vill eller välj **Fortsätt** och sedan **Till hemskärmen**. Du kan gå igenom dina val igen under **Inställningar › Kör guiden igen**.

## Använd fjärrkontrollen och apparna

- Bläddra bland favoriterna med Vänster och Höger och öppna en app med OK. Upp tar dig till tillgängliga program eller uppspelningsknappar, eller till menyn högst upp om inget sådant finns. Ned tar dig till rutnätet med appar.
- Håll in OK eller tryck Meny på en annan app för att lägga till den bland favoriterna, flytta den bland favoriterna eller dölja den. Om Android ber om behörighet att läsa tv-program kan du tillåta det för att se programkorten som den markerade appen delar.

## Frigör lagringsutrymme

Under **Inställningar › Rensa och snabba upp** listar **Snabbrensning** dina appar efter filstorlek. Välj en app för att begära avinstallation och bekräfta i Android.

**Grundlig rensning** använder lokal felsökning på tv:n. Gå igenom alternativen innan du börjar. Du kan bland annat tömma cacheminnen och stänga av utvalda förinstallerade appar. Med **Avstängda appar** kan du slå på appar som startskärmen har stängt av. Raderade cachedata går inte att återställa.

## Gå tillbaka till den ursprungliga hemskärmen

Om du aktiverade tillgänglighetstjänsten väljer du **Inställningar › Hemknappen › Pausa** och trycker sedan på hemknappen. **Öppna ursprunglig hemskärm** visas också när en annan hemapp är standard. Om SeeClap Launcher är standard väljer du tillverkarens hemapp i Androids inställningar för standardappar. Menyerna varierar mellan olika tv-apparater.

## Integritet och användningsvillkor

Dina inställningar sparas på tv:n och kan ingå i Androids säkerhetskopia. Startskärmen läser programmen lokalt. Bilder kan hämtas via HTTPS och sparas i cachen. APK-utgåvan ansluter även till `downloads.seeapps.studio` för installationer och uppdateringar. Startskärmen har varken annonser eller användarstatistik. Tillgänglighetstjänsten känner av knapptryckningar och när den ursprungliga hemskärmen öppnas, utan att läsa skärmens innehåll.

Licensen tillåter personligt, icke-kommersiellt bruk av programvaran i oförändrat skick. Du får dela länken till det officiella projektet. Vidare distribution av programvaran kräver skriftligt förhandstillstånd från See Apps Studio. SeeClap ligger alltid först. Rutan går inte att flytta eller dölja. SeeClap kan inte tas bort eller avinstalleras från startskärmen. Låt SeeClap vara installerad så länge du använder startskärmen.

Behöver du hjälp kan du skriva till [support@seeapps.studio](mailto:support@seeapps.studio).
