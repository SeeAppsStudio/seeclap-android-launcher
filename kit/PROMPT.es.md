# Liberar espacio en el televisor con Claude Code o Codex

Conecta el ordenador a la misma red privada que tu propio Android TV o Google TV. Abre Claude Code o Codex con acceso al terminal e instala `adb` desde [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Si también quieres instalar el launcher, necesitarás `apksigner` de [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

En el televisor, ve a Ajustes › Sistema › Información. Si no ves las opciones de desarrollador, pulsa siete veces el número de compilación. Activa la depuración inalámbrica o la depuración USB con un cable de datos compatible. Ten el mando a mano para aceptar la conexión.

Indica el modelo y el idioma en la primera línea. Copia el bloque entero en el chat del asistente.

```text
Mi televisor es un [marca y modelo]. Respóndeme en [español]. Ayúdame a limpiar la caché.
Pídeme permiso por separado para instalar el launcher o desactivar aplicaciones.

1. Para conectar por Wi-Fi, usa con adb pair la dirección y el puerto de vinculación que
muestra mi televisor. Introduciré el código en mi ordenador. Si hace falta, usa después el
puerto de conexión con adb connect. Por USB, espera a que autorice el ordenador en el
televisor. Identifica mi TV en la lista y sustituye SERIAL por su identificador en cada
orden. Si hay dudas, pregúntame.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Guarda en una carpeta con fecha el modelo, la versión de Android, el identificador del
usuario activo y la aplicación de inicio actual. Confirma conmigo que es el televisor
correcto.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Mide el espacio disponible en /data, pide a Android que libere las cachés y vuelve a
medir. Dime la diferencia observada, aunque sea cero. Las cachés pueden volver a crecer.
Nunca uses pm clear, que también borra los datos de la aplicación.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Solo si te lo pido, descarga la APK del launcher de los archivos adjuntos a la versión
oficial que aparece abajo. Si no hay APK, continúa solo con la limpieza. Comprueba en mi
ordenador el nombre del paquete, studio.seeapps.home, y verifica la firma. Compara el SHA-256
del certificado del firmante con este valor:

https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Usa adb -s SERIAL install -r APP.apk únicamente si la firma es válida y el certificado
coincide. En caso contrario, omite la instalación.

5. Si quiero desactivar aplicaciones, consulta las listas por modelo de CleanYourTV, de
Gathin23. Comprueba que el modelo coincide y explícame para qué sirve cada paquete. Omite los
casos dudosos.

https://github.com/Gathin23/cleanyourtv

Anota el usuario y el estado original de cada paquete activo. Con mi aprobación para cada
aplicación, usa pm disable-user --user ID PAQUETE mediante adb -s SERIAL shell. Guarda la
orden de restauración completa: pm enable --user ID PAQUETE si estaba activada expresamente o
pm default-state --user ID PAQUETE si tenía el estado predeterminado. Comprueba antes que la
orden esté disponible.

Nunca desactives ni elimines SeeClap, su launcher, la pantalla de inicio del fabricante ni
los servicios esenciales de red, ajustes, teclado, Cast y actualizaciones. Mantén SeeClap en
la primera posición. Protege estos paquetes:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Déjame elegir la pantalla de inicio y activar personalmente, si quiero, el servicio del
botón Inicio en Accesibilidad. No cambies esos ajustes mediante órdenes ni eludas controles.
Entrégame el registro, las instrucciones para restaurar cada aplicación, desactivar el
servicio y recuperar el inicio del fabricante. Desconecta ADB y muéstrame cómo olvidar el
ordenador, revocar las autorizaciones de depuración y desactivar la depuración en el
televisor.

Servicio del botón Inicio
studio.seeapps.home/studio.seeapps.home.HomeGuardService
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Versiones en GitHub](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/releases)
