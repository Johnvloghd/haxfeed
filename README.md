# Meu Haxball (Android)

App Android mínimo que abre o Haxball oficial (https://www.haxball.com/play) num WebView
e adiciona controles de toque:

- Joystick virtual (esquerda) -> setas do teclado
- Botão X (direita) -> chute

O app **não altera o código do jogo**; ele só envia teclas para a página.

## Gerar o APK sem Android Studio (GitHub Actions)

1. Crie uma conta no github.com e um repositório novo (pode ser privado).
2. Envie todos os arquivos desta pasta para o repositório
   (botão "Add file" > "Upload files"; inclua a pasta `.github`).
3. Abra a aba **Actions**. O workflow "Gerar APK" roda sozinho a cada envio
   (ou clique em "Run workflow").
4. Quando terminar (uns 3-5 min), abra a execução e baixe o artefato **MeuHaxball-apk**.
5. Descompacte o zip, passe o `app-debug.apk` para o celular e instale
   (permita "instalar apps de fontes desconhecidas" quando o Android pedir).

## Compilar no seu PC (opcional)

Precisa só do JDK 17, das *command-line tools* do Android SDK (ANDROID_HOME configurado)
e do Gradle 8.7:

    gradle assembleDebug

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

## Próximos passos

- Ícone do app (res/mipmap) e nome.
- Tela de perfis (nick salvo), tela cheia, botão para recarregar a sala.
- Ajustar tamanho e posição dos controles; mais botões (ex.: chat).
