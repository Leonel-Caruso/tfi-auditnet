## Permissions by profile and where they are enforced

| Module · action | ADM | ORG | ANL | AUD | RSP |
|---|---|---|---|---|---|
| Dashboard, client status, devices, baselines, rules, history, findings, alerts and reports · view | ✔ | ✔ | ✔ | ✔ | ✔ |
| Devices · register, edit and disable | ✔ | ✔ | | | |
| Device types · register | ✔ | | | | |
| Baselines and rules · create, version and edit | ✔ | | | ✔ | |
| Configurations · import; audits · run | ✔ | | ✔ | ✔ | |
| Findings · change the tracking status | ✔ | | ✔ | ✔ | |
| Critical alerts · assign or return | ✔ | | ✔ | | ✔ |
| Reports · generate | ✔ | | ✔ | ✔ | |
| Organizations · register, edit and disable | ✔ | | | | |
| Sites · register, edit and disable | ✔ | ✔ | | | |
| Users · manage administrators | ✔ | | | | |
| Users · manage operational profiles | ✔ | ✔ | | | |
| Plan · view the requests | ✔ | ✔ | | | ✔ |
| Plan · request a change | | ✔ | | | |
| Plans · assign, approve requests and edit prices | ✔ | | | | |
| Logs and administration panel | ✔ | | | | |

ADM: system administrator · ORG: organization administrator · ANL: network analyst · AUD: technical auditor ·
RSP: IT management lead.

### Where it is enforced

| Layer | Where | What it controls |
|---|---|---|
| API | `@RolesAllowed` on each resource in `api/rest` | Which profiles can call each endpoint (403 otherwise). |
| Scope | `UsuarioAutenticado` (audit-core), `Actor` and the user and site services (management) | That every profile, except the system administrator, works only on its own organization. |
| Plan | `ControlPlan` and `PlanService` | Device and user limits and the read-only mode of an expired Piloto. |
| Documentation | `MatrizPermisos` and `RolServiceTest` | The table shown on the Users screen; it must be updated if a permission changes. |
| Console | `Sidebar.astro` (profiles of each item) and the `data-roles` / `data-admin-only` attributes | Which menus and buttons are shown. It is only a convenience: security is enforced by the API. |
| Tests | `test_perfiles.py`, `test_pantallas.py` and each service's tests | That each profile sees and can do what it should. |
