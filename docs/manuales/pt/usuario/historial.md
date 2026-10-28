## Histórico de auditorias

Menu `HIS`. Todas as auditorias, com a configuração avaliada, a baseline e as regras usadas. É somente leitura.

![Histórico com o detalhe e a evolução de uma auditoria](historial.png)

### Buscar

Filtre por dispositivo, baseline, severidade máxima, resultado (conforme ou com achados), quem a executou e intervalo
de datas, e clique em **Consultar**. As auditorias feitas pela revisão periódica aparecem como **automática**.

### Detalhe e evolução

Ao escolher uma auditoria, você vê a configuração (versão e quem a importou), a baseline com a sua versão, as regras
aplicadas e os achados gerados com o seu estado atual. A **evolução** a compara com a auditoria anterior do mesmo
equipamento:

| Categoria | Significa |
|---|---|
| novos desvios | regras que antes eram cumpridas e agora não |
| corrigidos | regras que falhavam e agora são cumpridas |
| persistem | regras que continuam falhando |
| regras novas / retiradas | mudanças na baseline entre uma auditoria e outra |

**Ver só o histórico deste dispositivo** filtra a lista por esse equipamento e **Ver achados desta auditoria** abre os
seus achados.
