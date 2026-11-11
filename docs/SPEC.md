# Especificação

O que o app faz. A organização do código está em `docs/ARCHITECTURE.md`, o vocabulário em
`docs/GLOSSARY.md` e o porquê das escolhas em `docs/DECISIONS.md`.

## Objetivo

Despertador para Android de uso pessoal, para quem tem sono pesado: o alarme só para quando a pessoa
cumpre desafios, e enquanto ele toca o celular não deixa desligar pelo menu de energia, baixar o volume
nem fechar o app. Junta os desafios com câmera do PushClock com o bloqueio de desligamento do Desafios
Despertador. Quatro fases, e cada uma termina num PR que deixa o app instalável e funcionando.

Validado num Samsung Galaxy S23. Roda em Android 12 ou superior, sem esforço extra por outros aparelhos.

## Checklist

Cada linha só é marcada quando o teste que a prova existe e passa. O que depende do aparelho de verdade
é marcado depois de conferido no S23, e a prova diz isso.

| | Capacidade | Fase | Prova |
| - | ---------- | ---- | ----- |
| [x] | Criar, editar, ativar, desativar e excluir alarmes | 1 | `AlarmTest`, `AlarmBookTest`, `WakeGuardAppTest` |
| [ ] | Alarme dispara no horário, com o celular bloqueado e a tela apagada | 1 | |
| [ ] | Disparo só termina com todos os desafios cumpridos, ou pelo tempo máximo | 1 | |
| [ ] | Desafios de matemática, memória, sacudir e digitar frase, com dificuldade e quantidade | 1 | |
| [x] | Alarmes futuros sobrevivem a reinício e a mudança de hora ou fuso | 1 | `AlarmBookTest`; reinício e troca de fuso conferidos no emulador |
| [ ] | Menu de energia fecha sozinho durante o disparo | 2 | |
| [ ] | Volume travado e app preso durante o disparo | 2 | |
| [ ] | Desinstalar e forçar parada bloqueados durante o disparo | 2 | |
| [ ] | Painel de proteções no Início | 2 | |
| [ ] | Desafios de flexão e agachamento pela câmera | 3 | |
| [ ] | Desafio de expressões pela câmera | 3 | |
| [ ] | Histórico de despertares no Início | 4 | |

## Alarme

| Campo | Regra |
| ----- | ----- |
| Horário | Hora e minuto, no fuso do aparelho. |
| Dias da semana | Nenhum ou vários. Sem nenhum, o alarme toca uma vez, na próxima vez que o horário chegar, e se desativa. |
| Nome | Opcional, até 40 caracteres. |
| Som | O som padrão do app ou um toque do sistema. A escolha entra na tela junto com o disparo. |
| Vibração | Liga ou desliga. Padrão ligada. |
| Volume crescente | Liga ou desliga. Padrão desligado. Ligado, o som sobe de 20% ao volume do disparo em 30 segundos. |
| Desafios | De 1 a 5, em ordem. Cada um com tipo, dificuldade e quantidade. O mesmo tipo pode repetir. |
| Silenciar durante o desafio | Liga ou desliga. Padrão desligado. |
| Ativo | Alarme desativado não dispara e continua na lista. |

- O próximo disparo de cada alarme ativo é calculado a partir do horário e dos dias da semana, e é
  sempre no futuro. Editar ou reativar recalcula.
- Não existe soneca.
- Alarme pode ser editado, desativado ou excluído a qualquer momento antes de disparar.
- "Testar alarme" dispara na hora um ensaio do alarme. O ensaio tem o botão "Encerrar teste", não liga
  as proteções e não entra no histórico.

## Disparo

- No horário, o app toca o som no canal de alarme, em repetição, vibra se o alarme pedir, acende a
  tela e abre a tela de disparo por cima do bloqueio, sem pedir PIN.
- A tela de disparo mostra a hora, o nome do alarme e o progresso ("Desafio 2 de 3"). Os desafios
  aparecem um por vez, na ordem do alarme. Não há botão de parar.
- O disparo termina de dois jeitos:
  - **Cumprido:** todos os desafios foram concluídos.
  - **Expirado:** o tempo máximo de disparo passou sem isso.
- Tempo máximo de disparo: ajuste global, com 1, 2, 5, 10 ou 30 minutos, ou sem limite. Padrão 2
  minutos.
- Com "Silenciar durante o desafio", o som e a vibração param enquanto a pessoa interage com o desafio
  e voltam depois de 20 segundos sem interação. O tempo máximo continua contando.
- Sair da tela de disparo não para o som. A notificação do disparo leva de volta à tela. Na fase 2, a
  tela volta sozinha.
- Um alarme que chega durante outro disparo é ignorado. Se ele repete, o próximo disparo dele é
  agendado normalmente.
- Reiniciar o celular encerra o disparo em curso, e ele não é retomado. Os alarmes futuros são
  reagendados quando o celular liga. Um alarme cujo horário passou com o celular desligado não toca.

## Desafios

Todo desafio tem **dificuldade** (Fácil, Médio, Difícil) e **quantidade**. Errar nunca encerra o
desafio; só atrasa.

### Sem câmera (fase 1)

| Desafio | Quantidade | Dificuldade | Regras |
| ------- | ---------- | ----------- | ------ |
| Matemática | Contas: 1 a 10, padrão 3 | Fácil: soma ou subtração de dois números de dois dígitos. Médio: `a × b + c`, com `a` e `b` de 2 a 12 e `c` de dois dígitos. Difícil: `a × b + c`, com `a` de dois dígitos, `b` de 3 a 9 e `c` de três dígitos. | Resposta digitada num teclado numérico da própria tela. Resultado nunca é negativo. Errar troca a conta por outra. |
| Memória | Sequências: 1 a 5, padrão 2 | Tamanho da sequência: 4, 6 ou 8. | Quatro botões coloridos piscam numa ordem e a pessoa repete. Errar sorteia outra sequência do mesmo tamanho. |
| Sacudir | Sacudidas: 10 a 200 de 10 em 10, padrão 30 | Força mínima para uma sacudida contar. | Conta pelo acelerômetro. Sacudidas a menos de 250 ms uma da outra contam como uma. |
| Digitar frase | Frases: 1 a 5, padrão 1 | Fácil: frase curta, só minúsculas. Médio: frase com maiúsculas e pontuação. Difícil: frase longa com números e símbolos. | Frases em português, sorteadas de uma lista do app. A comparação é exata. Colar é bloqueado. |

### Com câmera (fase 3)

| Desafio | Quantidade | Dificuldade | Regras |
| ------- | ---------- | ----------- | ------ |
| Flexão | Repetições: 5 a 50, padrão 10 | O quanto o cotovelo precisa dobrar para a repetição contar. | Celular apoiado no chão, câmera frontal. Uma repetição é descer e subir com o corpo inteiro no quadro. |
| Agachamento | Repetições: 5 a 50, padrão 15 | O quanto o joelho precisa dobrar. | Celular apoiado, corpo inteiro no quadro. |
| Expressões | Expressões: 3 a 10, padrão 5 | Tempo segurando cada uma: 1, 2 ou 3 segundos. | Sequência sorteada entre sorrir, piscar o olho esquerdo, piscar o olho direito, abrir a boca e inclinar a cabeça para um lado. A mesma expressão não sai duas vezes seguidas. |

- A detecção roda no aparelho. Nenhuma imagem é gravada nem sai do celular.
- Sem permissão de câmera, o alarme não pode ser salvo com um desafio de câmera.

## Proteções (fase 2)

Valem só enquanto um disparo de verdade está em curso; ensaio não liga nenhuma. Dependem do serviço de
acessibilidade do app, que a pessoa liga uma vez nas configurações do Android.

| Proteção | O que faz durante o disparo |
| -------- | --------------------------- |
| Menu de energia | Fecha o menu de desligar e reiniciar assim que ele abre. |
| Volume | Mantém o canal de alarme no volume do disparo; as teclas de volume não baixam. |
| App preso | Sair para a tela inicial, para os recentes ou para outro app traz a tela de disparo de volta. Tirar o app dos recentes não para o som. |
| Desinstalar e forçar parada | Fecha a tela de informações do app, o diálogo de desinstalação e a tela do serviço de acessibilidade do próprio app. |

- Sem o serviço de acessibilidade ligado, o alarme dispara normalmente, só sem proteção.
- O reinício forçado pelos botões não é bloqueável por nenhum app, e o disparo não volta depois dele.
  Isso é uma escolha, ver `docs/DECISIONS.md`.
- Cada proteção tem uma chave nos Ajustes, todas ligadas por padrão.

## Telas

Três abas: **Início**, **Alarmes** e **Ajustes**. Interface em pt-BR, horários em 24 horas.

- **Início:** o próximo alarme com contagem regressiva ("Toca em 7 h 12 min"), ou um convite para criar
  o primeiro. Na fase 2, o painel de proteções: uma linha por permissão ou proteção, com o estado e um
  atalho para corrigir o que estiver faltando. Na fase 4, o histórico.
- **Alarmes:** lista por horário, com chave de ativar, nome, dias e os desafios. Tocar abre a
  edição; o botão "+" cria.
- **Edição do alarme:** os campos da tabela de Alarme, a lista de desafios com adicionar, remover e
  reordenar, "Testar alarme" e excluir com confirmação.
- **Ajustes:** tempo máximo de disparo, volume do disparo (padrão 100%), as chaves das proteções
  (fase 2) e os atalhos para as permissões do Android.
- **Disparo:** tela cheia, sem as abas, descrita em "Disparo".

## Histórico (fase 4)

- Cada disparo de verdade grava: alarme, instante em que começou, como terminou (cumprido ou expirado)
  e quanto tempo levou.
- O Início mostra os últimos 30 dias: tempo médio para cumprir, sequência atual de dias cumpridos e a
  lista dos disparos.

## Dados e distribuição

- Tudo fica no aparelho. Não há conta, servidor nem rede; o app não pede a permissão de internet.
- Instalação por APK, pelo cabo USB. Publicar na Play Store não está no plano.

## Fora desta versão

Ver a entrada "Fora do escopo da primeira versão" em `docs/DECISIONS.md`.

## Ordem de construção

Cada fase é uma branch com um PR para `main`, revisado com `/code-review` antes do merge commit.

**Fase 1, `feat/phase-1`: o alarme que funciona**

1. Documentos do projeto.
2. Esqueleto: app Compose vazio com as três abas, formatação, lint e testes rodando.
3. Alarmes: modelo, cálculo do próximo disparo, persistência e as telas de lista e edição.
4. Agendamento: alarme exato, reagendar no boot e na mudança de hora ou fuso.
5. Disparo: serviço com som e vibração, tela por cima do bloqueio, tempo máximo e ensaio.
6. Desafios sem câmera: matemática, memória, sacudir e digitar frase.
7. Início com o próximo alarme, Ajustes e permissões.
8. README com como instalar e rodar.

**Fase 2, `feat/phase-2`: proteções**

1. Serviço de acessibilidade e a tela que explica e pede a permissão.
2. Menu de energia.
3. Volume e app preso.
4. Desinstalar e forçar parada.
5. Painel de proteções no Início e as chaves nos Ajustes.

**Fase 3, `feat/phase-3`: câmera**

1. Câmera e detecção de pose na tela de disparo.
2. Flexão.
3. Agachamento.
4. Expressões.

**Fase 4, `feat/phase-4`: acabamento**

1. Histórico de despertares.
2. Polimento visual.
3. README final com prints.
