# Libertar espaço na TV com o Claude Code ou o Codex

Liga o computador à mesma rede privada da tua Android TV ou Google TV. Abre o Claude Code ou o Codex com acesso ao terminal e instala o `adb` através das [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools). Para instalares também o launcher, precisas do `apksigner` das [Android SDK Build-Tools](https://developer.android.com/tools/apksigner).

Na TV, abre Definições › Sistema › Acerca de. Se as opções de programador estiverem ocultas, prime sete vezes o número da compilação. Ativa a depuração sem fios ou usa a depuração USB com um cabo de dados compatível. Mantém o comando da TV à mão para autorizares a ligação.

Preenche o modelo e o idioma na primeira linha e cola todo o bloco na conversa com o assistente.

```text
A minha TV é uma [marca e modelo]. Responde em [português]. Ajuda-me a limpar a cache. Pede
autorização em separado para instalar o launcher ou desativar aplicações.

1. Por Wi-Fi, usa o endereço e a porta de emparelhamento que aparecem na TV com adb pair.
Introduzo o código no computador. Se necessário, usa depois a porta de ligação com
adb connect. Por USB, espera que eu autorize o computador na TV. Identifica a minha TV na
lista e substitui SERIAL pelo identificador em cada comando. Se tiveres dúvidas, pergunta-me.

adb pair IP:PAIR_PORT
adb connect IP:CONNECT_PORT
adb devices -l

2. Guarda numa pasta com a data, no meu computador, o modelo, a versão do Android, o ID do
utilizador ativo e a aplicação atual do ecrã inicial. Confirma comigo que é a TV certa.

adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

3. Mede o espaço disponível em /data, pede ao Android que liberte a cache e volta a medir.
Diz-me a diferença observada, mesmo que seja zero. A cache pode voltar a crescer. Nunca uses
pm clear, que também apaga os dados da aplicação.

adb -s SERIAL shell df -k /data
adb -s SERIAL shell pm trim-caches 999999999999
adb -s SERIAL shell df -k /data

4. Só se eu pedir, transfere o APK do launcher a partir dos ficheiros anexos à versão oficial
abaixo. Se não houver APK, continua apenas com a limpeza. No computador, confirma o nome do
pacote studio.seeapps.home e verifica a assinatura. Compara o SHA-256 do certificado do
signatário com este valor:

https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/releases

apksigner verify --print-certs APP.apk

6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e

Usa adb -s SERIAL install -r APP.apk apenas se a verificação for bem-sucedida e o certificado
coincidir. Caso contrário, salta a instalação.

5. Se eu quiser desativar aplicações, consulta as listas por modelo do CleanYourTV, de
Gathin23. Confirma que correspondem ao modelo exato e explica a função de cada pacote. Em
caso de dúvida, deixa-o como está.

https://github.com/Gathin23/cleanyourtv

Regista o perfil e o estado inicial de cada pacote ativo. Com a minha aprovação para cada
aplicação, usa pm disable-user --user ID PACOTE através de adb -s SERIAL shell. Guarda o
comando de restauro completo: pm enable --user ID PACOTE para uma aplicação ativada
explicitamente ou pm default-state --user ID PACOTE para o estado predefinido. Verifica antes
se o comando está disponível.

Nunca desatives nem removas o SeeClap, o seu launcher, o ecrã inicial do fabricante ou
serviços essenciais como a rede, as definições, o teclado, o Cast e as atualizações. Mantém o
SeeClap na primeira posição. Protege estes pacotes:

com.seeappsstudio.seeclap / com.codingbyjerez.seeclap / studio.seeapps.home

6. Deixa-me escolher o ecrã inicial e ativar pessoalmente o serviço do botão Início em
Acessibilidade, se quiser. Não alteres essas definições por comando nem contornes os
controlos do sistema. Entrega-me o registo e os passos para restaurar as aplicações,
desativar o serviço e voltar ao ecrã inicial original. Desliga o ADB e mostra-me como
desemparelhar o computador, revogar as autorizações de depuração e desligar a depuração na
TV.

Serviço do botão Início
studio.seeapps.home/studio.seeapps.home.HomeGuardService
```

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) · [Versões no GitHub](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/releases)
