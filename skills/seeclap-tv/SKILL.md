---
name: seeclap-tv
description: Guide the owner of an Android TV or Google TV through connecting their own computer, clearing app caches and optionally installing the official SeeClap Launcher or restoring app choices.
---

# Set up my TV with SeeClap

Speak the owner's language. Do the computer work yourself. Give one remote-control instruction at a time when the owner needs to act on the TV. A request to clean means temporary caches. Offer launcher installation separately. Review preinstalled apps only when requested.

## Connect the right TV

1. Use a local terminal on the owner's computer. Look for Google's `adb`; if missing, help set up [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) for this OS. A web chat without terminal or local network access can only give instructions.
2. On the same private Wi-Fi, guide the owner to **Settings › System (or Device preferences) › About**. Press OK on the Android TV build seven times. Open Developer options. Use the wireless pairing address and port **shown by the TV** with `adb pair HOST:PAIR_PORT`, then the connection port with `adb connect HOST:CONNECT_PORT` if necessary. Have the owner enter pairing codes locally. On older TVs use network debugging at the address shown, or a supported USB data connection. Let the owner approve their computer on-screen. If debugging is unavailable, offer the manual APK guide.
3. Use `adb devices -l` to find an authorized connection. Get the model with `adb -s SERIAL shell getprop ro.product.model`; confirm which TV to use with the owner. Always specify its exact serial. Leave unrelated devices alone. If the device disconnects, reidentify it before continuing.

## Clean and show the result

Use the bundled `scripts/cache.py` with Python 3 and Platform-Tools. If you received this skill as a URL, get the [official SeeClap repository](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv) on the computer or download [cache.py](https://raw.githubusercontent.com/SeeAppsStudio/seeclap-launcher-android-tv/main/skills/seeclap-tv/scripts/cache.py) from it. Read the script locally before running it. Pass the full path to adb with `--adb` if needed.

```sh
python3 scripts/cache.py --serial SERIAL
python3 scripts/cache.py --serial SERIAL --clean
```

Run these commands from the `skills/seeclap-tv` folder; for a standalone download, use the saved script’s actual path. The first command identifies the TV and measures storage. Run the second only for a requested cleanup, after confirming the TV. On Windows use the available Python 3 launcher. Report the measured change in free space, including zero or a decrease. Caches can grow back. If Android refuses or the final measurement fails, report exactly that. Never use `pm clear`, delete app files, or uninstall an app to reclaim more space.

## Install the launcher if the owner chooses it

For Android TV or Google TV running Android 7 or later, use the signed `seeclap-launcher-<code>.apk` asset from the [latest official release](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/releases). If there is no published APK, explain that installation is pending and finish the other requested work. Never substitute a debug build or another project's launcher.

On the computer, use Android SDK Build-Tools to run `aapt2 dump badging FILE.apk` and `apksigner verify --print-certs FILE.apk`. Require package `studio.seeapps.home`, successful signature verification, and the signer certificate SHA-256 `6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e`. If the release lists a file checksum, verify that separately. Install with `adb -s SERIAL install -r FILE.apk` and check `adb -s SERIAL shell pm path studio.seeapps.home`. On a signature conflict preserve the installed app and its data. Guide the owner to launch SeeClap Launcher, choose their favorites and optionally choose Home or enable the Home button in Android's settings. Keep SeeClap first. The owner grants accessibility on the TV.

## Optional app review and undo

If asked to review apps, record the active user, stock Home, installed packages and prior enabled states in a dated local log. Ask which apps the owner uses. Consult the [CleanYourTV device examples](https://github.com/Gathin23/cleanyourtv/tree/main/examples) as a reference for an exact model. Explain each identified app by name and what will change. Skip unknown packages. Protect SeeClap (`com.seeappsstudio.seeclap` and `com.codingbyjerez.seeclap`), its launcher, stock Home, settings, input, network, Cast, updates and other essential services.

Before an approved change, save the exact restore command for the confirmed serial, active user and package. Only change an enabled app when its original state and restoration command are known. Use `pm disable-user --user ID PACKAGE`; restore with `pm enable --user ID PACKAGE` for an explicitly enabled app or `pm default-state --user ID PACKAGE` for the default state. Use the selected device for every shell call. Verify the resulting state. For a restore request, apply the recorded inverse for that TV and user and verify it. Cache contents cannot be restored.

Finish with storage before/after, installation status and exactly which apps changed. Give the owner the log location and how to return to their original Home if relevant. Disconnect only the selected network connection and guide them to turn off debugging and forget the computer on the TV.
