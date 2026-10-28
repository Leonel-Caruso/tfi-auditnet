## Sua organização e suas sedes

Menu `ORG`. Você vê apenas a sua organização: à direita as sedes e abaixo o plano e os prazos de revisão.

![Sua organização com as sedes, o plano e o uso](organizacion-propia.png)

### Sedes

1. Clique em **+ sede**.
2. Digite o **nome** (único dentro da organização) e a **localização**.
3. Clique em **Registrar sede**.

**Editar** muda esses dados. **Desativar** uma sede impede atribuir a ela novos dispositivos; os que ela já tem mantêm
o seu histórico. **Ativar** a deixa disponível novamente.

### Plano e uso

Mostra o plano vigente e quantos dispositivos e usuários ativos estão em uso do total permitido. **ver Meu plano →**
abre o detalhe e o pedido de mudança (capítulo *Meu plano*).

### Prazos de revisão

![Prazos de revisão da organização](plazos-revision.png)

| Campo | O que define | Padrão |
|---|---|---|
| crítica / alta / média / baixa · dias | Em quantos dias vence um achado pendente dessa severidade, contados a partir da detecção | 7 / 15 / 30 / 60 |
| reauditar a cada · dias | Com que frequência a última configuração de um equipamento é auditada novamente | 30 |
| configuração desatualizada após · dias | A partir de quando um equipamento sem configuração nova conta como desatualizado | 30 |
| Reauditar automaticamente | Se a revisão diária audita sozinha os equipamentos cujo prazo foi cumprido | ativado |

Os prazos de severidade devem ir do menor para o maior (crítica ≤ alta ≤ média ≤ baixa) e cada valor vai de 1 a 365
dias. Clique em **Salvar prazos**: a mudança corrige na hora os vencimentos de todos os achados.

> A revisão periódica é executada todos os dias às 6:00 (horário da Argentina): reaudita o que for necessário e avisa
> por e-mail os achados vencidos e os equipamentos desatualizados. O AuditNet não se conecta aos equipamentos:
> reauditar é avaliar a última configuração carregada contra a baseline vigente.
