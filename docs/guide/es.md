# Guía de instalación de SeeClap Launcher

En un televisor o dispositivo con Android TV 7 o posterior, SeeClap Launcher muestra tus apps favoritas y los programas que comparten. La primera tarjeta abre SeeClap o te ofrece instalarla.

## Con un ordenador y un asistente de IA

Puedes revisar tu propio televisor y limpiar sus cachés incluso antes de que se publique el primer APK. El kit adapta el método de [CleanYourTV](https://github.com/Gathin23/cleanyourtv). También sirve para instalar el launcher cuando haya un APK oficial disponible.

1. Conecta el televisor y el ordenador a la misma red privada. En el ordenador, prepara Claude Code o Codex con acceso al terminal y las Android SDK Platform-Tools, que incluyen `adb`.
2. En el televisor, abre **Ajustes › Sistema › Información** y pulsa siete veces el número de compilación para mostrar las opciones de desarrollador. Activa temporalmente la depuración inalámbrica o de red si está disponible. Sigue las instrucciones de emparejamiento y confirma la conexión de tu ordenador en el televisor. Los nombres de los menús dependen del modelo.
3. Copia las instrucciones del [kit completo de IA en español](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/blob/main/kit/PROMPT.es.md) en tu asistente y añade tu petición. Puedes copiar este ejemplo.

> Comprueba el modelo de mi televisor y mide el espacio libre antes y después de limpiar las cachés. Conserva mis datos y mis sesiones abiertas. No uses `pm clear`, que borra los datos de una app. Antes de desactivar o desinstalar algo, muéstrame la app concreta y espera a que yo elija.

El espacio que se recupera depende de las cachés y puede ser cero. Al terminar, desactiva la depuración en los menús del televisor. Si ADB no está disponible, sigue el método manual que aparece a continuación. Para instalar el launcher necesitas el APK oficial, disponible a partir de la primera publicación.

## Instalar el launcher

1. En el repositorio oficial de SeeClap Launcher en GitHub, abre **Releases**, entra en la última versión y despliega **Assets**. Hasta que se publique la primera versión, no habrá ningún APK. Está prevista una edición para Google Play.
2. Descarga `seeclap-launcher-<code>.apk`, donde `<code>` es el número de versión. Usa el navegador del televisor o copia el APK desde un ordenador a una memoria USB y ábrelo en el televisor con un gestor de archivos.
3. Si Android bloquea la instalación, abre los ajustes que te ofrece. En Android 8 o posterior, permite **instalar aplicaciones desconocidas** a la app que abre el archivo. En Android 7, activa temporalmente **Orígenes desconocidos** en los ajustes de seguridad.
4. Vuelve al archivo, confirma **Instalar** y desactiva ese permiso. Abre **SeeClap Launcher** desde las aplicaciones del televisor.

## Primer inicio

1. Selecciona **Empezar**. Usa Izquierda y Derecha para ver los temas y pulsa OK para elegir. **Automático** sigue el tema del televisor. También puedes elegir **Claro** u **Oscuro**.
2. Si falta SeeClap, elige **Instalar** o **Más tarde**. El launcher abre la tienda disponible (Google Play o Amazon en Fire TV). La edición APK también puede descargar SeeClap desde su web oficial y verificar su firma. Confirma la instalación en Android. La edición de Play usa la tienda para instalar y actualizar.
3. Puedes elegir hasta cinco favoritos más con la disposición inicial, o cuatro con las tarjetas grandes. Pulsa **Continuar** para guardarlos.
4. Para el botón Inicio, selecciona SeeClap Launcher si Android te pide una app de inicio predeterminada. Si el asistente ofrece **Abrir Accesibilidad**, lee la explicación del servicio. Elige **Sí** para continuar y activa **SeeClap Launcher – botón Inicio** en Android. **Cancelar** cierra la solicitud y **Más tarde** permite saltar este paso.
5. Con el servicio activo, pulsa el botón del mando que quieras asignar. Una pulsación breve abrirá SeeClap. Una pulsación larga puede abrir la app original si está instalada, el launcher la reconoce y la opción está activa en **Ajustes › Mando**.
6. Ejecuta la limpieza opcional o elige **Continuar** y después **Ir al inicio**. Puedes revisar tus elecciones en **Ajustes › Repetir el asistente**.

## Usar el mando y las aplicaciones

- Pulsa Izquierda o Derecha para recorrer tus favoritos y OK para abrir uno. Arriba lleva a los programas o controles de reproducción disponibles, o al menú superior si no los hay. Abajo lleva a la cuadrícula de apps.
- En otra app, mantén pulsado OK o pulsa Menú para añadirla a favoritos, cambiarla de sitio entre los favoritos u ocultarla. Si Android te pide permiso para leer los programas de televisión, concédelo para ver las tarjetas que comparte la app seleccionada.

## Liberar espacio

En **Ajustes › Limpiar y acelerar**, **Limpieza rápida** ordena tus apps por tamaño de archivo. Elige una para solicitar su desinstalación y confirma en Android.

**Limpieza completa** usa la depuración local del televisor. Revisa las opciones antes de empezar. Puedes vaciar las cachés y desactivar ciertas apps preinstaladas. En **Apps desactivadas** puedes volver a activar las que desactivó el launcher. Los datos de caché borrados no se pueden recuperar.

## Volver a la pantalla de inicio original

Si activaste el servicio de accesibilidad, elige **Ajustes › Botón Inicio › Pausar** y pulsa Inicio. También verás **Abrir el inicio original** si hay otra app de inicio predeterminada. Si SeeClap Launcher es la predeterminada, selecciona la del fabricante en los ajustes de aplicaciones predeterminadas de Android. Los menús varían según el televisor.

## Privacidad y condiciones de uso

Tus ajustes se guardan en el televisor y Android puede incluirlos en su copia de seguridad. El launcher lee los programas en el televisor. Puede descargar las imágenes por HTTPS y guardarlas en la caché. La edición APK también se conecta a `downloads.seeapps.studio` para instalar y actualizar. El launcher no incluye publicidad ni analítica de uso. El servicio de accesibilidad detecta las pulsaciones y la apertura de la pantalla de inicio original sin leer el contenido de la pantalla.

La licencia permite el uso personal y no comercial del software sin modificar. Puedes compartir el enlace oficial del proyecto. Para redistribuir el software se requiere autorización previa y por escrito de See Apps Studio. SeeClap permanece en primer lugar. Su tarjeta no se puede mover ni ocultar, y desde el launcher no es posible quitar ni desinstalar SeeClap. Una vez instalada SeeClap, mantenla instalada mientras uses el launcher.

Si necesitas ayuda, escribe a [support@seeapps.studio](mailto:support@seeapps.studio).
