# SeeClap Launcher — guide d’installation

Sur une TV ou une box sous Android TV 7 ou plus récent, SeeClap Launcher réunit vos apps favorites et les programmes qu’elles partagent. La première tuile ouvre SeeClap, ou propose de l’installer.

## Avec un ordinateur et une IA

Vous pouvez faire le point sur votre TV personnelle et nettoyer ses caches avant même la publication du premier APK. Le kit s’inspire de la méthode de [CleanYourTV](https://github.com/Gathin23/cleanyourtv). Il permet aussi d’installer le launcher quand un APK officiel est disponible.

1. Reliez la TV et l’ordinateur au même réseau privé. Sur l’ordinateur, préparez Claude Code ou Codex avec accès au terminal, ainsi que les Android SDK Platform-Tools, qui contiennent `adb`.
2. Sur la TV, ouvrez **Réglages › Système › À propos** et appuyez sept fois sur le numéro de build pour afficher les options pour les développeurs. Activez temporairement le débogage sans fil ou réseau si la TV le propose. Suivez les indications d’appairage et confirmez la connexion de votre ordinateur sur la TV. Les noms des menus varient selon le modèle.
3. Copiez les instructions du [kit IA complet en français](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/blob/main/kit/PROMPT.fr.md) dans votre assistant, puis ajoutez votre demande. Par exemple, vous pouvez copier ce texte.

> Vérifie le modèle de ma TV et mesure l’espace libre avant et après le nettoyage des caches. Préserve mes données et mes connexions. N’utilise pas `pm clear`, qui efface les données d’une app. Avant toute désactivation ou désinstallation, montre-moi l’app concernée et attends mon choix.

Le gain dépend du contenu des caches et peut être nul. Une fois terminé, désactivez le débogage dans les menus de la TV. Si ADB est indisponible, passez à la méthode manuelle ci-dessous. L’installation du launcher demande un APK officiel, disponible à partir de la première publication.

## Installer le launcher

1. Dans le dépôt GitHub officiel de SeeClap Launcher, ouvrez **Releases**, puis la dernière version et **Assets**. Avant la première publication, aucun APK n’y est disponible. La version Google Play est prévue.
2. Téléchargez `seeclap-launcher-<code>.apk`, où `<code>` est le numéro de la version. Utilisez le navigateur de la TV, ou copiez l’APK depuis un ordinateur sur une clé USB et ouvrez-le sur la TV avec un gestionnaire de fichiers.
3. Si Android bloque l’installation, ouvrez les réglages proposés. Sur Android 8 ou plus récent, autorisez l’app qui ouvre le fichier à **installer des applications inconnues**. Sur Android 7, activez temporairement **Sources inconnues** dans les réglages de sécurité.
4. Revenez au fichier, confirmez **Installer**, puis désactivez la même autorisation. Ouvrez **SeeClap Launcher** dans les applications de la TV.

## Premier lancement

1. Choisissez **Commencer**. Parcourez les thèmes avec Gauche et Droite, puis appuyez sur OK. **Automatique** suit la TV. Vous pouvez aussi choisir **Clair** ou **Foncé**.
2. Si SeeClap manque, choisissez **Installer** ou **Plus tard**. Le launcher ouvre la boutique disponible (Google Play ou Amazon sur Fire TV). L’édition APK peut aussi télécharger SeeClap depuis son site officiel et vérifier sa signature. Confirmez alors l’installation dans Android. L’édition Play passe par la boutique pour les installations et les mises à jour.
3. Choisissez jusqu’à cinq autres favoris par défaut, ou quatre si vous utilisez les grandes tuiles. **Continuer** enregistre la sélection.
4. Pour le bouton Accueil, choisissez SeeClap Launcher si Android vous propose un accueil par défaut. Si l’assistant propose **Ouvrir Accessibilité**, lisez l’explication du service. Choisissez **Oui** pour continuer, puis activez **SeeClap Launcher – bouton Accueil** dans Android. **Annuler** ferme la demande et **Plus tard** permet de passer cette étape.
5. Quand ce service est actif, pressez la touche de télécommande souhaitée. Un appui court ouvrira SeeClap. L’appui long peut ouvrir l’app d’origine si elle est reconnue, installée et que l’option est activée dans **Réglages › Télécommande**.
6. Lancez le nettoyage si vous le souhaitez, ou choisissez **Continuer**, puis **Aller à l'accueil**. Pour revoir vos choix, ouvrez **Réglages › Recommencer l'assistant**.

## Télécommande et applications

- Gauche et Droite parcourent les favoris. OK ouvre l’app. Haut rejoint les programmes ou les commandes de lecture disponibles, sinon le menu du haut. Bas rejoint la grille des applications.
- Maintenez OK ou appuyez sur Menu sur une autre app pour la mettre en favori, la déplacer parmi les favoris ou la masquer. Si Android demande la lecture des programmes TV, autorisez-la pour afficher les cartes partagées par l’app sélectionnée.

## Libérer de l’espace

Dans **Réglages › Nettoyer et accélérer**, **Nettoyage rapide** classe vos apps par taille de fichier. Sélectionnez-en une pour demander sa désinstallation, puis confirmez dans Android.

**Nettoyage complet** utilise le débogage local de la TV. Examinez les options avant de lancer le nettoyage. Vous pouvez notamment vider les caches et désactiver certaines apps préinstallées. **Apps désactivées** permet de réactiver les apps désactivées par le launcher. Effacer un cache est définitif.

## Retrouver l’accueil d’origine

Si vous avez activé le service d’accessibilité, choisissez **Réglages › Bouton Accueil › Mettre en pause**, puis appuyez sur Accueil. **Ouvrir l'accueil d'origine** est aussi proposé quand un autre accueil est celui par défaut. Si SeeClap Launcher est l’accueil par défaut, sélectionnez l’accueil du fabricant dans les réglages Android des applications par défaut. Les menus varient selon la TV.

## Vie privée et conditions d’utilisation

Vos réglages sont enregistrés sur la TV et Android peut les inclure dans sa sauvegarde. Les programmes sont lus sur la TV. Leurs images peuvent être téléchargées en HTTPS et mises en cache. L’édition APK contacte aussi `downloads.seeapps.studio` pour les installations et les mises à jour. Le launcher est sans publicité ni mesure d’audience. Le service d’accessibilité repère les touches et l’ouverture de l’accueil d’origine sans lire le contenu de l’écran.

La licence autorise l’usage personnel et non commercial du logiciel non modifié. Vous pouvez partager le lien officiel du projet. La redistribution du logiciel exige l’accord écrit préalable de See Apps Studio. SeeClap reste en première position. Sa tuile ne peut être ni déplacée ni masquée, et SeeClap ne peut pas être retirée ou désinstallée depuis le launcher. Une fois SeeClap installée, gardez-la installée tant que vous utilisez le launcher.

Pour obtenir de l’aide, écrivez à [support@seeapps.studio](mailto:support@seeapps.studio).
