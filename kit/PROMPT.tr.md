# Claude Code veya Codex ile TV’de yer açma

Bilgisayarınızı kendi Android TV veya Google TV’nizle aynı özel ağa bağlayın. Claude Code veya Codex’i terminal erişimiyle açın. `adb` aracını [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) paketinden yükleyin. Başlatıcıyı da yüklemek isterseniz [Android SDK Build-Tools](https://developer.android.com/tools/apksigner) paketindeki `apksigner` gerekir.

TV’de Ayarlar › Sistem › Hakkında bölümünü açın. Geliştirici seçenekleri gizliyse derleme numarasına yedi kez basın. Kablosuz hata ayıklamayı açın veya uyumlu bir veri kablosuyla USB hata ayıklamayı kullanın. Bağlantıyı onaylamak için kumandayı yanınızda tutun.

İlk satıra modelinizi ve yanıt dilini yazın. Aşağıdaki metnin tamamını asistanın sohbetine yapıştırın.

```text
TV’m [marka ve model]. Bana [Türkçe] yanıt ver. Önbelleği temizlememe yardım et. Başlatıcıyı
yüklemeden veya uygulamaları devre dışı bırakmadan önce bunlar için ayrı ayrı onayımı al.

1. Wi-Fi için TV’de görünen eşleme adresini ve eşleme bağlantı noktasını adb pair ile kullan.
Kodu bilgisayarımda ben gireceğim. Gerekirse adb connect için TV’deki bağlantı noktasını
kullan. USB ile bağlanıyorsak bilgisayarı TV’de onaylamamı bekle. Listeden TV’mi bul ve her
komutta SERIAL yerine onun kimliğini yaz. Emin değilsen bana sor.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Modeli, Android sürümünü, etkin kullanıcı kimliğini ve mevcut ana ekran uygulamasını
bilgisayarımda tarihli bir klasöre kaydet. Doğru TV olduğunu benimle teyit et.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. /data üzerindeki boş alanı ölç. Android’den önbelleği temizlemesini iste ve aynı ölçümü
tekrarla. Fark sıfır olsa bile sonucu bildir. Önbellek yeniden büyüyebilir. Uygulama
verilerini de sildiği için pm clear komutunu asla kullanma.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Yalnızca istersem aşağıdaki resmî sürümün ekli dosyalarından başlatıcının APK’sını indir.
APK yoksa yalnızca önbellek temizliğiyle devam et. Bilgisayarımda paket adının
studio.seeapps.home olduğunu ve imzanın geçerli olduğunu doğrula. İmzalayanın sertifikasına
ait SHA-256 değerini şununla karşılaştır:

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Yalnızca doğrulama başarılıysa ve sertifika eşleşiyorsa adb -s SERIAL install -r APP.apk ile
yükle. Aksi durumda yüklemeyi atla.

5. Uygulama devre dışı bırakmak istersem Gathin23’ün CleanYourTV projesindeki model
listelerine bak. Modelin tam olarak eşleştiğini doğrula ve her paketin işlevini açıkla. Emin
olmadığın paketlere dokunma.

https://github.com/Gathin23/cleanyourtv

Etkin olan her paketin profilini ve ilk durumunu kaydet. Her uygulama için onayımı aldıktan
sonra adb -s SERIAL shell üzerinden pm disable-user --user ID PAKET kullan. Tam geri alma
komutunu kaydet: özellikle etkinleştirilmiş bir uygulama için pm enable --user ID PAKET,
varsayılan durum için pm default-state --user ID PAKET. Değişiklikten önce geri alma
komutunun kullanılabildiğini doğrula.

SeeClap’i, başlatıcısını, üreticinin ana ekranını ve ağ, ayarlar, klavye, Cast, güncellemeler
gibi temel hizmetleri asla devre dışı bırakma veya kaldırma. SeeClap ilk kutucuk olarak
kalsın. Şu paketleri koru:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Android ayarlarında ana ekran uygulamasını benim seçmeme izin ver. Bu seçenek yoksa TV’nin uygulama listesinden SeeClap Launcher’ı nasıl açacağımı göster. Kurulum engelini bildir, değişiklik günlüğünü ve geri alma adımlarını ver, ardından ADB bağlantısını kes.
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [GitHub sürümleri](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
