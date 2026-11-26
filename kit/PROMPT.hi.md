# Claude Code या Codex की मदद से टीवी में जगह खाली करें

अपने Android TV या Google TV और कंप्यूटर को एक ही निजी नेटवर्क से जोड़ें। कंप्यूटर पर टर्मिनल चलाने की सुविधा के साथ Claude Code या Codex खोलें। [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) से `adb` इंस्टॉल करें। अगर आप लॉन्चर भी इंस्टॉल करना चाहते हैं, तो [Android SDK Build-Tools](https://developer.android.com/tools/apksigner) का `apksigner` भी चाहिए।

टीवी पर सेटिंग्स › सिस्टम › जानकारी खोलें। अगर डेवलपर विकल्प नहीं दिखते, तो बिल्ड नंबर को सात बार दबाएँ। वायरलेस डीबगिंग चालू करें या उपयुक्त डेटा केबल के साथ USB डीबगिंग इस्तेमाल करें। कनेक्शन की पुष्टि के लिए रिमोट पास रखें।

पहली पंक्ति में मॉडल और अपनी भाषा भरें। फिर नीचे का पूरा पाठ सहायक की चैट में पेस्ट करें।

```text
मेरा टीवी [ब्रांड और मॉडल] है। मुझे [हिन्दी] में जवाब दो। टीवी का कैश साफ़ करने में मेरी मदद
करो। लॉन्चर इंस्टॉल करने या ऐप बंद करने से पहले हर काम के लिए अलग से मेरी अनुमति लो।

1. वाई-फ़ाई के लिए टीवी पर दिख रहे पेयरिंग पते और पोर्ट के साथ adb pair चलाओ। कोड मैं अपने
कंप्यूटर पर दर्ज करूँगा। ज़रूरत हो तो उसके बाद टीवी पर दिख रहे कनेक्शन पोर्ट के साथ
adb connect चलाओ। USB के लिए पहले मुझे टीवी पर कंप्यूटर को अनुमति देने दो। सूची में मेरा टीवी
पहचानो और हर कमांड में SERIAL की जगह उसी की आईडी डालो। पहचान साफ़ न हो तो मुझसे पूछो।

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. कंप्यूटर पर तारीख के नाम से एक फ़ोल्डर बनाओ। उसमें टीवी का मॉडल, Android संस्करण, सक्रिय
उपयोगकर्ता की आईडी और मौजूदा होम ऐप दर्ज करो। मुझसे पुष्टि कर लो कि यही मेरा टीवी है।

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. /data पर खाली जगह मापो। Android से कैश साफ़ करवाओ और फिर से उसी तरह खाली जगह मापो। जो अंतर
दिखे वह बताओ, भले ही वह शून्य हो। कैश बाद में फिर बन सकता है। pm clear कभी मत चलाना क्योंकि
यह ऐप का डेटा भी मिटा देता है।

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. मेरे कहने पर ही नीचे दी गई आधिकारिक रिलीज़ के साथ जुड़ी फ़ाइलों से लॉन्चर का APK डाउनलोड
करो। APK उपलब्ध न हो तो केवल कैश की सफ़ाई जारी रखो। कंप्यूटर पर जाँचो कि पैकेज का नाम
studio.seeapps.home है और APK का हस्ताक्षर सही है। हस्ताक्षर करने वाले के प्रमाणपत्र का
SHA-256 नीचे दिए मान से मिलाओ।

https://github.com/SeeAppsStudio/seeclap-android-launcher/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

हस्ताक्षर की जाँच सफल हो और प्रमाणपत्र का मान मिले, तभी adb -s SERIAL install -r APP.apk
चलाओ। ऐसा न हो तो इंस्टॉल मत करो।

5. अगर मैं कुछ ऐप बंद करना चाहूँ, तो Gathin23 के CleanYourTV प्रोजेक्ट की मॉडलवार सूची देखो।
पूरा मॉडल मिलाओ और हर पैकेज का काम मुझे समझाओ। जिस पैकेज या मॉडल को लेकर संदेह हो, उसे छोड़
दो।

https://github.com/Gathin23/cleanyourtv

हर सक्रिय पैकेज के लिए उपयोगकर्ता की आईडी और उसकी शुरुआती स्थिति दर्ज करो। हर ऐप के लिए मेरी
सहमति मिलने पर ही adb -s SERIAL shell के ज़रिए pm disable-user --user ID PACKAGE चलाओ। बहाली
की पूरी कमांड लिखकर रखो: पहले से अलग से चालू किए गए ऐप के लिए pm enable --user ID PACKAGE और
डिफ़ॉल्ट स्थिति के लिए pm default-state --user ID PACKAGE। बदलाव से पहले जाँचो कि टीवी पर
बहाली की कमांड उपलब्ध है।

SeeClap, उसके लॉन्चर, निर्माता की होम स्क्रीन और नेटवर्क, सेटिंग्स, कीबोर्ड, Cast या अपडेट
जैसी ज़रूरी सेवाओं को कभी बंद मत करो और न ही हटाओ। SeeClap को पहली टाइल पर रखो। इन पैकेजों को
सुरक्षित रखो:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. होम स्क्रीन मुझे खुद चुनने दो। अगर मुझे होम बटन की सेवा चाहिए, तो उसे मैं खुद सुलभता की
सेटिंग्स में चालू करूँगा। कमांड के ज़रिए ये सेटिंग्स मत बदलो और सिस्टम की जाँच को दरकिनार मत
करो। बदलावों का रिकॉर्ड दो और बताओ कि हर ऐप कैसे बहाल करूँ, सेवा कैसे बंद करूँ और निर्माता की
होम स्क्रीन कैसे वापस चुनूँ। ADB डिस्कनेक्ट करो। टीवी पर कंप्यूटर की पेयरिंग हटाने, डीबगिंग
की अनुमति वापस लेने और डीबगिंग बंद करने का तरीका बताओ।

होम बटन की सेवा
studio.seeapps.home/studio.seeapps.home.HomeGuardService
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [GitHub पर रिलीज़](https://github.com/SeeAppsStudio/seeclap-android-launcher/releases)
