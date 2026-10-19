# Meu Haxball (Android)

Cliente Android que abre o **Haxball oficial** (https://www.haxball.com/play) num WebView e
adiciona controles de toque. Salas online, multiplayer, nicks, chat, times e placar são os do
jogo original; o app não altera o código do jogo nem cria uma simulação.

## O que o app faz

- **Joystick de 8 direções** (setas) e **botão X** (chutar). Os dois funcionam ao mesmo tempo.
- **Botão Chat** (envia Enter, que abre o chat no Haxball) e **botão de menu** (☰).
- **Personalizar controles**: arraste cada controle, ajuste tamanho do joystick, tamanho dos
  botões e opacidade. Tudo é salvo no aparelho. "Restaurar padrão" volta ao original.
- **Tela**: sempre na horizontal (sem recriar a tela ao girar), tela cheia, respeita notch e
  cantos arredondados, sem zoom, rolagem da página travada (opcional).
- **Chat**: ao abrir o teclado, os controles somem para não cobrir o campo/chat.
- **Som**: som do jogo liberado; os botões de volume do aparelho e o controle de volume do menu
  mexem no volume de mídia.
- **Conexão**: se a página não carregar, aparece uma tela de erro e o app recarrega sozinho
  quando a internet volta. "Recarregar / reconectar" no menu volta para a sala atual.
- **Voltar** abre/fecha o menu (não sai da sala sem querer).
- **Modo desktop** (opcional): faz o site tratar o app como navegador de computador.
- Teclado ou controle físico ligados ao celular funcionam normalmente no jogo.

## Limitações conhecidas

- Os botões enviam as teclas padrão do Haxball (setas e X). Se você mudar as teclas nas
  configurações do próprio Haxball, os botões não acompanham.
- O volume do menu é o volume de **mídia do aparelho**; o Haxball não oferece API para o app
  controlar o volume do jogo (ele tem seu próprio liga/desliga de som nas configurações dele).
- Ao sair do app (Home), o navegador interno é pausado para economizar bateria. Ao voltar,
  talvez seja preciso usar "Recarregar / reconectar" para entrar de novo na sala.
- Criar sala e carregar mapas próprios dependem do site oficial no WebView e não foram testados.

## Gerar o APK sem Android Studio (GitHub Actions)

1. Crie um repositório no github.com e envie todos os arquivos desta pasta, incluindo
   `.github/workflows/build.yml`.
2. Aba **Actions** > "Gerar APK" roda sozinho (ou "Run workflow").
3. Quando terminar (3-5 min), baixe o artefato **MeuHaxball-apk** no final da página da execução.
4. Descompacte e instale o `app-debug.apk` no celular.

## Estrutura do código

| Arquivo | Função |
| --- | --- |
| `MainActivity.java` | WebView, tela cheia/notch/teclado, menu, painel de edição, conexão |
| `ControlsLayer.java` | Camada de controles sobre o jogo e modo de arrastar |
| `JoystickView.java` | Joystick virtual de 8 direções |
| `ActionButton.java` | Botões circulares (chute, chat, menu) |
| `KeyInjector.java` | Envia teclas ao WebView e solta tudo ao pausar |
| `ControlSettings.java` | Preferências salvas (layout, opacidade, opções) |
| `UiKit.java` | Estilo visual dos menus |
