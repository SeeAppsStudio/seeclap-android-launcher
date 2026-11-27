# Liberare spazio sulla TV con Claude Code o Codex

Collega il computer alla stessa rete privata della tua Android TV o Google TV. Apri Claude Code o Codex con accesso al terminale e installa `adb` dagli [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Per installare anche il launcher ti serve `apksigner`, disponibile negli [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

Sulla TV apri Impostazioni › Sistema › Informazioni. Se le opzioni sviluppatore sono nascoste, premi sette volte sul numero build. Attiva il debug wireless oppure usa il debug USB con un cavo dati compatibile. Tieni il telecomando a portata di mano per autorizzare la connessione.

Inserisci modello e lingua nella prima riga e incolla tutto il blocco nella chat dell’assistente.

```text
La mia TV è una [marca e modello]. Rispondimi in [italiano]. Aiutami a liberare le cache.
Chiedimi separatamente se voglio installare il launcher o disattivare qualche app.

1. Per il Wi-Fi usa con adb pair l’indirizzo e la porta di accoppiamento mostrati dalla TV.
Inserirò il codice sul computer. Se serve, usa poi la porta di connessione con adb connect.
Con USB attendi che autorizzi il computer sulla TV. Individua la mia TV nell’elenco e
sostituisci SERIAL con il suo identificativo in ogni comando. Nel dubbio, chiedimi.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Salva sul computer, in una cartella con la data, il modello, la versione di Android, l’ID
dell’utente attivo e l’app Home attuale. Verifica con me che sia la TV giusta.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Misura lo spazio libero su /data, chiedi ad Android di ridurre le cache e misura di nuovo.
Dimmi la differenza osservata, anche se è zero. Le cache possono ricrearsi. Non usare mai
pm clear, perché cancella anche i dati dell’app.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Solo se te lo chiedo, scarica sul computer l’edizione completa ufficiale dal link qui sotto. Verifica che il pacchetto sia studio.seeapps.home.

https://downloads.seeapps.studio/launcher/tv-full.apk

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Usa adb -s SERIAL install -r APP.apk soltanto se la verifica riesce e il certificato
corrisponde. Altrimenti salta l’installazione.

5. Se voglio disattivare delle app, consulta gli elenchi per modello di CleanYourTV di
Gathin23. Controlla che il modello sia quello esatto e spiegami a cosa serve ogni pacchetto.
Tralascia i casi dubbi.

https://github.com/Gathin23/cleanyourtv

Annota il profilo e lo stato iniziale di ogni pacchetto attivo. Con il mio consenso per
ciascuna app, usa pm disable-user --user ID PACCHETTO tramite adb -s SERIAL shell. Salva il
comando di ripristino completo: pm enable --user ID PACCHETTO per un’app attivata
esplicitamente o pm default-state --user ID PACCHETTO per lo stato predefinito. Verifica
prima che il comando sia disponibile.

Non disattivare né rimuovere mai SeeClap, il suo launcher, la Home del produttore o servizi
essenziali come rete, impostazioni, tastiera, Cast e aggiornamenti. Lascia SeeClap al primo
posto. Proteggi questi pacchetti:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Con l’edizione completa, lascia che attivi personalmente il servizio facoltativo del tasto Home nelle impostazioni di accessibilità della TV e che scelga la scorciatoia nelle impostazioni del telecomando.
Lascia che scelga l’app Home nelle impostazioni Android. Se l’opzione manca, mostrami come aprire SeeClap Launcher dalle app della TV. Segnala eventuali blocchi, consegnami il registro e i passaggi per annullare le modifiche, quindi scollega ADB.
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Versioni su GitHub](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
