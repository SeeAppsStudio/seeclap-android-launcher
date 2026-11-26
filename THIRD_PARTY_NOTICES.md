# Third-party notices

## CleanYourTV (MIT)

https://github.com/Gathin23/cleanyourtv

The Home-button guard and the remote-button handling of SeeClap Launcher
(`tv/src/main/java/studio/seeapps/home/Guard.kt`) are ported from CleanYourTV's
`HomeKeyService` and `RemoteKeys`: the boot probe, the short-press redirect after
the system handled Home, opening the Home screen as an app while Android keeps
its stock launcher, the "hold" long-press action and the conservative clean-up
list. The clean-up's ADB signature padding and public-key format follow the
public adb protocol as also implemented by AdbLib.

```
MIT License

Copyright (c) 2026 Gathin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Roboto (Apache License 2.0)

The words "SeeClap Launcher" in `home_banner.png` are set in Roboto, © Google,
Apache License 2.0. No font file is shipped: the app uses the TV's own system font.

## Build tools

The Gradle Wrapper scripts and JAR are distributed under the [Apache License 2.0](docs/licences/Apache-2.0.txt).
The Windows script also carries a notice from Meta Platforms, Inc. and affiliates.
Its MIT licence grants the permissions and conditions reproduced above under CleanYourTV,
with the copyright notice “Copyright (c) Meta Platforms, Inc. and affiliates.”

## SeeClap marks

The SeeClap banner (`seeclap_banner.png`) and the app icon (`ic_launcher.png`) are
the property of See Apps Studio and are not covered by the LICENSE file.
