# wake-guard

Despertador para Android com desafios, de uso pessoal: o alarme só para quando a pessoa cumpre os
desafios, e enquanto ele toca o celular não deixa desligar pelo menu de energia. Kotlin + Jetpack
Compose, um módulo Gradle (`app`), Android 12 ou superior, validado num Galaxy S23.

## Limites que nenhuma mudança quebra

- O disparo pertence ao `RingingService`. A tela de disparo só desenha o estado que ele publica e
  nunca guarda progresso de desafio, som ou relógio.
- As regras ficam em Kotlin puro, sem import do Android: `Alarm.nextRinging`, `Ringing` e o gerador e
  a conferência de cada desafio. Serviço, receivers, sensores, câmera e composables só chamam.
- Só o pacote `alarm` fala com o `AlarmManager`. Salvar, ativar ou excluir um alarme pelo `AlarmBook`
  já agenda ou cancela; ninguém de fora agenda.
- Proteção só age durante um disparo de verdade. Nunca em ensaio, nunca fora de disparo.
- Reiniciar encerra o disparo e ele não é retomado. No boot, só os alarmes futuros são reagendados.
- Nada sai do aparelho: sem `INTERNET` no manifesto, sem gravar imagem da câmera.
- Schema do Room só muda com versão nova e migration. Uma migration que já rodou nunca é editada.

Os motivos estão em `docs/DECISIONS.md`, e o que ficou de fora de propósito também.

## Pronto quer dizer

- `.\gradlew check` verde na raiz: formatação (Spotless com ktlint), Android Lint e testes.
  `.\gradlew spotlessApply` corrige a formatação.
- Mudança de tela ou de disparo: conferir também no emulador (`.\gradlew installDebug`), e no S23 o
  que depende de aparelho de verdade: som, vibração, câmera e proteções.
- O Gradle roda com o JDK 25 do `JAVA_HOME`. O SDK do Android vem do `local.properties`, que não é
  versionado.

## Código

- Por funcionalidade em `io.github.guilhermedkdk.wakeguard.<pacote>`. Tudo nasce `internal` ou
  `private`; só é público o que outro pacote usa.
- Nomes de domínio seguem `docs/GLOSSARY.md`. Código em inglês; interface e documentação em pt-BR,
  com os textos em `res/values/strings.xml`.
- Versões de biblioteca só em `gradle/libs.versions.toml`.
- AGP 9, Kotlin 2.4 e Compose mudaram APIs recentes. Consulte a documentação atual (context7) antes
  de escrever configuração de build ou usar uma API que você não viu neste repositório.

## Docs

- `docs/SPEC.md`: o que o app faz, o checklist e a ordem de construção por fase. Leia antes de mudar
  comportamento e marque o checklist quando o teste que prova um item passar.
- `docs/ARCHITECTURE.md`: componentes Android, pacotes, seams e testes. Leia antes de criar ou mover
  código.
- `docs/GLOSSARY.md`: o vocabulário. Termo novo ou renomeado passa pela skill `domain-modeling`.
- `docs/DECISIONS.md`: armadilhas e escolhas não óbvias. Leia antes de "consertar" algo que parece
  estranho. Aprendizado novo entra no topo, com data.

## Git

Uma branch por fase (`feat/phase-1` a `feat/phase-4`), um commit por passo da fase e um PR para `main`
com merge commit, que preserva os commits no histórico.

## Skills

Use as skills globais na fase em que elas se aplicam: `codebase-design` ao desenhar pacote ou
interface, `tdd` ao implementar regra, `diagnosing-bugs` quando algo quebrar e `code-review` no PR de
cada fase, antes do merge.
