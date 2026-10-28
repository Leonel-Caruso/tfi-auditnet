## Achados

Menu `LOG`. Um **achado** é uma regra que não foi cumprida em uma auditoria, com a sua evidência. A lista é
**priorizada**: primeiro os pendentes, da maior para a menor severidade, considerando a criticidade do equipamento.

![Achados priorizados com o prazo de cada um](hallazgos.png)

Use os filtros por estado (incluindo *vencidos*), severidade, dispositivo, baseline e data, ou a busca por regra,
dispositivo ou evidência. A coluna de estado indica o prazo: *faltam N dias*, *vence hoje* ou *vencido há N dias*.

Ao escolher um achado, você vê a regra, o dispositivo, a auditoria que o detectou, a evidência, a recomendação e o
**acompanhamento** (cada mudança de estado com usuário, data e comentário). O seu perfil o consulta; o acompanhamento é
atualizado pelo analista de redes e pelo auditor técnico.

| Estado | Significa |
|---|---|
| aberto | detectado, ninguém o assumiu ainda |
| em revisão | alguém está trabalhando na correção |
| resolvido | corrigido; normalmente confirmado por uma nova auditoria |
| risco aceito | decidiu-se não corrigir, com justificativa |
