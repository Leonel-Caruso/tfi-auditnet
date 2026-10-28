## Permisos por perfil y dónde se controlan

| Módulo · acción | ADM | ORG | ANL | AUD | RSP |
|---|---|---|---|---|---|
| Tablero, estado del cliente, dispositivos, baselines, reglas, historial, hallazgos, alertas y reportes · consultar | ✔ | ✔ | ✔ | ✔ | ✔ |
| Dispositivos · registrar, modificar y deshabilitar | ✔ | ✔ | | | |
| Tipos de dispositivo · registrar | ✔ | | | | |
| Baselines y reglas · crear, versionar y modificar | ✔ | | | ✔ | |
| Configuraciones · importar; auditorías · ejecutar | ✔ | | ✔ | ✔ | |
| Hallazgos · cambiar el estado de seguimiento | ✔ | | ✔ | ✔ | |
| Alertas críticas · derivar o devolver | ✔ | | ✔ | | ✔ |
| Reportes · generar | ✔ | | ✔ | ✔ | |
| Organizaciones · registrar, modificar y deshabilitar | ✔ | | | | |
| Sedes · registrar, modificar y deshabilitar | ✔ | ✔ | | | |
| Usuarios · gestionar administradores | ✔ | | | | |
| Usuarios · gestionar perfiles de operación | ✔ | ✔ | | | |
| Plan · consultar las solicitudes | ✔ | ✔ | | | ✔ |
| Plan · solicitar un cambio | | ✔ | | | |
| Planes · asignar, aprobar solicitudes y editar precios | ✔ | | | | |
| Bitácoras y panel de administración | ✔ | | | | |

ADM: administrador del sistema · ORG: administrador de organización · ANL: analista de redes · AUD: auditor técnico ·
RSP: responsable de gestión IT.

### Dónde se controla

| Capa | Dónde | Qué controla |
|---|---|---|
| API | `@RolesAllowed` en cada recurso de `api/rest` | Qué perfiles pueden llamar a cada endpoint (403 si no). |
| Alcance | `UsuarioAutenticado` (audit-core), `Actor` y servicios de usuarios y sedes (management) | Que cada perfil, salvo el administrador del sistema, opere solo sobre su organización. |
| Plan | `ControlPlan` y `PlanService` | Límites de dispositivos y usuarios y el modo consulta del Piloto vencido. |
| Documentación | `MatrizPermisos` y `RolServiceTest` | La tabla que muestra la pantalla de Usuarios; hay que actualizarla si cambia un permiso. |
| Consola | `Sidebar.astro` (perfiles de cada ítem) y atributos `data-roles` / `data-admin-only` | Qué menús y botones se muestran. Es solo comodidad: la seguridad la aplica la API. |
| Pruebas | `test_perfiles.py`, `test_pantallas.py` y las pruebas de cada servicio | Que cada perfil vea y pueda hacer lo que le corresponde. |
