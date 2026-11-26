# Guia de instalação do SeeClap Launcher

Numa TV ou box com Android TV 7 ou posterior, o SeeClap Launcher mostra as tuas apps favoritas e os programas que elas partilham. O primeiro mosaico abre o SeeClap ou permite instalá-lo.

## Com um computador e um assistente de IA

Podes verificar a tua própria TV e limpar as caches antes mesmo da publicação do primeiro APK. O kit adapta o método do [CleanYourTV](https://github.com/Gathin23/cleanyourtv). Também ajuda a instalar o launcher quando houver um APK oficial disponível.

1. Liga a TV e o computador à mesma rede privada. No computador, prepara o Claude Code ou o Codex com acesso ao terminal e as Android SDK Platform-Tools, que incluem o `adb`.
2. Na TV, abre **Definições › Sistema › Acerca de** e prime sete vezes o número de compilação para mostrar as opções de programador. Ativa temporariamente a depuração sem fios ou de rede, se estiver disponível. Segue as instruções de emparelhamento e confirma a ligação do computador na TV. Os nomes dos menus dependem do modelo.
3. Copia as instruções do [kit de IA completo em português](https://github.com/SeeAppsStudio/seeclap-launcher-android-tv/blob/main/kit/PROMPT.pt.md) para o assistente e acrescenta o teu pedido. Podes copiar este exemplo.

> Verifica o modelo da minha TV e mede o espaço livre antes e depois de limpar as caches. Preserva os dados das apps e as minhas sessões iniciadas. Não uses `pm clear`, que apaga os dados de uma app. Antes de desativar ou desinstalar algo, mostra-me a app em causa e espera que eu escolha.

O espaço recuperado depende das caches e pode ser nulo. Quando acabares, desativa a depuração nos menus da TV. Se o ADB não estiver disponível, segue o método manual abaixo. Para instalar o launcher é preciso o APK oficial, disponível a partir da primeira publicação.

## Instalar o launcher

1. No repositório oficial do SeeClap Launcher no GitHub, abre **Releases**, escolhe a versão mais recente e expande **Assets**. Até à primeira publicação, não há nenhum APK disponível. Está prevista uma edição para o Google Play.
2. Transfere `seeclap-launcher-<code>.apk`, em que `<code>` é o número da versão. Usa o navegador da TV ou copia o APK de um computador para uma pen USB e abre-o na TV com um gestor de ficheiros.
3. Se o Android bloquear a instalação, abre as definições que ele sugere. No Android 8 ou posterior, permite que a app que abre o ficheiro possa **instalar aplicações desconhecidas**. No Android 7, ativa temporariamente **Fontes desconhecidas** nas definições de segurança.
4. Volta ao ficheiro, confirma **Instalar** e desativa a mesma permissão. Abre o **SeeClap Launcher** a partir das aplicações da TV.

## Primeiro arranque

1. Seleciona **Começar**. Usa as setas para a esquerda e para a direita para veres os temas e prime OK para escolher. **Automático** segue o tema da TV. Também podes escolher **Claro** ou **Escuro**.
2. Se o SeeClap não estiver instalado, escolhe **Instalar** ou **Mais tarde**. O launcher abre a loja disponível (Google Play ou Amazon na Fire TV). A edição APK também pode transferir o SeeClap do site oficial e verificar a assinatura. Confirma a instalação no Android. A edição do Google Play usa a loja para instalações e atualizações.
3. Na disposição inicial podes escolher até cinco favoritos além do SeeClap, ou quatro se usares os mosaicos grandes. Seleciona **Continuar** para guardar a seleção.
4. Para o botão Início, escolhe o SeeClap Launcher se o Android pedir uma app de início predefinida. Se o assistente mostrar **Abrir Acessibilidade**, lê a explicação do serviço. Escolhe **Sim** para continuar e ativa **SeeClap Launcher – botão Início** no Android. **Cancelar** fecha o pedido e **Mais tarde** permite saltar este passo.
5. Com o serviço ativo, prime o botão do comando que queres associar. Um toque curto abrirá o SeeClap. Um toque longo pode abrir a app original se estiver instalada, for reconhecida e a opção estiver ativa em **Definições › Comando**.
6. Executa a limpeza opcional ou escolhe **Continuar** e depois **Ir para o início**. Podes rever as escolhas em **Definições › Repetir o assistente**.

## Usar o comando e as aplicações

- Usa Esquerda e Direita para percorrer os favoritos e OK para abrir uma app. Para cima acede aos programas ou controlos de reprodução disponíveis, ou ao menu superior se não existirem. Para baixo abre a grelha de aplicações.
- Noutra app, mantém OK premido ou prime Menu para a adicionar aos favoritos, mudar a sua posição nos favoritos ou ocultá-la. Se o Android pedir acesso aos programas de TV, concede a permissão para veres os cartões partilhados pela app selecionada.

## Libertar espaço

Em **Definições › Limpar e acelerar**, a opção **Limpeza rápida** ordena as tuas apps por tamanho do ficheiro. Seleciona uma para pedir a desinstalação e confirma no Android.

A opção **Limpeza completa** usa a depuração local da TV. Revê as opções antes de começar. Podes limpar as caches e desativar certas apps pré-instaladas. Em **Apps desativadas** podes reativar as apps que o launcher desativou. Os dados de cache apagados não podem ser recuperados.

## Voltar ao ecrã inicial original

Se ativaste o serviço de acessibilidade, escolhe **Definições › Botão Início › Pausar** e prime Início. A opção **Abrir o início original** aparece quando outra app é o início predefinido. Se o SeeClap Launcher for o início predefinido, escolhe o do fabricante nas definições de aplicações predefinidas do Android. Os menus variam consoante a TV.

## Privacidade e condições de utilização

As tuas definições ficam na TV e o Android pode incluí-las na cópia de segurança. O launcher lê os programas na TV. Pode transferir imagens por HTTPS e guardá-las na cache. A edição APK também contacta `downloads.seeapps.studio` para instalações e atualizações. O launcher não tem publicidade nem recolhe estatísticas de utilização. O serviço de acessibilidade deteta as teclas premidas e a abertura do ecrã inicial original sem ler o conteúdo do ecrã.

A licença permite a utilização pessoal e não comercial do software sem modificações. Podes partilhar a ligação oficial do projeto. A redistribuição do software exige autorização prévia por escrito da See Apps Studio. O SeeClap fica sempre em primeiro lugar. O seu mosaico não pode ser movido nem ocultado, e não é possível remover ou desinstalar o SeeClap a partir do launcher. Depois de instalares o SeeClap, mantém a app instalada enquanto usares o launcher.

Se precisares de ajuda, escreve para [support@seeapps.studio](mailto:support@seeapps.studio).
