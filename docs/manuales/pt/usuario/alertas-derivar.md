## Alertas críticos

Menu `ALR`. Cada achado de severidade **alta ou crítica** gera um alerta (no máximo um vigente por equipamento e
regra). Eles servem para que alguém assuma rapidamente.

![Alertas priorizados com o alerta escolhido](alertas.png)

Filtre por estado (por padrão, os vigentes), severidade, criticidade do equipamento, dispositivo e sede. Os
indicadores mostram os vigentes, os sem atendimento, os encaminhados e os críticos.

| Estado | Significa |
|---|---|
| ativo | ninguém o assumiu ainda |
| em atendimento | foi encaminhado a um perfil técnico |
| fechado | o seu achado foi resolvido ou o risco foi aceito |

### Encaminhar ou devolver um alerta

![Detalhe do alerta com o achado que o originou](alerta-derivar.png)

1. Escolha o alerta: você vê o achado que o originou, o equipamento e a evidência.
2. Em **ação**, escolha:
   - **encaminhar a Analista de redes** ou **encaminhar a Auditor técnico**: passa a *em atendimento*; o comentário é
     opcional (o que deve ser revisado);
   - **devolver a ativo (sem atribuição):** o motivo é obrigatório.
3. Clique em **Atualizar alerta**.

Um alerta não é fechado manualmente: ele se fecha sozinho quando o seu achado é resolvido ou o risco é aceito. Para
reativá-lo, é preciso reabrir o achado.
