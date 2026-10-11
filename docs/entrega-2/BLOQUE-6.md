# Bloque 6 — Seguridad, usuarios y panel de administración

Casos de uso: CU-001-001 (iniciar sesión), CU-001-002 (gestionar usuarios y perfiles), CU-007-001 (organizaciones) y
CU-007-002 (sedes). Punto mandatorio: panel de administrador web (seguridad, bitácoras, salud de servicios, colas).
Decisiones: D-28 a D-34 en `REGISTRO-DECISIONES.md`. Migración management V4.

## API nueva o modificada (management-service)

| Método y ruta | Perfiles | Qué hace |
|---|---|---|
| `POST /api/auth/login` | Público | Igual que antes, más: bloqueo 15 min tras 5 contraseñas incorrectas (403), organización deshabilitada (403) |
| `POST /api/auth/password` | Cualquier usuario | `{"actual":"...","nueva":"..."}`. Cambia la propia contraseña y devuelve un login nuevo (token nuevo) |
| `GET /api/users` | Administrador | Usuarios con `bloqueadoHasta` y `ultimoAcceso` |
| `GET /api/users/{id}` | Administrador | Un usuario |
| `POST /api/users` | Administrador | Alta (aplica la política de contraseñas) |
| `PUT /api/users/{id}` | Administrador | `{"organizationId":2,"nombre":"...","email":"...","roles":["ANALISTA_RED"]}`. El nombre de usuario no cambia |
| `PATCH /api/users/{id}/status` | Administrador | `{"estado":"DESHABILITADO"}` o `ACTIVO` (baja lógica) |
| `POST /api/users/{id}/password` | Administrador | `{"password":"..."}`. Restablece, quita el bloqueo y cierra sus sesiones (204) |
| `POST /api/users/{id}/unlock` | Administrador | Quita el bloqueo por intentos fallidos (409 si no está bloqueada) |
| `GET /api/roles` | Administrador | Los cuatro perfiles |
| `PUT /api/roles/{id}` | Administrador | `{"descripcion":"..."}` (los perfiles son fijos, D-29) |
| `GET /api/roles/permissions` | Administrador | Matriz de permisos por perfil |
| `PUT /api/client-organizations/{id}` | Administrador | Modifica identificador y razón social |
| `PATCH /api/client-organizations/{id}/status` | Administrador | Habilita o deshabilita la organización |
| `GET /api/sites/{id}`, `PUT /api/sites/{id}` | Administrador | Consulta y modifica nombre y ubicación de una sede |
| `PATCH /api/sites/{id}/status` | Administrador | Habilita o deshabilita la sede |
| `GET /api/admin/health` | Administrador | Salud de management-service |
| `GET /api/admin/security` | Administrador | Resumen de seguridad de las últimas 24 horas |

En audit-core-service: `GET /api/system/health` (administrador), salud de audit-core-service.

Se elimina `POST /api/roles` (los perfiles no se crean desde la interfaz, D-29).

Errores: 400 (dato inválido o contraseña que no cumple la política), 401 (sin sesión, sesión cerrada por un cambio en
la cuenta, usuario u organización deshabilitados), 403 (perfil sin permiso, cuenta bloqueada), 404 (no existe),
409 (estado repetido, duplicado, protección del último administrador).

## Seguridad de cuentas

- **Política de contraseñas:** 10 a 72 caracteres, letras y números, sin el nombre de usuario. La pantalla muestra el
  avance mientras se escribe; el servidor la vuelve a validar.
- **Bloqueo:** 5 intentos fallidos seguidos → 15 minutos bloqueada (también cuentan los intentos con la contraseña
  actual incorrecta en Mi cuenta). El administrador la desbloquea desde Usuarios. Conviene tener dos administradores.
- **Deshabilitar en el acto:** cada solicitud verifica usuario, organización y fecha de las sesiones válidas (D-31).
  Deshabilitar un usuario o su organización, cambiar su contraseña, sus perfiles o su organización cierra sus
  sesiones abiertas.
- **Vencimiento del token:** 60 minutos (antes vencía a los 5 en la nube).
- **Bitácora de sistema, eventos nuevos:** USUARIO_MODIFICADO, USUARIO_HABILITADO, USUARIO_DESHABILITADO,
  CONTRASENA_CAMBIADA, CONTRASENA_RESTABLECIDA, CUENTA_BLOQUEADA, CUENTA_DESBLOQUEADA y ROL_MODIFICADO. Nunca se
  registran contraseñas.

## Pantallas

- **Usuarios y perfiles (`/users`, USR):** búsqueda y filtro por estado (activos, deshabilitados, bloqueados); alta y
  edición con perfiles (con su descripción); restablecer contraseña; desbloquear; habilitar y deshabilitar con
  confirmación; último acceso; perfiles con descripción editable y matriz de permisos.
- **Organizaciones y sedes (`/organizations`, ORG):** editar, habilitar y deshabilitar organizaciones y sedes. Con la
  organización deshabilitada se avisa y no se agregan sedes.
- **Mi cuenta (`/account`, menú del usuario → mi_cuenta):** datos de la cuenta y cambio de la propia contraseña.
- **Panel de administración (`/admin`, ADM):** salud de los dos servicios (versión, tiempo en línea, memoria,
  PostgreSQL y MongoDB), gateway, colas de mensajería ("no aplica"), indicadores de seguridad, eventos de 24 h con
  enlace a la bitácora filtrada, últimos rechazos y accesos a las tres bitácoras. Se actualiza cada minuto.
- **Bitácoras (`/logs`):** filtro con los eventos nuevos; acepta `?evento=` y `?resultado=` en la dirección.
- **Calendario:** los campos de fecha de Reportes, Bitácoras, Historial y Hallazgos usan el calendario de la consola
  (flechas para moverse, Enter para elegir, Escape para cerrar, "hoy" y "limpiar").
- **Reporte:** nuevo formato de documento en pantalla y en PDF (D-34). En el diálogo de impresión conviene desmarcar
  "Encabezados y pies de página" del navegador: el pie del reporte ya trae el número y la paginación.
- **Nombre:** AuditNet en todas las pantallas (D-28).

## Casos de prueba manuales (para la matriz de CP)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B6-01 | Usuarios → + usuario con contraseña "corta" | No deja guardar; las reglas de la contraseña se marcan mientras se escribe |
| CP-B6-02 | Alta de un usuario analista con contraseña válida | Aparece activo y "nunca ingresó"; Bitácoras → SYS muestra USUARIO_CREADO y ROLES_ASIGNADOS |
| CP-B6-03 | Ingresar con ese usuario y equivocar la contraseña 5 veces | La 5.ª responde que la cuenta está bloqueada; con la contraseña correcta sigue bloqueada |
| CP-B6-04 | Como administrador, Usuarios → filtro "bloqueados" → Desbloquear | La cuenta vuelve a ingresar; Bitácoras muestra CUENTA_BLOQUEADA y CUENTA_DESBLOQUEADA |
| CP-B6-05 | Con el analista con sesión abierta en otra ventana, deshabilitarlo | En la otra ventana, la siguiente acción lo devuelve al login; no puede volver a ingresar |
| CP-B6-06 | Editar el usuario y cambiarle el perfil | Sus sesiones abiertas se cierran; al volver a ingresar tiene el perfil nuevo |
| CP-B6-07 | Intentar deshabilitarse a sí mismo o quitarse el perfil de administrador | No se ofrece / responde que no se puede (409) |
| CP-B6-08 | Restablecer la contraseña de un usuario | Ingresa con la nueva; la sesión que tenía abierta se cierra |
| CP-B6-09 | Mi cuenta → contraseña actual incorrecta | Informa el error y la sesión sigue abierta |
| CP-B6-10 | Mi cuenta → cambiar la contraseña correctamente | Sigue trabajando; otra sesión abierta del mismo usuario vuelve al login |
| CP-B6-11 | Usuarios → editar la descripción de un perfil | Se ve en la lista y en el formulario de usuarios; Bitácoras muestra ROL_MODIFICADO |
| CP-B6-12 | Organizaciones → editar y luego deshabilitar una organización con usuarios | Sus usuarios no ingresan (sesiones abiertas incluidas); no deja agregar sedes; Bitácoras → TRX muestra MODIFICACION y CAMBIO_ESTADO |
| CP-B6-13 | Deshabilitar una sede → Dispositivos → nuevo dispositivo de esa organización | La sede no se ofrece; un equipo que ya estaba en ella la muestra como "(deshabilitada)" y la conserva |
| CP-B6-14 | Panel de administración | Ambos servicios operativos con la versión desplegada (sha-…), PostgreSQL con esquema V4 (management) y V6 (audit-core), MongoDB en línea; colas "no aplica" |
| CP-B6-15 | Panel → tocar CUENTA_BLOQUEADA | Abre la bitácora de sistema filtrada por ese evento |
| CP-B6-16 | Reportes → elegir fechas con el calendario y generar | El calendario respeta el estilo; el reporte se genera con ese rango |
| CP-B6-17 | Abrir un reporte → Descargar PDF | Documento con encabezado AuditNet, apartados numerados, pie "AuditNet · REP-NNN" y "Página X de Y" |
| CP-B6-18 | Recorrer las pantallas | Dice AuditNet en la pestaña, la barra superior y el login |
| CP-B6-19 | Como no administrador, desde la consola (F12) pedir `/api/admin/health` | 403 |

Para CP-B6-19, en la consola del navegador (F12, con la sesión iniciada):
`fetch('http://localhost:8081/api/admin/health', {headers: {Authorization: 'Bearer ' + sessionStorage.getItem('tfi.auth.token')}}).then(r => r.status)`

## Pendiente de actualizar en la carpeta de tesis

- CU-001-001: bloqueo por intentos fallidos, organización deshabilitada, mensaje de cuenta bloqueada (D-30).
- CU-001-002: modificación, baja lógica, restablecer y desbloquear; perfiles fijos con descripción editable y matriz de
  permisos (D-29); política de contraseñas (D-30); reglas de protección del último administrador.
- CU-007-001 y CU-007-002: modificación y baja lógica; efecto sobre usuarios, sedes y dispositivos (D-33).
- Requisitos no funcionales de seguridad: verificación de la sesión en cada solicitud y cierre de sesiones (D-31).
- Tabla 46 (diccionario): columnas nuevas de `usuarios` (`intentos_fallidos`, `bloqueado_hasta`, `ultimo_acceso`,
  `sesiones_validas_desde`).
- Matriz de Puntos Mandatorios: panel de administrador web (D-32), con la aclaración de que no hay colas de mensajería.
- Capturas del panel de administración, de Usuarios y del reporte en PDF para el manual de usuario.
