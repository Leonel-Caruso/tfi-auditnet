## Permissões por perfil e onde são controladas

| Módulo · ação | ADM | ORG | ANL | AUD | RSP |
|---|---|---|---|---|---|
| Painel, estado do cliente, dispositivos, baselines, regras, histórico, achados, alertas e relatórios · consultar | ✔ | ✔ | ✔ | ✔ | ✔ |
| Dispositivos · registrar, editar e desativar | ✔ | ✔ | | | |
| Tipos de dispositivo · registrar | ✔ | | | | |
| Baselines e regras · criar, versionar e editar | ✔ | | | ✔ | |
| Configurações · importar; auditorias · executar | ✔ | | ✔ | ✔ | |
| Achados · alterar o estado de acompanhamento | ✔ | | ✔ | ✔ | |
| Alertas críticos · encaminhar ou devolver | ✔ | | ✔ | | ✔ |
| Relatórios · gerar | ✔ | | ✔ | ✔ | |
| Organizações · registrar, editar e desativar | ✔ | | | | |
| Sedes · registrar, editar e desativar | ✔ | ✔ | | | |
| Usuários · gerenciar administradores | ✔ | | | | |
| Usuários · gerenciar perfis de operação | ✔ | ✔ | | | |
| Plano · consultar os pedidos | ✔ | ✔ | | | ✔ |
| Plano · solicitar uma mudança | | ✔ | | | |
| Planos · atribuir, aprovar pedidos e editar preços | ✔ | | | | |
| Registros e painel de administração | ✔ | | | | |

ADM: administrador do sistema · ORG: administrador da organização · ANL: analista de redes · AUD: auditor técnico ·
RSP: responsável pela gestão de TI.

### Onde é controlado

| Camada | Onde | O que controla |
|---|---|---|
| API | `@RolesAllowed` em cada recurso de `api/rest` | Quais perfis podem chamar cada endpoint (403 se não puderem). |
| Escopo | `UsuarioAutenticado` (audit-core), `Actor` e serviços de usuários e sedes (management) | Que cada perfil, exceto o administrador do sistema, opere apenas sobre a sua organização. |
| Plano | `ControlPlan` e `PlanService` | Limites de dispositivos e usuários e o modo consulta do Piloto vencido. |
| Documentação | `MatrizPermisos` e `RolServiceTest` | A tabela mostrada na tela de Usuários; deve ser atualizada se uma permissão mudar. |
| Console | `Sidebar.astro` (perfis de cada item) e atributos `data-roles` / `data-admin-only` | Quais menus e botões são mostrados. É só conveniência: a segurança é aplicada pela API. |
| Testes | `test_perfiles.py`, `test_pantallas.py` e os testes de cada serviço | Que cada perfil veja e possa fazer o que lhe cabe. |
