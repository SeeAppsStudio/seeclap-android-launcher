# 用 Claude Code 或 Codex 为电视腾出空间

让电脑和你自己的 Android TV 或 Google TV 连入同一个私人网络。在电脑上打开可以使用终端的 Claude Code 或 Codex，从 [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) 安装 `adb`。如果还想安装 SeeClap Launcher，需要准备 [Android SDK Build-Tools](https://developer.android.com/tools/apksigner) 中的 `apksigner`。

在电视上打开“设置 › 系统 › 关于”。如果没有看到开发者选项，连续按七次“版本号（Build）”。打开无线调试，或者使用电视支持的数据线进行 USB 调试。用遥控器在电视上确认连接。

在第一行填入电视型号和回复语言，然后把下面整段文字复制到助手的对话框。

```text
我的电视是［品牌和型号］。
请用［简体中文］回答。
帮我清理缓存。
安装启动器和停用应用是可选操作，每项都要先征得我的同意。

1. 使用无线调试时，请用电视显示的配对地址和端口运行 adb pair。
配对码由我在电脑上输入。
如需手动连接，再用电视显示的连接端口运行 adb connect。
使用 USB 时，请等我在电视上授权这台电脑。
从设备列表中确认我的电视，每条命令里的 SERIAL 都要替换成它的设备标识。
无法确认时先问我。

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. 在电脑上建立以日期命名的文件夹，记录电视型号、Android 版本、当前用户 ID 和当前主屏幕应用。
和我确认这是要操作的那台电视。

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. 先测量 /data 的可用空间，请 Android 清理缓存，再测量一次。
告诉我实际差值，即使差值为零。
缓存以后还会重新生成。
不要使用 pm clear，因为它也会删除应用数据。

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. 只有我提出要求时，才从下方官方发布页面的附件中下载启动器 APK。
如果没有 APK，就只清理缓存。
在电脑上确认包名为 studio.seeapps.home，并验证下载文件的签名。
将签名证书的 SHA-256 指纹与下方数值核对：

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

只有在签名验证成功且证书指纹一致时，才运行 adb -s SERIAL install -r APP.apk。
否则跳过安装。

5. 如果我想停用应用，请参考 Gathin23 的 CleanYourTV 项目中的机型清单。
先核对完整型号，再向我说明每个软件包的用途。
型号或用途不明确的都跳过。

https://github.com/Gathin23/cleanyourtv

记录每个已启用软件包所在的用户及原始状态。
逐个征得我同意后，通过 adb -s SERIAL shell 运行 pm disable-user --user ID PACKAGE。
保存填好用户 ID 和包名的恢复命令：对于原先明确启用的应用用 pm enable --user ID PACKAGE，对于原先使用默认状态的应用用 pm default-state --user ID PACKAGE。
修改前先确认电视支持相应的恢复命令。

不要停用或移除 SeeClap、SeeClap Launcher、厂商主屏幕，或网络、设置、键盘、Cast、更新等必要服务。
始终让 SeeClap 排在第一个位置。
保护下面这些软件包：

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. 由我自行选择主屏幕。
如需主页键服务，也由我在无障碍设置中亲自开启。
不要通过命令直接改写无障碍设置，也不要绕过系统检查。
把变更记录交给我，说明如何恢复每个应用、关闭这项服务并切换回厂商主屏幕。
断开 ADB 连接，并指导我在电视上取消与电脑的配对、撤销调试授权和关闭调试。

主页键服务
studio.seeapps.home/studio.seeapps.home.HomeGuardService
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [GitHub 发布版本](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
