## Achados

Menu `LOG`. Um **achado** é uma regra que não foi cumprida em uma auditoria, com a sua evidência. A lista é
**priorizada**: primeiro os pendentes, da maior para a menor severidade, considerando a criticidade do equipamento.

![Achados priorizados com o prazo de cada um](hallazgos.png)

### Buscar

No topo há filtros por estado (incluindo *vencidos*), severidade, dispositivo, baseline e data de detecção, e uma
busca por regra, dispositivo ou evidência. Os indicadores mostram pendentes, críticos e altos, em revisão e fechados. A
coluna de estado indica o **prazo**: *faltam N dias*, *vence hoje* ou *vencido há N dias*.

### Detalhe e acompanhamento

Ao escolher um achado, você vê a regra, a condição, o dispositivo, a baseline, a auditoria que o detectou, o prazo, o
padrão buscado, a **evidência** e o **acompanhamento** (cada mudança de estado com usuário, data e comentário).

![Detalhe de um achado com o formulário de acompanhamento](hallazgo-seguimiento.png)

**Alterar o estado**

1. Escolha o **novo estado**.
2. Escreva um **comentário**: é obrigatório, exceto para passar a *em revisão*.
3. Clique em **Atualizar estado**.

| Estado | Quando usar | Pode passar a |
|---|---|---|
| aberto | recém-detectado ou reaberto | em revisão, resolvido, risco aceito |
| em revisão | alguém está trabalhando na correção | aberto, resolvido, risco aceito |
| resolvido | foi corrigido no equipamento | aberto (reabrir) |
| risco aceito | decide-se não corrigir, com justificativa (exceção ou falso positivo) | aberto (reabrir) |

> O habitual é corrigir no equipamento, importar a nova configuração e auditar: se a regra agora for cumprida, o
> achado passa sozinho para **resolvido** com o usuário SISTEMA e um comentário que indica a auditoria. Ao fechar um
> achado (resolvido ou risco aceito), o seu alerta é fechado; ao reabri-lo, o alerta volta a ficar vigente.
