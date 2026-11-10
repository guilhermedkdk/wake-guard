# Decisões

Log append-only, com a entrada mais nova no topo. Cada entrada registra o que uma sessão futura faria
errado sem ela.

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
