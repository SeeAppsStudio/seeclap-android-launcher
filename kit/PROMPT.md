# Free up space on your TV with Claude Code or Codex

Use a computer on the same private network as your own Android TV or Google TV. Open Claude Code or Codex with terminal access and install `adb` from [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). To install the launcher as well, get `apksigner` from [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

On the TV, go to Settings › System › About. If Developer options are hidden, press the build number seven times. Turn on Wireless debugging, or use USB debugging with a supported data cable. Keep the remote handy to approve the connection.

Fill in the model and language on the first line, then paste the whole block into your agent’s chat.

```text
My TV is a [brand and model]. Reply in [English]. Help me clear its caches. Ask separately
before installing the launcher or disabling apps.

1. For Wi-Fi pairing, use the pairing address and port shown on my TV with adb pair. I’ll
enter the code locally. If needed, use the connection port shown on the TV with adb connect.
For USB, wait for me to approve this computer on the TV. Find my TV in the list and use its
identifier for SERIAL in each command. Ask if you cannot identify it.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Record the model, Android version, active user ID and current Home app in a dated folder
on my computer. Check with me that this is the right TV.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Measure the available space on /data, ask Android to trim its caches, then measure again.
Tell me the observed difference, even if it is zero. Caches can grow again. Never use
pm clear, which also erases app data.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Only if I ask, get the launcher APK from the assets of the official release below. If
there is no APK, continue with cache cleanup alone. Check the downloaded package is
studio.seeapps.home and verify the signature on my computer. Compare the signer certificate’s
SHA-256 with this value:

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Use adb -s SERIAL install -r APP.apk only after signature verification succeeds and the
certificate matches. Otherwise skip installation.

5. If I want to disable apps, consult the model lists from CleanYourTV by Gathin23. Check for
an exact model match and explain each package. Skip anything whose identity or purpose is
uncertain.

https://github.com/Gathin23/cleanyourtv

Record the profile and the original state of each enabled package. With my consent for each
app, run pm disable-user --user ID PACKAGE through adb -s SERIAL shell. Save its full restore
command: pm enable --user ID PACKAGE for an explicitly enabled app or
pm default-state --user ID PACKAGE for the default state. Check the restore command is
available before making the change.

Never disable or remove SeeClap, its launcher, the manufacturer’s Home app or essential
services such as networking, Settings, the keyboard, Cast and updates. Keep SeeClap as the
first tile. Protect these packages:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Let me choose the Home app in Android settings. If the choice is unavailable, show me how to open SeeClap Launcher from the TV’s apps. Report any installation block. Give me the change log and undo steps, then disconnect ADB and show me how to revoke debugging access.
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [GitHub releases](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
