# Glossário

Despertador com desafios: o alarme só para quando a pessoa cumpre os desafios, e o celular fica
protegido contra desligamento enquanto ele toca. Os termos estão em português, como aparecem na
interface; o nome entre crases é o usado no código.

## Alarme

**Alarme** (`Alarm`):
Um horário configurado para acordar a pessoa, com dias da semana, som e uma lista de desafios. Existe
até ser excluído, tocando ou não.
_Evite_: despertador (é o app), lembrete, timer

**Alarme ativo**:
Alarme com a chave ligada. Só alarme ativo tem próximo disparo.
_Evite_: habilitado, ligado

**Alarme único**:
Alarme sem dias da semana. Toca uma vez e se desativa.
_Evite_: alarme avulso, one-shot

**Próximo disparo** (`nextRinging`):
O instante futuro em que um alarme ativo vai tocar, calculado do horário e dos dias da semana.
_Evite_: próxima ocorrência, próximo toque

**Som do alarme** (`AlarmSound`):
O áudio que toca no disparo: um som do app ou um toque do sistema.
_Evite_: toque (confunde com o disparo), ringtone, música

## Disparo

**Disparo** (`Ringing`):
O período em que um alarme está tocando, do horário até ser cumprido ou expirar. Só existe um por vez.
_Evite_: toque, alarme tocando, sessão

**Ensaio** (`rehearsal`):
Disparo iniciado por "Testar alarme". Pode ser encerrado por um botão, não liga proteções e não entra
no histórico.
_Evite_: teste (confunde com teste automatizado), preview, simulação

**Cumprido** (`COMPLETED`):
Desfecho do disparo em que todos os desafios foram concluídos.
_Evite_: desligado, dispensado, resolvido

**Expirado** (`EXPIRED`):
Desfecho do disparo em que o tempo máximo passou antes de os desafios serem concluídos.
_Evite_: perdido, ignorado, timeout

**Tempo máximo de disparo** (`maxRingingDuration`):
Quanto um disparo pode durar antes de expirar. Ajuste global, pode ser sem limite.
_Evite_: timeout, duração do alarme

**Tela de disparo** (`RingingActivity`):
A tela cheia que aparece por cima do bloqueio durante o disparo e mostra os desafios.

## Desafio

**Desafio** (`Challenge`):
Uma tarefa que a pessoa cumpre durante o disparo. Tem tipo, dificuldade e quantidade.
_Evite_: missão, tarefa, jogo

**Tipo de desafio** (`ChallengeType`):
Matemática, memória, sacudir, digitar frase, flexão, agachamento ou expressões.

**Dificuldade** (`Difficulty`):
`EASY`, `MEDIUM` ou `HARD`, na interface Fácil, Médio e Difícil. O que ela muda depende do tipo.
_Evite_: nível

**Quantidade** (`amount`):
Quantas rodadas o desafio tem: contas, sequências, sacudidas, frases, repetições ou expressões.
_Evite_: número, vezes, contagem

**Rodada** (`round`):
Uma unidade do desafio: uma conta, uma sequência, uma frase, uma repetição.
_Evite_: etapa, passo, questão

**Desafio de câmera**:
Desafio que confere o movimento ou o rosto pela câmera: flexão, agachamento e expressões.

**Repetição** (`rep`):
A rodada de flexão ou de agachamento: descer e subir uma vez.

**Expressão** (`Expression`):
A rodada do desafio de expressões: sorrir, piscar um olho, abrir a boca ou inclinar a cabeça.
_Evite_: careta, gesto

## Proteção

**Proteção** (`Protection`):
Algo que o app impede enquanto um disparo está em curso: abrir o menu de energia, baixar o volume,
sair do app, desinstalar ou forçar parada.
_Evite_: bloqueio (bloqueio é a tela de bloqueio do Android), trava, modo estrito

**Menu de energia**:
A tela do Android com "Desligar" e "Reiniciar".
_Evite_: menu de desligar, power menu

**Reinício forçado**:
Reiniciar o aparelho segurando os botões físicos. Nenhuma proteção cobre.

**Serviço de acessibilidade** (`GuardService`):
O serviço do Android que a pessoa liga uma vez e que deixa o app perceber e fechar telas do sistema
durante o disparo. Sem ele não há proteção.

**Painel de proteções**:
A parte do Início que mostra se cada permissão e proteção está em ordem.
_Evite_: status, checklist

## Histórico

**Despertar** (`WakeRecord`):
O registro de um disparo de verdade: alarme, quando começou, desfecho e duração.
_Evite_: log, evento, sessão

**Sequência** (`streak`):
Dias seguidos em que todo disparo foi cumprido. Não confundir com a sequência do desafio de memória,
que no código é `pattern`.
