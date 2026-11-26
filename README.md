# SeeClap Launcher for Android TV

A Home screen for Android TV and Google TV running Android 7 or later. SeeClap
always occupies the first favorite tile. Press OK to open SeeClap or start its
installation. Choose your other favorites, browse programs shared by compatible
apps and open Settings to change the theme or set up the Home button.

## Set up your TV with an AI assistant

On a computer connected to the same private network as **your own TV**, open
Claude Code or Codex with terminal access. Choose your language below. The
**guide** shows what to do on the TV. The **AI instructions** are the complete
text to copy into your assistant. You can also follow the manual APK installation
in the same guide.

The assistant first identifies your TV and measures its free space. You choose
whether to clear temporary caches or review individual preinstalled apps. It
records the choices and measures again so you can see the result. Cache cleanup
can be done before the first launcher APK is published. The remote-button handling
and parts of the cleanup are adapted from [CleanYourTV](https://github.com/Gathin23/cleanyourtv).
Third-party credits and licence terms are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

| Language | Guide | AI instructions to copy |
| --- | --- | --- |
| Français | [Guide d’installation](docs/guide/fr.md) | [Instructions à copier](kit/PROMPT.fr.md) |
| English | [Installation guide](docs/guide/en.md) | [Copy the instructions](kit/PROMPT.md) |
| Deutsch | [Installationsanleitung](docs/guide/de.md) | [Anweisungen kopieren](kit/PROMPT.de.md) |
| Español | [Guía de instalación](docs/guide/es.md) | [Copiar las instrucciones](kit/PROMPT.es.md) |
| Italiano | [Guida all’installazione](docs/guide/it.md) | [Copia le istruzioni](kit/PROMPT.it.md) |
| Nederlands | [Installatiegids](docs/guide/nl.md) | [Instructies kopiëren](kit/PROMPT.nl.md) |
| Português | [Guia de instalação](docs/guide/pt.md) | [Copiar as instruções](kit/PROMPT.pt.md) |
| Русский | [Руководство по установке](docs/guide/ru.md) | [Скопировать инструкции](kit/PROMPT.ru.md) |
| Svenska | [Installationsguide](docs/guide/sv.md) | [Kopiera instruktionerna](kit/PROMPT.sv.md) |
| Türkçe | [Kurulum kılavuzu](docs/guide/tr.md) | [Talimatları kopyala](kit/PROMPT.tr.md) |
| 简体中文 | [安装指南](docs/guide/zh.md) | [复制操作说明](kit/PROMPT.zh.md) |
| हिन्दी | [इंस्टॉलेशन गाइड](docs/guide/hi.md) | [निर्देश कॉपी करें](kit/PROMPT.hi.md) |

The official APK will appear in [Releases](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/releases), under **Assets**, as
`seeclap-launcher-<versionCode>.apk`. If there is no release with an APK, the
download is not available yet. A Google Play listing is planned. The site edition
can install SeeClap from the official download service. The Play edition uses
the store. Android may ask you to confirm installation or grant permissions.

## Languages and privacy

The launcher uses the TV's language. Its interface is available in all twelve
languages listed above, with English as the fallback. Apps supply their own names,
program titles and descriptions. Android and the store supply their own dialogs.

The launcher includes no advertising or analytics SDK. It reads programs shared
by compatible apps on the TV and may request artwork from their servers.
The site edition also contacts the official download service to check for updates.

## Personal use

You may study the source, build it unchanged and run it on your own devices for
personal use. Keep the SeeClap tile visible in first place and retain SeeClap
while using the launcher once it is installed. Changing, rebranding,
redistributing or deploying the launcher for anyone else requires prior written
permission from See Apps Studio. You can share the official project link.
[Licence](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md)

## Build from source

Use JDK 17 and Android SDK 36. The Gradle wrapper is included.

```sh
./gradlew :app:assembleSiteDebug :app:assemblePlayDebug
./gradlew :app:testSiteDebugUnitTest :app:testPlayDebugUnitTest
python3 tools/strings/build.py
```

Personal debug builds use a local development key. Official release signing
requires the publisher's private configuration. Some SeeClap editions require
a matching signature for direct program sharing.
