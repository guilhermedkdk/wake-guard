# Arquitetura

Como o código se organiza e por quê. O comportamento está em `docs/SPEC.md` e o vocabulário em
`docs/GLOSSARY.md`.

## Stack

| Parte | Escolha |
| ----- | ------- |
| Linguagem e UI | Kotlin, Jetpack Compose, Material 3 |
| Build | Gradle com Kotlin DSL, versões em `gradle/libs.versions.toml` |
| Dados | Room, com um banco por pacote (`alarms.db`, depois o de despertares), e DataStore para os ajustes |
| Câmera (fase 3) | CameraX e ML Kit (pose e rosto), com os modelos embutidos no APK |
| Testes | JUnit para as regras, Robolectric para o que precisa do Android, testes de Compose para telas |
| Qualidade | Spotless com ktlint, Android Lint |

Um módulo Gradle só, `app`. `minSdk` 31 (Android 12). Sem injeção de dependência por framework: um
`AppContainer` criado na `Application` monta os poucos objetos de vida longa.

## Onde cada parte roda

| Peça | Componente Android | Vive |
| ---- | ------------------ | ---- |
| Abas e edição | `MainActivity`, uma activity com navegação do Compose | Enquanto a pessoa usa o app |
| Agendamento | `AlarmManager.setAlarmClock`, que acorda o app no horário | No sistema, mesmo com o app morto |
| Entrada do disparo | `AlarmReceiver`, no pacote `ringing`, que recebe o broadcast `AlarmTime` agendado pelo `alarm` | Um instante: acerta o alarme com `AlarmBook.markRung` e inicia o serviço |
| Disparo | `RingingService`, serviço em primeiro plano com som, vibração e relógio do tempo máximo | Do horário até o desfecho |
| Tela de disparo | `RingingActivity`, por cima do bloqueio | Enquanto o disparo existe |
| Reagendar | `RescheduleReceiver`, no pacote `alarm`, no boot e na mudança de hora ou fuso | Um instante |
| Proteções (fase 2) | `GuardService`, serviço de acessibilidade | Sempre ligado, age só durante o disparo |

O disparo pertence ao serviço, não à tela. A tela pode morrer e voltar; o som, o relógio e o progresso
dos desafios continuam no serviço, e a tela só desenha o estado que ele publica.

## Pacotes

Pacote base `io.github.guilhermedkdk.wakeguard`, organizado por funcionalidade. Cada pacote esconde o
máximo: o que não está na coluna do meio é `internal` ou `private`.

| Pacote | Interface (o que outros pacotes usam) | O que fica escondido |
| ------ | ------------------------------------- | -------------------- |
| `alarm` | `Alarm`, `Alarm.nextRinging(now)`, `AlarmBook` (listar, salvar, excluir, ativar, `markRung`, `rescheduleAll`), `AlarmTime` (o broadcast do horário), `AlarmsScreen`, `AlarmEditorScreen` | O cálculo do próximo disparo, as entidades e o DAO do Room, a conversão entre banco e domínio, o `AlarmScheduler` e o `RescheduleReceiver`: salvar um alarme já o agenda, ninguém de fora agenda nada. `AlarmBook.of(context)` devolve o único livro do processo, que telas e receivers compartilham. |
| `challenge` | `ChallengeSpec` (tipo, dificuldade, quantidade), `ChallengeSpecDialog` e `challengeSummary` para a edição do alarme, `Challenge` (estado de um desafio em andamento), `ChallengeScreen(spec, onCompleted, onInteraction)` | Um subpacote por tipo, cada um com o gerador de rodadas, a conferência da resposta e o seu composable. Sensores e câmera ficam aqui dentro. |
| `ringing` | `RingingController` (`start(alarmId)`, `rehearse(alarm)`, `state`), `RingingState` | O `Ringing` (a máquina do disparo), o `RingingService`, o `AlarmReceiver`, o `AlarmPlayer` (som e vibração), o volume crescente, o silêncio durante o desafio, o tempo máximo, a notificação e a `RingingActivity`. |
| `guard` (fase 2) | `ProtectionStatus`, para o painel do Início | O `GuardService` e o que ele reconhece em cada tela do sistema. Lê `RingingController.state` para saber se age. |
| `history` (fase 4) | `WakeLog.record(...)`, `WakeLog.recent()` | A tabela de despertares e as contas de média e sequência. |
| `settings` | `Settings` (tempo máximo, volume do disparo, chaves das proteções) | O DataStore e os valores padrão. |
| `home` | A aba Início | A contagem regressiva e o painel. |
| `theme` | `WakeGuardTheme`, usado pelas duas activities | As cores. |
| `app` | `WakeGuardApplication`, `AppContainer`, `MainActivity` e a navegação das abas | A montagem dos objetos. |

Dependências: `alarm` usa `challenge`, pelo `ChallengeSpec` e pela edição de um desafio; `ringing` usa
`alarm`, `challenge`, `settings` e `history`; `guard` usa `ringing` e `settings`; `home` usa `alarm`,
`guard` e `history`; `app` usa todos. `theme`, `challenge`, `settings` e `history` não dependem de nenhum outro
pacote de funcionalidade. Nenhum ciclo.

A navegação é um estado só em `WakeGuardApp`: a aba atual e o alarme aberto na edição. Não há
biblioteca de navegação nem ViewModel; cada tela recebe o que usa por parâmetro.

## Regras em Kotlin puro

As regras ficam em classes sem nenhum import do Android, testadas com JUnit simples e um relógio
injetado:

- `Alarm.nextRinging(now)`: horário, dias da semana e fuso, incluindo a virada do horário de verão.
- `Ringing`: recebe eventos (rodada concluída, interação, tempo passou) e devolve o estado. Decide a
  ordem dos desafios, quando o som fica mudo, quando expira e o desfecho.
- Cada desafio: um gerador que recebe um `Random` e produz rodadas, e uma função que confere a
  resposta. O de sacudir recebe leituras de aceleração; os de câmera recebem ângulos e probabilidades
  já extraídos, nunca um frame.

O Android fica nas bordas: serviço, receivers, sensores, câmera e composables chamam essas classes e
não decidem nada. O `RingingService` pergunta ao `Ringing`, a cada 250 ms, o volume e se está mudo, e
repassa ao `AlarmPlayer`; por isso som e vibração não têm seam próprio e são conferidos no aparelho.

## Seams

| Seam | Produção | Teste |
| ---- | -------- | ----- |
| Agora (`() -> ZonedDateTime`) | `ZonedDateTime::now`, lido a cada uso | Instante fixo ou que avança à mão |
| `Random` | `Random.Default` | Semente fixa |
| `AlarmScheduler` | `SystemAlarmScheduler`, sobre o `AlarmManager` | Mapa em memória dos agendamentos |
| Banco | Room em arquivo | Room em memória, pelo Robolectric |
| Sensor de sacudida | Acelerômetro | Sequência de leituras escrita no teste |
| Detector de pose e rosto (fase 3) | ML Kit sobre a CameraX | Sequência de ângulos e probabilidades |

## Testes

- **Regras:** JUnit puro em `app/src/test`, sem Android. É onde fica a maioria dos testes.
- **Android sem aparelho:** Robolectric em `app/src/test`, para Room, receivers, o agendamento e as
  telas de Compose.
- **Aparelho ou emulador:** `app/src/androidTest` só para o que o Robolectric não reproduz, como a
  tela por cima do bloqueio e o serviço de acessibilidade. Não roda no `check`.
- **Conferência manual no S23:** som, vibração, câmera e proteções. O roteiro fica no PR da fase.

## Permissões

| Permissão | Para quê | Fase |
| --------- | -------- | ---- |
| `USE_EXACT_ALARM` (Android 13+) e `SCHEDULE_EXACT_ALARM` (Android 12) | Alarme no minuto exato | 1 |
| `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT` | Notificação do disparo e tela por cima do bloqueio | 1 |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Serviço do disparo | 1 |
| `RECEIVE_BOOT_COMPLETED` | Reagendar depois de reiniciar | 1 |
| `VIBRATE`, `WAKE_LOCK` | Vibração e tela acesa | 1 |
| Isenção de otimização de bateria | O sistema não adiar nem matar o disparo | 1 |
| Serviço de acessibilidade | Proteções | 2 |
| `CAMERA` | Desafios de câmera | 3 |

O app não declara `INTERNET`.
