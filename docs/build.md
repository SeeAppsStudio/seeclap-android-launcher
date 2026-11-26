# Build SeeClap Launcher for Android TV

For a personal build from the unchanged source, use JDK 17 and Android SDK platform 36 with Build-Tools 36.0.0. From the repository root, run the Gradle wrapper. On Windows use `gradlew.bat`.

```sh
./gradlew :app:assembleSiteDebug :app:assemblePlayDebug
./gradlew :app:testSiteDebugUnitTest :app:testPlayDebugUnitTest
python3 tools/strings/build.py
```

Debug APKs are written to `app/build/outputs/apk/` and use a local development key. Some SeeClap editions require a matching signature for direct program sharing. Official releases are signed separately by See Apps Studio.
