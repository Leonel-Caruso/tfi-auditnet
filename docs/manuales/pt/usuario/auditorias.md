## Auditorias

Menu `RUN`. No topo ficam as **configurações** importadas e abaixo as **auditorias executadas**. À direita, o contexto
do que você escolher.

### Importar uma configuração

1. Clique em **+ carregar configuração**.
2. Escolha o **dispositivo**.
3. Cole o **running-config** ou envie um arquivo `.txt` ou `.cfg` (o conteúdo aparece na caixa).
4. Clique em **Importar configuração**.

![Carregamento de uma configuração](configuracion-carga.png)

Cada importação é uma **nova versão** do equipamento (v1, v2, …). O original é mantido exatamente como chegou e é
gerada uma cópia normalizada para comparar. O arquivo original fica no repositório de arquivos com o seu hash SHA-256:
no contexto da configuração é possível baixá-lo, e o AuditNet verifica que ele não mudou.

### Executar uma auditoria

1. Escolha a configuração na lista. O contexto mostra a **baseline aplicável** e quantas regras ativas ela tem.
2. Clique em **executar auditoria**.

Se não aparecer uma baseline aplicável, falta uma baseline ativa para esse tipo de dispositivo na organização.

### Ler o resultado

![Resultado de uma auditoria com a avaliação de cada regra](auditoria-resultado.png)

- **regras avaliadas / cumpridas**, **achados** e **severidade máxima**.
- **Avaliação de regras:** cada regra com CUMPRE ou ACHADO e a sua evidência (a linha encontrada ou o que falta).
- **Ver achados →** abre os achados desta auditoria; **Ver no histórico (evolução) →** a compara com a anterior.

O que mais acontece ao auditar:

- Cada regra que não é cumprida gera um **achado**.
- Os achados de severidade **alta ou crítica** geram um **alerta crítico** (um só por equipamento e regra) e um e-mail
  para quem tem esse aviso ativado.
- Os achados pendentes de auditorias anteriores do mesmo equipamento cuja regra agora é cumprida passam sozinhos para
  **resolvido** e o seu alerta é fechado.
