# Plan de bloques 9 a 16

Origen: devolución de la Entrega 2 (multi idioma, manuales de usuario y de desarrollador, notificaciones por correo) e
ideas propias (administrador de organización, planes con límites, revisión periódica).

Decisiones tomadas con el autor:

| Tema | Decisión |
|---|---|
| Administrador de organización | Alcance: la organización completa (la sede es solo informativa) |
| Límites del plan | Dispositivos activos (carpeta) y usuarios activos (nuevo: Piloto 5, Inicial 15, Profesional 40) |
| Correo | Azure Communication Services Email; Mailpit en local |
| Revisión periódica | Vencimiento por severidad, re-evaluación periódica y aviso de configuración desactualizada |
| Orden | Funcionalidad primero; multi idioma y manuales al final para no traducir ni documentar pantallas que cambian |

Reglas de trabajo que se mantienen: un parche por bloque, rama `entrega-2` (o la que se defina), tests en verde antes de
avanzar, despliegue con aprobación, bitácoras en cada operación nueva y registro de decisiones.

## Resumen

| Bloque | Tema | Migraciones | Depende de |
|---|---|---|---|
| 9 | Cierre de la Entrega 2 y pruebas de pantalla en CI | — | — |
| 10 | Administrador de organización | — (rol nuevo por bootstrap) | 9 |
| 11 | Planes y límites | management V5 | 10 |
| 12 | Notificaciones por correo y recuperación de contraseña | management V6, audit-core V8 | 11 |
| 13 | Revisión periódica y vencimientos | audit-core V9 | 12 |
| 14 | Multi idioma (es, en, pt) | management V7 | 10–13 |
| 15 | Manuales de usuario y de desarrollador | — | 14 |
| 16 | Carpeta, trazabilidad y cierre | — | todos |

Los números de migración son tentativos: se asignan en orden al implementar cada bloque.

---

## Bloque 9 · Cierre de la Entrega 2

- Prueba de carga con k6 y evaluación SEO Optimer; completar `CARGA.md` y `SEO.md`.
- `app.delivery` a "Entrega 2" (y sus tests), README actualizado (Azure en lugar de Render), etiquetas de formularios en
  castellano como arreglo provisorio hasta el Bloque 14.
- **Pruebas de pantalla en el CI:** llevar al repo (`tests/pantallas/`) el servidor simulado y las pruebas con
  Playwright que hoy se corren a mano, y sumarlas como job de GitHub Actions. Base para probar los bloques siguientes.
- Tag `entrega-2-stable` y unión a `main` con aprobación del autor.

**Aceptación:** CI con 4 jobs en verde (tests de ambos servicios, frontend, pantallas), resultados de carga y SEO en docs.

---

## Bloque 10 · Administrador de organización

**Jerarquía**

| Nivel | Perfil | Alcance |
|---|---|---|
| 1 | Administrador del sistema | Toda la plataforma |
| 2 | Administrador de organización (`ADMINISTRADOR_ORGANIZACION`) | Su organización |
| 3 | Analista de red, Auditor técnico, Responsable de gestión IT | Su organización, operación diaria |

**Reglas**

- Solo el administrador del sistema crea administradores de organización (y de sistema).
- El administrador de organización crea, modifica, deshabilita, desbloquea y restablece la contraseña de usuarios de
  nivel 3 de su organización. No puede asignar perfiles de nivel 1 o 2 ni tocar usuarios de esos niveles.
- Gestiona las sedes y los dispositivos de su organización (respetando el plan desde el Bloque 11).
- Consulta todo lo de su organización. No ve bitácoras ni salud de la plataforma.
- Un usuario puede tener más de un perfil (ya se admite): en organizaciones chicas el administrador de organización
  puede ser además analista.

**Cambios**

- management: rol nuevo en el bootstrap; `UsuarioService` y `SedeService` validan el nivel y la organización del
  actor; `MatrizPermisos` y la tabla de perfiles del panel.
- audit-core: `@RolesAllowed` del alta y edición de dispositivos y tipos; filtro por organización.
- Frontend: Usuarios visible para el perfil nuevo con la organización fija; menú según perfil.

**Pruebas:** casos de `UsuarioServiceTest` por combinación actor/objetivo (otra organización → 403, escalar perfil →
403, tocar a un administrador → 403); prueba de pantalla del menú por perfil; CP manuales con dos organizaciones.

---

## Bloque 11 · Planes y límites

**Planes** (carpeta: Tabla 20 del plan de marketing y Tabla 32 de la estructura comercial):

| Código | Dispositivos | Usuarios | Vigencia | Precio de lista (Año 1) |
|---|---|---|---|---|
| PILOTO | 10 | 5 | 60 a 90 días (90 por defecto) | Bonificado o costo simbólico |
| INICIAL | 50 | 15 | — | USD 150/mes |
| PROFESIONAL | 150 | 40 | — | USD 300/mes |
| EMPRESA | más de 150, a medida | a medida | — | A cotizar |

- Los límites de usuarios son nuevos (confirmados por el autor): se agregan a las Tablas 20 y 32 de la carpeta en el
  Bloque 16.
- El precio es informativo y editable por el administrador del sistema: la carpeta lo revisa cada año (+12 % en el
  Año 2 y +15 % en el Año 3, Tabla 33), así que no se fija en el código.
- La solicitud de cambio muestra las condiciones comerciales de la carpeta: ampliación de Inicial a Profesional
  USD 350 por evento, descuento del 10 % por contratación anual, implementación inicial según el plan. Son datos para
  el cliente; no hay cobro en la plataforma.

**Modelo (management V5)**

- `planes`: código, nombre, máximo de dispositivos y de usuarios, días de vigencia, precio de referencia.
- `organizaciones_cliente`: plan, fecha de inicio, vencimiento (piloto) y límites propios (EMPRESA).
- `solicitudes_cambio_plan`: organización, plan actual, plan pedido, modalidad (mensual o anual), estado (PENDIENTE,
  APROBADA, RECHAZADA, CANCELADA), quién y cuándo pidió y resolvió, comentario.
- Organizaciones existentes: plan INICIAL.

**Reglas**

- Alta o rehabilitación de un dispositivo o de un usuario: 409 con mensaje claro si supera el límite.
- Bajar de plan: se rechaza si el uso actual supera el límite nuevo.
- Piloto vencido: la organización queda en solo consulta (no importa configuraciones ni ejecuta auditorías) hasta que
  se apruebe un plan.
- Cada servicio controla su recurso: management los usuarios; audit-core los dispositivos, leyendo el plan de la base
  en modo lectura (mismo criterio que la verificación de sesión).
- Sin cobros reales: la facturación queda fuera del alcance.

**API:** `GET /api/plans`; `GET /api/client-organizations/{id}/plan` (plan, uso y vencimiento);
`POST /api/client-organizations/{id}/plan-requests`; `GET /api/admin/plan-requests`;
`POST /api/admin/plan-requests/{id}/approve|reject`; asignación directa por el administrador del sistema.

**Pantallas:** "Mi plan" (uso con barras, días de piloto, botón Solicitar cambio); panel ADM con solicitudes
pendientes; plan visible en Organizaciones.

**Bitácora:** PLAN y SOLICITUD_PLAN en transacciones.

**Pruebas:** límite exacto (N permitido, N+1 rechazado), rehabilitar por encima del límite, bajar de plan con uso
mayor, piloto vencido, circuito completo de solicitud; pantallas.

---

## Bloque 12 · Notificaciones por correo

**Infraestructura**

- Azure Communication Services con un dominio administrado por Azure (`…azurecomm.net`), creado por un script nuevo
  (`08-crear-email.sh`). Envío por la API REST firmada con la clave de acceso (HMAC), igual que Blob Storage: no
  requiere registrar aplicaciones en Entra ID, que puede estar restringido en la suscripción de estudiante.
- Local: Mailpit en el docker compose (se ven los correos en `http://localhost:8025`).
- **Impacto financiero pendiente:** verificar el precio por correo de Azure Communication Services.

**Diseño**

- Cada servicio envía lo suyo: management (recuperación de contraseña, alta de usuario, solicitudes de plan) y
  audit-core (alertas críticas y, en el Bloque 13, vencimientos y resúmenes). Los destinatarios y sus preferencias se
  leen de la base.
- Tabla de salida (`correos_salientes`) por servicio: el correo se guarda en la misma transacción que el evento y se
  envía después; si falla, se reintenta. Nada se pierde si el proveedor no responde.
- Plantillas HTML con el logo y los colores de AuditNet, en el idioma del destinatario desde el Bloque 14.
- Preferencias por usuario: alertas críticas, recordatorios, resumen mensual (activables en Mi cuenta).
- Sin servicio configurado no se envía nada y la salud lo informa (CU de alertas: "no debe enviar alertas externas si
  el servicio de notificación no está configurado").

**Recuperación de contraseña:** "¿Olvidaste tu contraseña?" en el login; enlace de un solo uso que vence en 30 minutos
(se guarda solo el hash del token); la respuesta es siempre la misma exista o no la cuenta; cierra las sesiones al
cambiarla; queda en la bitácora de sistema.

**Opcional:** avisos dentro de la consola (campana con no leídos).

**Pruebas:** firma de la API (como `FirmaSharedKeyTest`), plantillas, reintentos, preferencias, token vencido o usado,
respuesta idéntica para cuentas inexistentes; correos verificados en Mailpit.

---

## Bloque 13 · Revisión periódica y vencimientos

Límite del alcance: la plataforma no se conecta a los equipos; "volver a auditar" es re-evaluar la última
configuración cargada contra la baseline vigente.

**Mecanismos**

1. **Vencimiento por severidad:** plazo por defecto crítica 7, alta 15, media 30, baja 60 días (configurable por
   organización). Un hallazgo pendiente con el plazo cumplido se marca vencido y se avisa por correo.
2. **Re-evaluación periódica:** cada organización define la frecuencia (por defecto 30 días). La auditoría queda
   marcada como automática y entra en el historial y la comparación como cualquier otra.
3. **Configuración desactualizada:** aviso si un dispositivo pasa N días (por defecto 30) sin configuración nueva.
4. **Resumen mensual** por correo al responsable de gestión IT y al administrador de organización.

**Ejecución:** un Azure Container Apps Job programado (una vez por día) llama a un endpoint interno de audit-core
protegido con un secreto. nginx no publica esa ruta. Así funciona aunque las apps estén en cero réplicas. Cada
ejecución es idempotente (no repite avisos ni re-evaluaciones del mismo período) y queda en las bitácoras.

**Modelo (audit-core V9):** política de revisión por organización; vencimiento y último aviso en hallazgos; origen
(MANUAL o AUTOMATICA) en auditorías.

**Pantallas:** hallazgos vencidos y dispositivos desactualizados en el tablero; filtro "vencidos" en Hallazgos;
configuración de plazos en la vista del administrador de organización.

**Pruebas:** fechas con reloj fijo, idempotencia (dos ejecuciones el mismo día), re-evaluación con reglas nuevas,
avisos una sola vez; ejecución manual del job en Azure.

---

## Bloque 14 · Multi idioma (español, inglés, portugués)

- Idioma guardado por usuario (management V7, `usuarios.idioma`), elegible en Mi cuenta y en la barra superior; en el
  login, selector que se recuerda en el navegador.
- Frontend: diccionarios `es`, `en`, `pt` y rutas por idioma de Astro (sin parpadeo de texto); los textos que arman
  los scripts usan el mismo diccionario.
- Backend: los errores llevan un código y el mensaje sale del idioma pedido (encabezado `Accept-Language`).
- Correos, reporte PDF y CSV en el idioma del usuario. Los datos cargados (nombres de reglas, descripciones) no se
  traducen.
- Se resuelven acá las etiquetas que siguen en inglés en los formularios.

**Pruebas:** chequeo en el CI de que los tres diccionarios tienen las mismas claves; pruebas de pantalla en los tres
idiomas sin claves faltantes ni desbordes.

---

## Bloque 15 · Manuales

**Manual de usuario**

- Un capítulo por perfil (administrador del sistema, administrador de organización, analista, auditor, responsable de
  gestión IT) con tareas paso a paso y capturas.
- Identidad visual: logo "A_", naranja `#f57c00`, gris oscuro `#171a1f`, tipografías JetBrains Mono e Inter.
- Fuente en Markdown en el repo y PDF generado con un script (reproducible); botón "Ayuda" en cada pantalla que abre
  la sección correspondiente.

**Manual de desarrollador e instalación**

- Qué hace la aplicación y para quién, arquitectura, estructura del repo y convenciones (puertos y adaptadores,
  migraciones, bitácoras, tests).
- Puesta en marcha local paso a paso; cómo agregar un endpoint, una migración, una regla, una pantalla y una traducción.
- CI/CD, despliegue en Azure (scripts 01 a 09), operación (modo demo, respaldos) y solución de problemas, con los casos
  reales que aparecieron en el desarrollo.
- Mapa por perfil: qué puede hacer cada perfil y dónde se controla en el código.

---

## Bloque 16 · Carpeta y cierre

- CU y RF nuevos: administrar usuarios de la organización, consultar plan, solicitar y aprobar cambio de plan,
  configurar notificaciones, recuperar contraseña, revisión periódica, cambiar idioma.
- Matrices de trazabilidad y de Puntos Mandatorios, diagramas (despliegue con correo y job), diccionario de datos.
- Presupuesto: costos reales de correo y job programado; planes del sistema iguales a los de la carpeta, con la columna
  de usuarios agregada en las Tablas 20 y 32.
- Tag final y unión a `main`.
