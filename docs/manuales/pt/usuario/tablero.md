## Painel de controle e estado do cliente

### Painel de controle

É a primeira tela ao entrar. Resume o estado da sua organização (o administrador do sistema escolhe a organização no
canto superior direito ou vê todas juntas).

- **Nível geral de risco:** alto, médio ou baixo, com o motivo.
  - **Alto:** há achados críticos pendentes ou alertas vigentes em equipamentos de criticidade alta ou crítica.
  - **Médio:** há achados de severidade alta pendentes em equipamentos de criticidade média ou baixa.
  - **Baixo:** restam apenas achados de severidade média ou baixa, ou nenhum.
- **Indicadores:** dispositivos auditados, conformidade atual, achados pendentes, críticos e altos, alertas vigentes,
  auditorias dos últimos 30 dias, achados vencidos e configurações desatualizadas. **Cada indicador abre o detalhe**
  (por exemplo, *achados vencidos* abre Achados filtrado por vencidos).
- **Achados pendentes por severidade** e **Desvios por semana** (detectados e fechados nas últimas 8 semanas). Passe o
  mouse sobre uma barra para ver os números, ou abra *ver tabela*.
- **Alertas vigentes**, **Auditorias recentes** e **Dispositivos com maior risco**, com links para cada um.

### Estado do cliente

Em **Estado por sede →** ou no menu `CLI`. Mostra o risco de cada sede, os achados pendentes por severidade, as
auditorias recentes, os dispositivos ordenados por nível de risco e os relatórios disponíveis da organização.

![Estado do cliente: risco geral e indicadores da organização](estado-cliente.png)

> **Conformidade atual** é a porcentagem de regras cumpridas na última auditoria de cada equipamento. Um **achado
> vencido** é um pendente que ultrapassou o prazo da sua severidade (por padrão: crítica 7 dias, alta 15, média 30 e
> baixa 60; cada organização pode ajustá-los).
