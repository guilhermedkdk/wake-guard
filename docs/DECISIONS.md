# Decisões

Log append-only, com a entrada mais nova no topo. Cada entrada registra o que uma sessão futura faria
errado sem ela.

### 2026-11-10 - Disparo: quem recebe o horário é o `ringing`, e o desafio ainda é provisório

**Armadilha:** o `AlarmManager` precisa acordar algo que inicie o disparo, mas `alarm` não pode
depender de `ringing`, que já depende dele. Um ensaio pode ser de um alarme nunca salvo, então não dá
para o serviço buscá-lo pelo id. E `startForegroundService` exige `startForeground` em poucos segundos,
mesmo quando o início é ignorado porque já há um disparo.
**Regra:** o `alarm` agenda um broadcast com a ação `AlarmTime.ACTION`, restrito ao próprio pacote, e o
`AlarmReceiver` do `ringing` o recebe. Disparo de verdade chega ao serviço pelo id e volta se o
processo morrer (`START_REDELIVER_INTENT`); ensaio chega em memória pelo `RingingController` e não
volta. A tela manda os eventos para `RingingController.service`, que só existe enquanto toca. O
`ChallengeScreen` é provisório, com os botões "Interagir" e "Concluir desafio", até cada tipo de desafio
ter a própria tela; a assinatura dele já é a final. O tempo máximo é uma constante de 2 minutos no
serviço até existir a aba Ajustes. O som padrão é `res/raw/alarm.wav`, gerado por script.
**Ainda não tratado:** sem a permissão de notificação, um disparo de verdade toca mas não abre a tela
sozinho; o pedido da permissão entra com o Início e os Ajustes. O volume do canal de alarme do aparelho
não é forçado. Na edição, o teclado numérico abre sozinho e cobre a tela.
**Conferir no emulador:** `adb shell pm grant <pacote> android.permission.POST_NOTIFICATIONS`, e
`adb shell dumpsys audio` mostra o player com `USAGE_ALARM` e o `mutedState`.
**Onde:** `ringing/`, `challenge/ChallengeScreen.kt`, `alarm/SystemAlarmScheduler.kt`.

### 2026-11-10 - Agendamento: o livro é um por processo e o "agora" não é um `Clock` guardado

**Armadilha:** os receivers acordam o app sem nenhuma tela e precisam do mesmo `AlarmBook` das telas,
mas o pacote `alarm` não pode depender do `app` para pegá-lo no `AppContainer`; e dois bancos Room
abertos no mesmo arquivo não avisam um ao outro das mudanças. Um `Clock.systemDefaultZone()` guardado
num campo congela o fuso do momento em que foi criado, e reagendar depois de uma troca de fuso
calcularia no fuso antigo. Dois `PendingIntent` que só diferem nos extras são o mesmo para o sistema.
**Regra:** `AlarmBook.of(context)` é o único jeito de obter o livro em produção e devolve sempre a
mesma instância. O livro recebe `now: () -> ZonedDateTime` e o lê a cada cálculo. Cada alarme tem no
máximo um agendamento, com o id do alarme como request code do `PendingIntent`; por isso agendar de novo
substitui e cancelar não precisa saber o horário. Um alarme único se desativa em `markRung`, quando o
horário chega, e não quando o disparo termina. As permissões são `USE_EXACT_ALARM` no Android 13 ou
superior, concedida na instalação, e `SCHEDULE_EXACT_ALARM` só até o Android 12.
**Conferir no emulador:** `adb shell cmd alarm set-time <ms>` e `set-timezone <fuso>` mudam o relógio
sem root, e `adb shell dumpsys alarm` mostra o "Next alarm clock". Depois de `adb reboot`, o receiver
de boot leva uns segundos a mais que o `sys.boot_completed` para rodar.
**Onde:** `alarm/AlarmBook.kt`, `alarm/SystemAlarmScheduler.kt`, `alarm/AlarmReceivers.kt`.

### 2026-11-10 - Alarmes: um banco por pacote e edição sem ViewModel

**Armadilha:** um banco Room único obrigaria uma classe central a conhecer as tabelas de todos os
pacotes. E um ViewModel por alarme, sem biblioteca de navegação, fica preso à activity: reabrir o mesmo
alarme mostraria o rascunho antigo.
**Regra:** cada pacote com dados tem o próprio banco, escondido atrás da sua interface; o de alarmes é
`alarms.db`, atrás do `AlarmBook`. Os desafios ficam numa tabela filha, na ordem, e são regravados
inteiros a cada salvamento. Tipo e dificuldade são gravados pelo nome do enum, então renomear um valor
de `ChallengeType` ou `Difficulty` pede migration. Os dias da semana são um inteiro de bits, com a
segunda no bit 0. O esquema exportado fica em `app/schemas` e é versionado. Na edição, cada campo é um
`rememberSaveable`, o que mantém o rascunho ao girar a tela e o descarta ao fechar.
**Onde:** `alarm/AlarmDatabase.kt`, `alarm/AlarmBook.kt`, `alarm/AlarmEditorScreen.kt`.

### 2026-11-10 - Armadilhas do esqueleto

**Armadilha:** com o JDK 25, todo teste do Robolectric falha com "Failed to interact with raw
FileDescriptor internals", porque ele usa `jdk.internal.access`, que o JDK não exporta mais. O
Robolectric 4.17 também não tem a imagem do Android 37, que é o `compileSdk`. E o Lint, com avisos como
erro, recusa `android:allowBackup` sozinho no manifesto.
**Regra:** as tasks de teste rodam com `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`. O
Robolectric simula o Android 36, fixado em `app/src/test/resources/robolectric.properties`; só suba
esse número junto com uma versão do Robolectric que traga a imagem nova. O manifesto aponta para um
`data_extraction_rules.xml` que exclui tudo de backup e de transferência entre aparelhos, porque nada
sai do aparelho. No AGP 9 o Kotlin é embutido: não existe o plugin `kotlin-android` no build, só o do
compilador do Compose.
**Onde:** `app/build.gradle.kts`, `app/src/test/resources/robolectric.properties`,
`app/src/main/res/xml/data_extraction_rules.xml`.

### 2026-11-10 - Fora do escopo da primeira versão

**Decisão:** ficaram de fora, por escolha do dono do projeto, e não por esquecimento:

- **Retomar o disparo depois de reiniciar.** Reiniciar encerra o disparo. Ver a entrada abaixo.
- **Soneca.**
- **Saída de emergência**, como um desafio alternativo quando a câmera falha.
- **Trava de edição na véspera.** O alarme pode ser editado, desativado ou excluído até o último
  minuto.
- **Desafio de dança.**
- **Desafio de apontar a câmera para um cômodo ou móvel.** Ideia para o futuro; o caminho mais
  confiável seria ler um QR code ou código de barras colado no lugar.
- **Conta, servidor, sincronização e publicação na Play Store.**

**Regra:** nenhum desses entra por iniciativa de uma sessão. Se um deles parecer necessário para fechar
uma brecha, pergunte antes.

### 2026-11-10 - Reiniciar encerra o disparo, e isso é de propósito

**Armadilha:** o motivo do projeto é o alarme não poder ser calado desligando o celular, então retomar
o disparo no boot parece a correção óbvia de uma brecha. Foi oferecido e recusado como exagero. As
proteções cobrem o menu de energia, que é o caminho usado na prática; o reinício forçado pelos botões
fica como a única fuga, aceita.
**Regra:** no boot, o app só reagenda os alarmes futuros. Não grava "disparo em curso" para retomar,
e um alarme cujo horário passou com o celular desligado não toca. Reagendar no boot não é opcional:
o `AlarmManager` esquece todos os alarmes quando o aparelho reinicia.
**Onde:** `alarm` (reagendamento), `ringing`.

### 2026-11-10 - O tempo máximo de disparo padrão é 2 minutos

**Armadilha:** dois minutos parece um erro num despertador para sono pesado sem soneca, e uma sessão
futura tenderia a "corrigir" para sem limite.
**Regra:** o padrão é 2 minutos, escolhido pelo dono do projeto, e é ajustável até sem limite. Não
mude o padrão sem pedir.
**Onde:** `settings`.

### 2026-11-10 - Kotlin nativo, não React Native

**Decisão:** Kotlin com Jetpack Compose, apesar da familiaridade do dono do projeto com React.
**Motivo:** o núcleo do app é API de sistema: alarme exato, serviço em primeiro plano, tela por cima do
bloqueio, serviço de acessibilidade e análise de câmera em tempo real. Em React Native ou Flutter
quase tudo isso seria módulo nativo em Kotlin de qualquer forma, com uma ponte a mais para manter.

### 2026-11-10 - Proteções por serviço de acessibilidade, só durante o disparo

**Decisão:** as proteções usam um serviço de acessibilidade, o mesmo caminho dos apps do gênero. Não
usam administrador do dispositivo nem modo quiosque.
**Regra:** o serviço não lê nem age em nada fora de um disparo de verdade, e não age em ensaio. Sem
ele, o alarme funciona sem proteção. App instalado por APK no Android 13 ou superior precisa que a
pessoa libere "configurações restritas" nas informações do app antes de conseguir ligar o serviço; a
tela que pede a permissão tem de explicar esse passo.
**Onde:** `guard` (fase 2).

### 2026-11-10 - Um disparo por vez, e ele pertence ao serviço

**Decisão:** só existe um disparo ao mesmo tempo, e um alarme que chega durante outro é ignorado. O
estado do disparo vive no `RingingService`; a tela só o desenha.
**Motivo:** o Android destrói e recria a tela de disparo ao girar, ao apagar a tela e quando a pessoa
sai. Se o progresso dos desafios vivesse na tela, sair e voltar zeraria o desafio ou, pior, pararia o
som.
**Onde:** `ringing`.
