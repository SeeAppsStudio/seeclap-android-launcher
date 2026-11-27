# SeeClap Android Launcher

[Français](docs/guide/fr.md) · [English](docs/guide/en.md) · [Deutsch](docs/guide/de.md) · [Español](docs/guide/es.md) · [Italiano](docs/guide/it.md) · [Nederlands](docs/guide/nl.md) · [Português](docs/guide/pt.md) · [Русский](docs/guide/ru.md) · [Svenska](docs/guide/sv.md) · [Türkçe](docs/guide/tr.md) · [简体中文](docs/guide/zh.md) · [हिन्दी](docs/guide/hi.md)

The [TV edition](tv/) brings your favorite apps together on Android TV or Google TV (Android 7 or later). SeeClap stays in the first tile.

![SeeClap Launcher home screen with a program carousel and six favorite apps](docs/images/launcher-preview.webp)

*Home screen illustration. The program and all apps except SeeClap are fictional.*

## 1. Install the complete edition with an assistant · Recommended

On a computer using the same Wi-Fi as your TV, open Claude Code or Codex with terminal access. Paste this message.

```text
Follow https://raw.githubusercontent.com/SeeAppsStudio/seeclap-android-launcher/main/skills/seeclap-tv/SKILL.md
to install the complete SeeClap Launcher on my TV and offer a deeper clean-up.
```

The assistant verifies the signed APK and installs it from your computer after you approve the connection and installation. The complete edition can handle the Home button on supported TVs and assign a remote shortcut. You enable the accessibility service on the TV. You can also ask the assistant for a deeper clean-up and review each suggested app change.

## 2. Install the light edition with Downloader or USB

The light edition keeps the app shelf and program carousel. You can make it your Home app where Android offers that setting. On your TV, open **Downloader by AFTVnews**, enter **`6812755`**, then select **Go**. If the code does not open the file, enter [downloads.seeapps.studio/launcher/tv.apk](https://downloads.seeapps.studio/launcher/tv.apk). Select **Install** and then **Open**. If Android asked for permission to install apps, turn it off again afterward. [Illustrated setup guide in 12 languages](https://seeclap.com/en/android-launcher/tv/)

**With a USB drive**

[Download the light edition for Android TV (signed APK)](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases/download/v1.0.1/seeclap-launcher-4.apk). Copy it to a USB drive, open it on the TV with a file manager, then select **Install**. If Android asks for installation permission, turn it off again afterward. Open **SeeClap Launcher** and follow the on-screen setup. [Step-by-step guide](docs/guide/en.md)

Both editions use the same app and signing key and have separate update channels. Android can refuse a switch to an older build.

[Detailed help](docs/reference/en.md) · [Personal-use licence](LICENSE) · [Build from source](docs/build.md)

The computer-assisted setup is inspired by CleanYourTV. Parts of the remote and Home-button handling are adapted from that project. [CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Credits and licences](THIRD_PARTY_NOTICES.md)
