# Libérer de la place sur sa TV avec Claude Code ou Codex

Sur un ordinateur relié au même réseau privé que votre propre Android TV ou Google TV, ouvrez Claude Code ou Codex avec accès au terminal. Installez `adb` depuis [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Pour installer aussi le launcher, il vous faudra `apksigner` dans [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

Sur la TV, ouvrez Réglages › Système › À propos et appuyez sept fois sur le numéro de build si les options pour les développeurs sont masquées. Activez le débogage sans fil, ou le débogage USB avec un câble de données compatible. Gardez la télécommande pour accepter la connexion.

Remplacez le modèle et la langue sur la première ligne. Copiez tout le bloc et collez-le dans la conversation de l’agent.

```text
Ma TV : [marque et modèle]. Réponds en [français]. Aide-moi à nettoyer son cache. Demande-moi
séparément si je veux installer le launcher ou désactiver des apps.

1. En Wi-Fi, utilise l’adresse et le port d’appairage affichés sur ma TV avec adb pair. Je
saisirai le code en local. Si nécessaire, utilise ensuite son port de connexion avec
adb connect. En USB, attends que j’accepte l’ordinateur sur la TV. Repère ma TV dans la liste
et utilise son identifiant à la place de SERIAL pour chaque action. Si tu hésites,
demande-moi.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Note dans un dossier daté sur l’ordinateur le modèle, la version Android, le numéro du
profil actif et l’accueil actuel. Vérifie avec moi que c’est la bonne TV.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Mesure l’espace disponible sur /data, demande à Android de libérer ses caches, puis refais
la même mesure. Donne-moi la différence observée, même si elle est nulle. Les caches peuvent
se reconstituer. N’utilise jamais pm clear, qui efface aussi les données de l’app.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Uniquement si je le demande, cherche l’APK du launcher dans les fichiers joints à la
version officielle ci-dessous. Sans APK, poursuis le nettoyage seul. Vérifie le paquet
studio.seeapps.home et la signature du fichier téléchargé sur l’ordinateur. Compare le
SHA-256 du certificat du signataire à cette valeur :

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Installe avec adb -s SERIAL install -r APP.apk seulement si la vérification réussit et que le
certificat correspond. Sinon, laisse l’installation de côté.

5. Si je veux désactiver des applications, consulte les listes par modèle de CleanYourTV
(Gathin23). Vérifie la correspondance exacte et explique le rôle de chaque paquet. Si le
modèle ou le rôle est incertain, passe ton tour.

https://github.com/Gathin23/cleanyourtv

Note le profil et l’état initial de chaque paquet activé. Avec mon accord pour chaque app,
utilise pm disable-user --user ID PAQUET via adb -s SERIAL shell. Consigne la commande
inverse complète : pm enable --user ID PAQUET pour une app explicitement activée,
pm default-state --user ID PAQUET pour l’état par défaut. Vérifie qu’elle est disponible
avant la modification.

Ne désactive ni ne retire SeeClap, son launcher, l’accueil du constructeur ou les services
essentiels (réseau, réglages, clavier, Cast, mises à jour). Garde SeeClap en première tuile.
Paquets SeeClap à protéger :

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Laisse-moi choisir l’accueil dans les réglages d’Android. Si le choix est indisponible, montre-moi comment ouvrir SeeClap Launcher depuis les applications de la TV. Signale tout blocage d’installation. Remets-moi le journal et les étapes pour annuler les changements. Déconnecte ADB et montre-moi comment révoquer les autorisations de débogage.
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Versions sur GitHub](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
