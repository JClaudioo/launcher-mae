# Launcher da mãe

Tela inicial (launcher) Android feita para o celular da minha mãe, um **Xiaomi 2406APNFAG com Android 16**.
Derivado do [s24-Launcher](https://github.com/JClaudioo/s24-Launcher), com o mesmo visual claro,
mas com menos coisa na tela e tudo em letras maiores.

## O que tem

**Tela inicial**
- Relógio grande e clima (cidade, temperatura, mínima e máxima do dia).
- Player do que está tocando (YouTube Music e qualquer outro app de música).
- Baterias do celular, do relógio e dos fones Bluetooth — relógio e fones só aparecem conectados.
- Cartão do **ar-condicionado**: Ligar, a temperatura e os botões − / +. Tocando no cartão abre o controle inteiro.
- Atalho do Google ao lado da pesquisa (com voz e Google Lens).
- Dock com 5 apps: Telefone, WhatsApp, Câmera, Galeria e Mensagens.

**Menu de apps** (deslizar para cima, ou para o lado)
- Todos os apps em ordem alfabética, 4 por linha, com o **ícone original de cada app** e o nome em duas linhas.
- Pesquisa por nome.
- Toque longo num app: informações ou desinstalar.
- "Mais opções": acesso à música, Bluetooth, papel de parede, launcher padrão e o último erro.

**Widget da tela de bloqueio**: baterias e clima.

## Controle do ar-condicionado

O celular tem emissor **infravermelho** (`android.hardware.consumerir`), então o launcher manda os comandos
direto para o ar, sem internet, sem rede e sem app da Samsung. É só apontar o celular para o aparelho.

- Protocolo: Samsung A/C, 38 kHz, 14 bytes (ajustes) ou 21 bytes (ligar, desligar e timer).
  Os quadros foram conferidos contra os da biblioteca [IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266).
- Controla: ligar/desligar, temperatura de 16 a 30 °C, modo (Frio, Ventilar, Seco), ventilação
  (Baixa, Média, Alta, Auto), aletas e desligar sozinho em 1 h, 2 h ou 4 h.
- O ar não responde ao celular, então o launcher mostra **o último comando que mandou**. Se alguém usar o
  controle de verdade, os dois podem ficar diferentes até o próximo toque no app.

## Compilar

O APK sai pelo GitHub Actions a cada push na `main` (aba **Actions** › última execução › artefato
`Launcher-Mae-v1.0.N`). Sem os secrets de assinatura o APK sai assinado em modo de teste, o que já instala.

Para assinar sempre com a mesma chave (e assim atualizar por cima sem desinstalar), crie dois secrets
em **Settings › Secrets and variables › Actions**:

- `KEYSTORE_BASE64` — o arquivo `.jks` convertido com `base64 -w0 launcher.jks`
- `KEYSTORE_PASSWORD` — a senha da chave

## Estrutura

```
app/src/main/java/br/joao/launcher/
  MainActivity.kt        junta tudo: páginas, permissões e as ações dos botões
  LauncherApp.kt         baterias e ar vivos enquanto o launcher existir
  data/ArSamsung.kt      o controle do ar: monta os bytes e manda pelo infravermelho
  data/Apps.kt           lista de apps instalados com o ícone original
  data/Battery.kt        bateria do celular + Bluetooth
  data/Weather.kt        clima (Open-Meteo)
  data/SystemActions.kt  pesquisa, Lens, ajustes do sistema, papel de parede
  media/Media.kt         o que está tocando
  ui/HomeScreen.kt       tela inicial
  ui/ArScreen.kt         cartão e controle inteiro do ar
  ui/DrawerScreen.kt     menu de apps
  ui/Glyphs.kt           ícones desenhados à mão
  widget/LockWidget.kt   widget da tela de bloqueio
```

O papel de parede (`res/drawable-nodpi/wallpaper_claro.jpg` e `wallpaper.jpg`) é um fundo neutro
provisório: é só trocar os dois arquivos por uma foto que ela goste, no mesmo nome.
