# Entrega 2 — Registro de decisiones

Formato: **DECISIÓN → CÓDIGO → DOCUMENTACIÓN (carpeta de tesis) → PRUEBA**.

## D-01 · Puntos Mandatorios 2026 como requisito de la cátedra

- **Decisión:** se toman como requisito los "Puntos Mandatorios de Carpeta 2026". Definen las tres bitácoras
  (transacciones, auditoría de sistema, excepciones con nivel de log configurable), la entidad NoSQL,
  el histórico del esquema, el deploy en la nube y el panel de administración.
- **Documentación:** incorporar la matriz Puntos 2026 contra AuditNet a la carpeta.

## D-02 · Reordenamiento de bloques

- **Decisión:** antes de ampliar funcionalidades se construye una base técnica transversal (Bloque 2):
  migraciones, deploy, bitácoras y CI. Motivo: los bloques funcionales cambian el esquema y todos los CU exigen
  registrar en bitácora.

## D-03 · Proveedor cloud: Azure (Azure for Students)

- **Decisión:** migrar de Render a Azure. Motivos: está dentro de los big three sugeridos por la cátedra, la base de
  Render gratuita vence a los 30 días y no tiene backups (RNF-009), y la cuenta de estudiante cubre la tesis.
- **Componentes previstos:** Static Web Apps (frontend), Container Apps (servicios Quarkus en Docker),
  PostgreSQL Flexible Server, Cosmos DB para MongoDB (bitácora de excepciones).
- **Impacto financiero pendiente:** el Excel debe reflejar el costo real de operación con precios de Azure pago,
  no los créditos de estudiante.
- **Documentación:** Figura 41 (despliegue), 10.4.5.2 (entorno servidor), Tabla 43 (tecnologías).
- **Render** queda congelado en la Entrega 1 (rama `main`) como respaldo hasta validar Azure.

## D-04 · Entidad NoSQL: bitácora de excepciones en MongoDB (Cosmos DB para MongoDB)

- **Decisión:** la bitácora de excepciones se persiste en NoSQL. PostgreSQL conserva el modelo de negocio y
  las bitácoras de transacciones y de auditoría de sistema.
- **Documentación:** Tabla 43 (reemplazar "SQL Server (BD alterna)"), DER de cada base.

## D-05 · Migraciones versionadas con Flyway (Bloque 2.1)

- **Decisión:** el esquema pasa a administrarse con Flyway. Cada servicio es dueño de sus tablas y tiene su propia
  tabla de historial. Hibernate pasa de `update` a `validate`.
- **Código:** `pom.xml` y `application.properties` de ambos servicios; `db/migration/V1__*.sql`.
- **Documentación:** `docs/database/MIGRACIONES.md`. En la tesis: 10.5.8 (criterios del modelo de datos) y
  Tabla 43 (agregar Flyway).
- **Prueba:** la V1 aplicada sobre una base limpia produce la misma estructura que la base local de la Entrega 1
  (columnas, tipos, nulabilidad, PK, UK, CHECK y FK por definición). Además: `mvn test` en ambos servicios,
  arranque local sobre una base nueva y validación de Hibernate sin errores.

## D-06 · Región y restricciones de la suscripción (Bloque 2.2)

- **Decisión:** región `brazilsouth`. La suscripción *Azure for Students* solo permite `southafricanorth`,
  `newzealandnorth`, `brazilsouth`, `canadacentral` y `chilecentral`.
- **Consecuencia:** Static Web Apps no está disponible en esas regiones, por lo que el frontend también se
  despliega como contenedor en Container Apps.

## D-07 · API Gateway con nginx (Bloque 2.2)

- **Decisión:** el contenedor web (nginx) sirve el sitio Astro y enruta `/api/...` a cada servicio.
  Los backends quedan con ingress interno (sin exposición a Internet) y no se necesita CORS.
- **Código:** `frontend/web-app/Dockerfile`, `frontend/web-app/nginx/default.conf.template`.
  El frontend se compila con URLs de API vacías (mismo origen); en local sigue usando `localhost:8081/8082`.
- **Documentación:** Tabla 42 y Figura 36 (el API Gateway pasa a existir físicamente), Figura 41 (despliegue).
- **Prueba:** enrutamiento verificado con nginx y servicios simulados: las 15 rutas van al servicio correcto,
  las páginas del sitio responden 200 sin redirecciones y se envían los encabezados de seguridad.

## D-08 · Imágenes en GitHub Container Registry publicadas por GitHub Actions (Bloque 2.2)

- **Decisión:** GitHub Actions ejecuta los tests (backend y frontend) y, solo si pasan, publica las 3 imágenes
  en `ghcr.io/leonel-caruso`. Se evita Azure Container Registry porque consume créditos.
- **Código:** `.github/workflows/imagenes.yml`. Adelanta parte del Bloque 2.4 (CI).
- **Trazabilidad:** cada imagen se etiqueta con `sha-<commit>`, así cada deploy apunta a un commit exacto.

## D-09 · Infraestructura como scripts versionados (Bloque 2.2)

- **Decisión:** la infraestructura se crea con scripts de Azure CLI en `infra/azure/`, ejecutados en Cloud Shell.
  Son reproducibles, quedan en el repositorio y no contienen secretos.
- **Documentación:** `docs/deploy/AZURE.md`.

## D-10 · Diseño de las tres bitácoras (Bloque 2.3)

- **Decisión:** según los Puntos Mandatorios 2026:
  - **Auditoría de sistema** (seguridad): PostgreSQL, `bitacora_sistema`, escribe management-service.
  - **Transacciones** (negocio, con valor anterior y nuevo): PostgreSQL, `bitacora_transacciones`, tabla
    creada por management-service y escrita por ambos servicios (audit-core ya depende del esquema de
    management, por lo que no se genera una dependencia circular).
  - **Excepciones** (nivel configurable): MongoDB (Cosmos DB para MongoDB), escriben ambos servicios.
- **Consulta:** management-service expone `/api/admin/bitacoras/...` solo para `ADMINISTRADOR_SISTEMA`.
- **Correlación:** nginx genera `X-Request-ID` por solicitud; los servicios lo guardan en cada registro y lo
  devuelven en la respuesta.
- **Documentación:** `docs/bitacoras/BITACORAS.md`. En la tesis: RF-014, RNF-003, Tabla 44 (clase Bitacora),
  Tabla 46 (diccionario: reemplaza `bitacora_actividad` por las tres bitácoras), nuevos CU y CP de consulta.

## D-11 · Bitácora de sistema: reglas de registro (Bloque 2.3a)

- **Rechazos (FALLO)** se guardan en una transacción nueva: quedan aunque la operación falle.
- **Éxitos (EXITO)** se guardan en la misma transacción que la operación: existen solo si la operación se confirmó.
- **Inmutable:** un trigger rechaza UPDATE, DELETE y TRUNCATE sobre la tabla.
- **Sin claves foráneas** hacia usuarios u organizaciones: el registro se conserva tal como ocurrió.
- **Login fallido:** se distingue internamente usuario inexistente / contraseña incorrecta / cuenta deshabilitada,
  pero la respuesta al cliente no revela si el usuario existe.
- **Prueba:** tests unitarios (login exitoso, contraseña incorrecta, usuario inexistente, cuenta deshabilitada,
  creación de usuario con roles, validación de filtros) y prueba de la migración: los triggers rechazan
  UPDATE, DELETE y TRUNCATE.

## D-12 · Bitácora de transacciones (Bloque 2.3b)

- **Decisión:** el puerto `TrazabilidadPort` de ambos servicios pasa de escribir texto en el log a registrar una
  `Transaccion` estructurada (entidad, id, operación, organización, valor anterior y nuevo en JSON, actor).
  Se guarda en la misma transacción que la operación.
- **Configuraciones:** se registra un resumen con hash SHA-256, no el contenido.
- **audit-core-service** inserta con SQL explícito (JDBC dentro de la transacción de Hibernate) en lugar de mapear
  la tabla como entidad, para no acoplar su arranque a la migración de management-service.
- **Prueba:** tests unitarios de alta de organización, alta de dispositivo, importación de configuración (hash y
  sin contenido) y filtros de consulta; migración V3 aplicada y triggers verificados.
- **Operación:** tras un deploy, management-service debe arrancar primero (aplica la V3). Hasta entonces, una alta
  en audit-core-service fallaría y se revertiría completa.

## D-13 · Bitácora de excepciones en MongoDB (Bloque 2.3c)

- **Decisión:** un mapeador global (`ErrorInesperadoMapper`) captura en ambos servicios los errores que ningún otro
  componente atendió, responde un 500 genérico con el código de correlación y los registra en MongoDB.
  `ExcepcionAplicacionMapper` (management) registra los rechazos de negocio como WARN.
- **Nivel configurable:** `BITACORA_EXCEPCIONES_NIVEL` (ERROR, WARN u OFF) y `LOG_LEVEL` para el log de Quarkus.
- **Escritura asíncrona** con timeouts cortos: la bitácora nunca demora ni rompe la respuesta al usuario.
- **Azure:** Cosmos DB para MongoDB (API 4.2) con plan gratuito, firewall limitado a servicios de Azure,
  conexión como secreto de Container Apps (`infra/azure/04-crear-cosmos.sh`). **Local:** `mongo:7` en el docker compose.
- **Diagnóstico:** `POST /api/admin/diagnostico/excepcion` (solo administradores) para demostrar el registro.
- **Documentación:** Tabla 43 (MongoDB reemplaza a "SQL Server (BD alterna)"), DER/diccionario (documento
  `bitacora_excepciones`), Figura 41 (despliegue).
- **Prueba:** tests del nivel configurable y de los filtros de consulta. En Azure: `POST /api/admin/diagnostico/excepcion`
  responde 500 con código de seguimiento y el registro aparece en `GET /api/admin/bitacoras/excepciones`.
- **Incidente de despliegue:** aunque el script pidió la API 6.0, Azure creó la cuenta con la API 3.6 y
  `az cosmosdb update --server-version` no la modificó. El driver de MongoDB 5.x exige 4.2 o superior, por lo que las
  excepciones no se guardaban (el error solo quedaba en el log del contenedor, sin afectar la respuesta al usuario).
  Se subió a 4.2 desde el Portal (desde 3.6 solo se permite 4.0 o 4.2). **Corrección:** `MONGO_VERSION="4.2"` y el
  script verifica la versión real al terminar y se detiene con instrucciones si no coincide.
  **Lección:** verificar siempre la configuración efectiva de un recurso cloud, no solo que el comando termine bien.

## D-14 · Pantalla de bitácoras en el panel de administración (Bloque 2.3d)

- **Decisión:** una sola pantalla `/logs` con tres pestañas (sistema, transacciones, excepciones), de solo lectura y
  exclusiva de `ADMINISTRADOR_SISTEMA`. Usa los endpoints de consulta existentes: no cambia el backend ni la base.
- **Código:** `frontend/web-app/src/pages/logs.astro`, tipos en `src/lib/api.ts`, ítem `BIT` en `Sidebar.astro`,
  ruta en `Topbar.astro` y en la barra de comandos (`MasterLayout.astro`), estilos `log-*` en `global.css`.
- **Corrección incluida:** en el build estático la ruta actual llega con barra final (`/logs/`), por lo que el menú no
  marcaba el ítem activo ni la ruta superior mostraba el módulo (en Azure se veía "console"). Se normaliza la ruta en
  `Sidebar.astro` y `Topbar.astro`; afecta a todas las pantallas.
- **Seguridad:** la autorización la decide la API (403); el frontend solo oculta el acceso. Todo valor recibido se
  escapa antes de mostrarse (los registros contienen datos ingresados por usuarios, por ejemplo el user-agent).
- **Documentación:** `docs/bitacoras/BITACORAS.md`. En la tesis: panel de administración (Puntos Mandatorios 2026),
  nuevo CU "Consultar bitácoras" con sus CP, y la pantalla en el prototipo de interfaz.
- **Prueba:** `astro check` y `astro build` sin errores; prueba de humo en navegador con API simulada (26 controles:
  carga y filtros de cada pestaña, parámetros enviados, inspector, búsqueda por código de seguimiento entre pestañas,
  validación de fechas e id, limpiar, navegación por teclado y por URL, escape de HTML, perfil sin permisos con 403,
  pantalla de 390 px sin desplazamiento horizontal). Prueba manual local y en Azure con datos reales.

## D-15 · Identificador de dispositivo único por organización (Bloque 3)

- **Decisión:** el identificador (`RTR-CORE-01`) es único dentro de cada organización, no en toda la plataforma.
  Motivo: AuditNet es B2B multicliente y dos clientes pueden nombrar igual a sus equipos (CU-002-001, regla 1).
- **Código:** migración audit-core `V2__activos_baselines_reglas.sql` (`uk_dispositivos_red_org_identificador`),
  `DispositivoService` y `PanacheDispositivoRepository.existeIdentificadorEnOrganizacion`.
- **Prueba:** test "permite el mismo identificador en otra organización" y "rechaza duplicado en la misma"; la
  migración se aplicó sobre una base con datos sin errores (la regla anterior era más estricta).

## D-16 · Ciclo de vida del dispositivo (Bloque 3)

- **Decisión:** `PUT /api/devices/{id}` modifica nombre, identificador, tipo, sede y criticidad; la organización no
  cambia. `PATCH /api/devices/{id}/status` hace la baja lógica (INACTIVO) o la reactivación. Nunca hay borrado físico
  (CU-002-001, reglas 4 y 5). Un dispositivo inactivo conserva su historial, pero no admite configuraciones nuevas
  ni auditorías.
- **Bitácora:** MODIFICACION, BAJA_LOGICA y CAMBIO_ESTADO con valor anterior y nuevo.
- **Documentación:** RF-003 y CU-002-001 (renombrar a "Gestionar dispositivo de red" y agregar los flujos de
  modificación y baja lógica).

## D-17 · Versionado de baselines y una baseline activa por alcance (Bloque 3)

- **Decisión:** el alcance de una baseline es organización + tipo de dispositivo, y solo una puede estar ACTIVA por
  alcance (CU-003-001, regla 3). El nombre y el alcance la identifican; la descripción se puede editar
  (`PUT /api/baselines/{id}`). Para cambiar los criterios se genera una versión nueva
  (`POST /api/baselines/{id}/versions`): copia las reglas activas, queda ACTIVA y la vigente pasa a INACTIVA.
  `PATCH /api/baselines/{id}/status` activa o desactiva.
- **Evidencia:** una baseline usada en auditorías no se sobrescribe (CU-003-001, reglas 1 y 5): sus reglas quedan
  congeladas y la API responde 409 con la indicación de versionar.
- **Código:** migración V2 (`uk_baseline_org_nombre_version`, versión ≥ 1), `BaselineService`, `BaselineResource`;
  `GET /api/baselines` informa `auditorias` por versión.
- **Prueba:** tests de alta con alcance ocupado, versión nueva (numeración, copia de reglas, reemplazo de la vigente
  y transacciones), activación con otra activa y edición de descripción.

## D-18 · Reglas: valor esperado, impacto y edición controlada (Bloque 3)

- **Decisión:** se agrega el tipo `VALOR_ESPERADO` (parámetro + valor esperado), previsto en la tesis: "criterios de
  presencia, ausencia o valor esperado de comandos o parámetros críticos". Se agrega el campo obligatorio `impacto`
  (RF-006 y RF-009); las reglas anteriores reciben un texto que indica que no lo tenían documentado.
- **Motor:** VALOR_ESPERADO cumple si alguna línea que empieza con el parámetro (palabras completas, sin distinguir
  mayúsculas ni espacios repetidos) tiene exactamente el valor esperado. La copia en evaluaciones y hallazgos guarda
  `parámetro = valor` y el hallazgo guarda también el impacto.
- **Validación de la condición** (CU-003-002, flujo 11): una sola línea, el valor esperado solo en VALOR_ESPERADO, y
  no se permiten dos reglas activas con la misma condición en una baseline (409).
- **Edición:** `PUT /api/rules/{id}` (código y baseline fijos) y `PATCH /api/rules/{id}/status` (baja lógica). No se
  crean, modifican ni reactivan reglas de una baseline auditada (409: hay que versionar); la baja lógica sí se
  permite porque no sobrescribe la regla (CU-003-002, flujo alternativo 6). Tampoco se permiten dos reglas activas
  contradictorias (DEBE_CONTENER y NO_DEBE_CONTENER del mismo patrón).
- **Rutas:** se mantienen `/api/rules` y `/api/baselines/{id}/rules`. **Documentación:** en CU-003-002 reemplazar
  `/api/audit-rules` por estas rutas (es más barato alinear la tesis que duplicar endpoints).
- **Prueba:** 5 tests nuevos del motor (valor esperado) y 6 del servicio de reglas.

## D-19 · Altas de organización, sede y tipo de dispositivo desde la interfaz (Bloque 3)

- **Decisión:** las pantallas permiten registrar organizaciones y sedes (RF-015, RF-016) y tipos de dispositivo,
  usando endpoints que ya existían. El alta de tipo ahora valida duplicados (409), responde errores en JSON y se
  registra en la bitácora de transacciones (`TIPO_DISPOSITIVO`, sin organización: es un catálogo común).
- **Corrección incluida:** las tablas anchas ya no ensanchan la página en pantallas chicas (se desplazan dentro de su
  panel).

## D-20 · Seguimiento de hallazgos (Bloque 4)

- **Decisión:** el hallazgo tiene un estado de seguimiento (CU-005-001): ABIERTO, EN_REVISION, RESUELTO y ACEPTADO
  (riesgo aceptado con justificación, por ejemplo una excepción o un falso positivo). La tesis no enumera los estados;
  se eligieron los mínimos para distinguir pendiente, en curso y cerrado.
- **Transiciones:** desde ABIERTO o EN_REVISION se puede pasar a cualquier otro estado; un hallazgo cerrado (RESUELTO
  o ACEPTADO) solo se reabre. Cerrar o reabrir exige comentario.
- **Trazabilidad:** el hallazgo guarda el último cambio (usuario y fecha) y la tabla `seguimientos_hallazgo` (solo
  inserción) guarda cada cambio con su comentario. Además se registra CAMBIO_ESTADO en la bitácora de transacciones.
  La evidencia, la severidad y la regla del hallazgo no se modifican nunca (CU-005-001, reglas 1, 4 y 7).
- **Permisos:** `PATCH /api/findings/{id}/status` para administrador, analista de redes y auditor técnico; el
  responsable de gestión IT solo consulta (403). Los no administradores operan solo sobre su organización.
- **Concurrencia:** el cambio de estado bloquea la fila del hallazgo (`SELECT ... FOR UPDATE`) y verifica que siga en
  el estado que vio el usuario; si otro usuario lo cambió antes, responde 409 y pide actualizar la pantalla. Así no se
  pierde un cambio ni queda un seguimiento con un "estado anterior" incorrecto.
- **Código:** migración audit-core `V3__seguimiento_hallazgos.sql`, `EstadoHallazgo`, `HallazgoService`,
  `HallazgoResource`, `results.astro`.
- **Prueba:** 6 tests del servicio (cambio con historial y bitácora, comentario obligatorio sin alterar la evidencia,
  transiciones inválidas y reapertura, organización ajena, priorización, rango de fechas) y la migración V3 sobre
  una base con datos (triggers de solo inserción verificados).

## D-21 · Filtros y priorización del lado del servidor (Bloque 4)

- **Decisión:** `GET /api/findings` filtra por estado, severidad, dispositivo, baseline, auditoría y fechas, y
  `GET /api/audits/history` por dispositivo, baseline, severidad máxima, resultado, usuario y fechas (CU-005-001 y
  CU-005-002, flujos 8 a 10). Los filtros solo leen; nunca modifican lo guardado.
- **Priorización de hallazgos:** pendientes primero, luego severidad, luego **criticidad del dispositivo**
  (CU-002-001, regla 3: la criticidad se usa para priorizar hallazgos) y por último la fecha.

## D-22 · Historial de auditorías con evolución (Bloque 4)

- **Decisión:** nueva pantalla `/history` (ítem HIS). El detalle de una auditoría muestra la configuración evaluada,
  la baseline usada, las reglas aplicadas y el estado actual de sus hallazgos (CU-005-002). `GET /api/audits/{id}/comparison`
  la compara con la auditoría anterior del mismo dispositivo regla por regla: desvíos nuevos, corregidos, persistentes,
  reglas nuevas y retiradas ("comparar resultados en el tiempo", observaciones de CU-005-002; detección de drift).
- **Base de la comparación:** las copias de cada evaluación (código, nombre, severidad, resultado), así que no la
  afectan los cambios posteriores de reglas o baselines.
- **Prueba:** 2 tests del comparador (todas las categorías y su orden; primera auditoría sin anterior).

## D-23 · Alertas críticas como entidad propia (Bloque 5)

- **Decisión:** tabla `alertas_criticas` (migración audit-core V4), una alerta por hallazgo CRITICA o ALTA, generada
  por el motor al auditar (CU-006-002, regla 1). Conserva el hallazgo y la auditoría de origen (regla 2) y no se borra
  nunca: un trigger rechaza DELETE y TRUNCATE (regla 5). Al migrar se crearon las alertas de los hallazgos CRITICA/ALTA
  existentes (cerradas si el hallazgo ya estaba resuelto o aceptado).
- **Estados:** ACTIVA → EN_ATENCION (derivada a ANALISTA_RED o AUDITOR_TECNICO) → CERRADA. Desde la pantalla de
  alertas solo se deriva o se devuelve (con motivo). El cierre sigue al hallazgo: RESUELTO o ACEPTADO cierran la
  alerta; reabrir el hallazgo la reactiva (flujo alternativo 4). Así la alerta y el hallazgo nunca se contradicen.
- **Permisos:** consultan los cuatro perfiles; derivan o devuelven el administrador, el responsable de gestión IT y
  el analista de redes. El auditor técnico solo consulta (403, flujo alternativo 6). Cada cambio va a la bitácora de
  transacciones como entidad ALERTA (regla 3). Mismo bloqueo de fila que en hallazgos para no pisar cambios; la
  sincronización con el hallazgo no exige el estado esperado (si la derivaron mientras tanto, igual se cierra).
- **Priorización:** vigentes primero, severidad, criticidad del dispositivo y fecha (regla 4). El impacto es texto
  libre y se muestra en el detalle, pero no ordena.
- **Sin notificaciones externas** (regla 6): no hay servicio de correo configurado.
- **Alternativa descartada:** alertas como vista de los hallazgos (sin tabla). Más simple, pero sin la entidad
  AlertaCritica ni EstadoAlerta que define la tesis.
- **Prueba:** 5 tests de `AlertaServiceTest` y 1 en `HallazgoServiceTest` (cierre y reactivación con el hallazgo).

## D-24 · Tablero de control y estado del cliente (Bloque 5)

- **Decisión:** `GET /api/dashboard/summary` devuelve en una sola respuesta los indicadores, las auditorías recientes,
  los equipos con mayor riesgo y la evolución de las últimas 8 semanas. Reemplaza a `/indicators`, `/recent-audits`
  y `/critical-alerts` de CU-006-003 (una sola consulta, sin estados intermedios en la pantalla); las alertas
  vigentes se leen de `/api/alerts/critical`. Del mismo modo, `GET /api/client-organizations/{id}/audit-status`
  incluye el resumen, el riesgo y los últimos reportes (reemplaza a `/summary` y `/risk` de CU-007-003).
- **Gateway:** `/api/client-organizations/*` es de management-service, pero `/audit-status` se calcula en audit-core.
  nginx deriva esa ruta puntual a audit-core (location regex anterior a la de management).
- **Cálculo:** en memoria sobre los datos del alcance (una organización, o todas para el administrador). Solo
  lectura: nada se guarda. Alcanza para el volumen del prototipo; con mucho volumen convendría agregar en SQL.
- **Indicadores:** dispositivos registrados, auditados y activos sin auditar; cumplimiento actual (reglas cumplidas en
  la última auditoría de cada equipo); hallazgos pendientes, en revisión y cerrados por severidad; alertas vigentes;
  auditorías de los últimos 30 días. Cada indicador lleva al módulo de detalle con el filtro aplicado (regla 5).
- **Evolución:** por semana de lunes a domingo (hora de Argentina): auditorías, hallazgos detectados y cerrados (por
  la fecha de cierre del hallazgo).
- **Permisos:** los no administradores ven solo su organización; pedir otra responde 403. El administrador puede
  elegir una organización o ver todas.
- **Prueba:** 5 tests de `TableroServiceTest`.

## D-25 · Nivel general de riesgo (Bloque 5)

- **Decisión:** reglas simples y explicables, iguales para organización, sede y dispositivo (`EvaluadorRiesgo`):
  1. SIN_DATOS: nunca se auditó.
  2. ALTO: hay un hallazgo CRITICA pendiente, o una alerta vigente en un equipo de criticidad ALTA o CRITICA.
  3. MEDIO: hay un hallazgo ALTA pendiente.
  4. BAJO: solo quedan hallazgos MEDIA o BAJA pendientes, o ninguno.
- **Alternativa descartada:** puntaje ponderado (severidad × criticidad con umbrales). Más fino, pero obliga a
  justificar pesos y umbrales arbitrarios.
- **Prueba:** `TableroServiceTest.reglasDelNivelDeRiesgo`.

## D-26 · Re-auditoría: una alerta por regla y resolución automática (Bloque 5)

- **Decisión:** al auditar un equipo, los hallazgos pendientes de auditorías anteriores del mismo equipo cuya regla
  (mismo código) ahora se cumple pasan solos a RESUELTO, con usuario SISTEMA, comentario ("se cumple en la auditoría
  AUD-n ejecutada por ...") en el seguimiento y en la bitácora, y su alerta se cierra. Es lo que define CU-005-001:
  "RESUELTO se confirma con una nueva auditoría".
- **Sin duplicar alertas:** si la regla sigue fallando, la auditoría nueva genera su hallazgo (cada auditoría
  conserva su evidencia), pero no una segunda alerta: hay como máximo una alerta vigente por equipo y regla. Si
  alguien cierra a mano el hallazgo que tenía la alerta y queda otro pendiente de la misma regla, la alerta pasa a
  ese (se genera una nueva). Reabrir un hallazgo no crea otra alerta si la regla ya tiene una vigente.
- **Solo hacia adelante:** se resuelven los hallazgos detectados sobre una versión de configuración igual o anterior
  a la auditada; re-auditar una versión vieja no resuelve lo que falla en una más nueva.
- **Severidad mayor:** si la regla subió de severidad (por ejemplo de ALTA a CRITICA), la alerta anterior se cierra
  ("reemplazada") y la vigente pasa a ser la del hallazgo nuevo, para que se priorice como corresponde.
- **Concurrencia:** la auditoría bloquea la fila del dispositivo, así dos auditorías simultáneas del mismo equipo se
  ejecutan en orden. Si otro usuario cambió un hallazgo en ese momento, la resolución automática no lo toca.
- **Datos anteriores:** la migración V4 había creado una alerta por cada hallazgo existente, así que un equipo
  auditado varias veces sin corregir quedaba con varias alertas vigentes para la misma regla. La migración V6 las
  consolida: conserva una (la que está en atención, si no la más grave y más reciente) y cierra las demás con un
  comentario que indica cuál quedó vigente.
- **Alternativa descartada:** un estado REEMPLAZADO para los hallazgos repetidos (más prolijo, pero suma un estado,
  una migración y cambios en todas las pantallas).
- **Prueba:** `ReauditoriaTest` (5 tests: auditar fallando y luego corregido; traslado y reactivación de la alerta;
  versión vieja; aumento de severidad).

## D-27 · Reportes de auditoría como foto fija (Bloque 5)

- **Decisión:** `POST /api/reports/audits` arma el reporte con las auditorías que cumplen los criterios (rango de
  fechas o auditorías específicas, dispositivo, severidades y estados de los hallazgos) y lo guarda en
  `reportes_auditoria` (migración audit-core V5): título, criterios y contenido en JSON, totales, usuario y fecha
  (regla 1). El contenido incluye, por auditoría, el dispositivo, la configuración y la baseline usadas, y por
  hallazgo la regla, severidad, impacto, evidencia, recomendación y estado en ese momento (reglas 2 a 4).
  `reportes_auditoria_auditorias` guarda la referencia a cada auditoría de origen.
- **Foto fija:** el reporte no se modifica ni se borra (triggers de solo inserción) y no cambia aunque cambien los
  hallazgos después; generarlo no altera los datos de origen (regla 5). Se registra en la bitácora como REPORTE.
- **JSON en `text`** (no `jsonb`): el contenido solo se lee completo, no se consulta por dentro, y así se evita
  depender de un mapeo específico del dialecto. Máximo 500 auditorías por reporte.
- **Descargas:** PDF con la impresión del navegador sobre la vista del reporte (estilos de impresión que dejan solo
  el documento; sin librerías nuevas en el servidor). `GET /api/reports/{id}/download` devuelve CSV para Excel
  (separador ";", UTF-8 con BOM, celdas que empiezan con = + - @ neutralizadas).
- **Sin resultados:** si los criterios no devuelven auditorías responde 200 con un mensaje y no guarda nada (flujo
  alternativo 3). Datos incompletos (configuración o baseline faltante) se informan en el reporte (flujo 4).
- **Permisos:** generan el administrador, el analista de redes y el auditor técnico (la tesis menciona que el
  analista "genera reportes"); el responsable de gestión IT los consulta y descarga. Los no administradores solo
  ven los de su organización. El administrador puede generar uno de todas las organizaciones.
- **Prueba:** `ReporteServiceTest` (5 tests, incluye el CSV).

## D-28 · Nombre del producto: AuditNet (Bloque 6)

- **Decisión:** la interfaz usa el nombre **AuditNet** en todas las pantallas (título de la pestaña, barra superior,
  login, menú lateral y reportes). Hasta el Bloque 5 algunas pantallas decían "AuditConfig", nombre de trabajo
  anterior. El logo sigue siendo "A_" y sigue llevando al tablero.
- **Alcance:** solo textos visibles. No cambian rutas, nombres de servicios, tablas ni variables.

## D-29 · Perfiles fijos y matriz de permisos (Bloque 6)

- **Decisión:** los cuatro perfiles de la tesis (administrador del sistema, analista de redes, auditor técnico y
  responsable de gestión IT) son **fijos**. Sus permisos están aplicados en el código (`@RolesAllowed` en cada
  endpoint) y se documentan en `MatrizPermisos`, que la pantalla de Usuarios muestra (`GET /api/roles/permissions`).
  Desde la interfaz solo se edita la descripción de cada perfil (`PUT /api/roles/{id}`, evento ROL_MODIFICADO).
- **Motivo:** un perfil nuevo creado desde la pantalla no tendría permisos en ningún endpoint; la matriz configurable
  (tabla de permisos + chequeo dinámico) no la pide la tesis y agregaría complejidad sin uso. Se elimina
  `POST /api/roles`.
- **Reglas de protección:** nadie se deshabilita ni se quita el perfil de administrador a sí mismo, y siempre queda al
  menos un administrador activo (409 si no). Los usuarios no se borran: se deshabilitan (baja lógica). El nombre de
  usuario no se modifica porque identifica al actor en las bitácoras.

## D-30 · Política de contraseñas y bloqueo por intentos fallidos (Bloque 6)

- **Política** (`PoliticaContrasena`): entre 10 y 72 caracteres (72 es el límite de BCrypt), letras y números, y sin
  contener el nombre de usuario. Se aplica al crear un usuario, al restablecer y al cambiar la propia contraseña. Las
  contraseñas existentes no se invalidan: la política rige desde el próximo cambio.
- **Bloqueo:** 5 contraseñas incorrectas seguidas bloquean la cuenta 15 minutos (403 con los minutos que faltan).
  Mientras está bloqueada no se valida la contraseña. Un ingreso correcto reinicia el contador. El administrador puede
  desbloquear antes (`POST /api/users/{id}/unlock`). Eventos: CUENTA_BLOQUEADA y CUENTA_DESBLOQUEADA.
- **Cambio de la propia contraseña:** una contraseña actual incorrecta cuenta como intento fallido, para que con una
  sesión robada no se pueda adivinar la contraseña sin límite.
- **Recomendación operativa:** tener al menos dos administradores. El bloqueo es por cuenta: si alguien prueba
  contraseñas contra el único administrador, este queda bloqueado 15 minutos y nadie más puede desbloquearlo.
- **Persistencia:** columnas nuevas en `usuarios` (migración management V4): `intentos_fallidos`, `bloqueado_hasta`,
  `ultimo_acceso`. El intento fallido se guarda en una transacción propia con bloqueo de fila, para que dos intentos
  simultáneos no se pisen el contador.
- **Respuesta al login:** usuario inexistente y contraseña incorrecta siguen respondiendo igual (401), para no revelar
  qué usuarios existen. El bloqueo sí se informa (403), para que la persona sepa por qué no puede ingresar.
- **Prueba:** `AuthServiceTest` (bloqueo, vencimiento, reinicio del contador, organización deshabilitada, cambio de la
  propia contraseña) y `PoliticaContrasenaTest`.
- **Alternativa descartada:** login con proveedor externo (OIDC). El login propio con JWT alcanza para la tesis.

## D-31 · Verificación de la sesión en cada solicitud (Bloque 6)

- **Decisión:** los dos servicios verifican en cada solicitud autenticada (un `SecurityIdentityAugmentor` con una
  consulta JDBC a la base compartida) que el usuario y su organización sigan activos y que el token no sea anterior a
  `usuarios.sesiones_validas_desde`. Si no, responden 401 y la consola vuelve al login.
- **Efecto:** deshabilitar un usuario o su organización lo deja afuera **en el acto**, sin esperar a que venza el
  token. Cambiar la contraseña (propia o restablecida), los perfiles o la organización de un usuario cierra sus
  sesiones abiertas; al cambiar la propia contraseña se devuelve un token nuevo para seguir trabajando.
- **Administrador:** no queda afuera si se deshabilita su organización (es el usuario de la plataforma, no del
  cliente).
- **Deshabilitar también cierra:** al deshabilitar un usuario o una organización se actualiza
  `sesiones_validas_desde`, así que si después se vuelve a habilitar los tokens viejos no recuperan validez.
- **Segundos:** el token guarda la emisión en segundos; `sesiones_validas_desde` se guarda truncado al segundo para
  que el token nuevo emitido en el mismo segundo no quede rechazado.
- **Cambios sobre la propia cuenta:** si el administrador se cambia el perfil o la organización, la pantalla avisa y
  lo devuelve al login; su propia contraseña la cambia desde Mi cuenta.
- **Falla segura:** si la base no responde, la sesión se rechaza.
- **Costo:** una consulta por clave primaria por solicitud; aceptable para la escala del prototipo.
- **Corrección incluida:** el token se emitía sin vencimiento explícito y en la nube vencía a los 5 minutos (valor por
  defecto de SmallRye JWT), aunque el login informaba 60. Ahora se emite con 60 minutos.
- **Prueba:** `ReglaSesionTest` (en los dos servicios) y `UsuarioServiceTest` (qué cambios cierran las sesiones).

## D-32 · Panel de administración (Bloque 6)

- **Decisión:** pantalla `/admin` (ítem ADM, solo administrador) que cubre el punto mandatorio "panel de administrador
  web: seguridad, bitácoras, salud de servicios, colas":
  - **Salud:** `GET /api/admin/health` (management) y `GET /api/system/health` (audit-core; no usa `/api/admin` porque
    el gateway deriva esa ruta a management). Informan versión desplegada (`APP_VERSION`, la etiqueta de la imagen),
    tiempo en línea, memoria, PostgreSQL (versión del esquema según Flyway) y MongoDB (ping). Estado UP o DEGRADADO.
  - **Seguridad:** `GET /api/admin/security`: usuarios activos y deshabilitados, cuentas bloqueadas, eventos de las
    últimas 24 h por tipo y últimos rechazos, con enlace a la bitácora filtrada.
  - **Bitácoras:** accesos directos a las tres bitácoras.
  - **Colas:** se muestra "no aplica": los servicios se comunican por REST sincrónico y comparten la base; no hay
    mensajería asíncrona en la arquitectura.
- Los errores de los componentes se informan por tipo, nunca con cadenas de conexión ni credenciales.
- **Prueba:** `SeguridadResumenServiceTest`; la salud se verifica en el smoke test del deploy (CP-B6-14).

## D-33 · Baja lógica de organizaciones y sedes (Bloque 6)

- **Decisión:** organizaciones y sedes se modifican (`PUT`) y se habilitan o deshabilitan (`PATCH .../status`); no se
  borran. Cada cambio queda en la bitácora de transacciones (MODIFICACION o CAMBIO_ESTADO, con valor anterior y
  nuevo).
- **Organización deshabilitada:** sus usuarios no ingresan ni operan (D-31) y no se le agregan sedes. Sus datos,
  auditorías y reportes se conservan.
- **Sede deshabilitada:** no se le asignan dispositivos nuevos (un dispositivo que ya estaba en esa sede conserva la
  asignación y su historial).
- Identificador y razón social siguen siendo únicos; el nombre de sede, único dentro de la organización (409).
- **Prueba:** `OrganizacionClienteServiceTest`.

## D-34 · Documento del reporte y calendario de la consola (Bloque 6)

- **Reporte:** la vista y el PDF usan un formato de documento: encabezado con el logo y el nombre AuditNet, número de
  reporte, tabla de datos de emisión y seis apartados numerados (resumen ejecutivo redactado a partir de los números,
  alcance y criterios, distribución de hallazgos, auditorías incluidas, detalle de hallazgos y observaciones). El PDF
  lleva en el pie "AuditNet · REP-NNN" y "Página X de Y" (Chrome y Edge). Sigue siendo la impresión del navegador:
  no se agregan librerías al servidor (D-27).
- **Calendario:** los campos de fecha (reportes, bitácoras, historial y hallazgos) usan un calendario propio con los
  colores de la consola, en lugar del selector nativo del navegador. Se maneja con mouse y teclado y no cambia el
  formato de los valores que leen las pantallas.

## D-35 · Repositorio de archivos en Azure Blob Storage (Bloque 7)

- **Decisión:** el "repositorio seguro de archivos" del diagrama de despliegue (10.5.9) es un contenedor **privado**
  de Azure Blob Storage (`stauditnetlcaruso` / `auditnet-archivos`). audit-core-service guarda ahí el archivo original
  de cada configuración importada, el CSV de cada reporte y los respaldos de la base (D-36). PostgreSQL sigue teniendo
  los datos que se consultan; el repositorio conserva el archivo como evidencia.
- **Catálogo e integridad (RNF-012):** tabla `archivos_repositorio` (migración audit-core V7, solo inserción) con la
  ruta, el tamaño y el SHA-256 de cada archivo. Al descargar se recalcula el hash: si no coincide, no se entrega (409)
  y queda en la bitácora de excepciones. Los archivos nunca se reemplazan (la subida usa `If-None-Match: *`) y su nombre
  lleva el comienzo del hash (`CFG-001-v1-3fa2b9c41d07.cfg`), así no chocan aunque un id se repita después de
  restaurar un respaldo.
- **Consistencia:** el archivo se sube en la misma operación que la importación o el reporte; si el repositorio no
  responde, la operación se revierte (503) para no dejar evidencia sin archivo. Las configuraciones y reportes
  anteriores al Bloque 7 se suben desde el panel ("Subir pendientes", bitácora ARCHIVO/ALTA).
- **Implementación:** cliente REST propio con firma *Shared Key* (`BlobStorageAdapter`), sin el SDK de Azure: son
  cuatro operaciones y se evita sumar dependencias pesadas. En local se usa **Azurite**, el emulador oficial, en el
  docker compose. Sin configuración (pruebas), los archivos quedan pendientes y la salud lo informa. El catálogo se
  registra en transacciones cortas: las subidas largas (respaldos, pendientes) quedan fuera de la transacción.
- **Seguridad:** sin acceso anónimo, solo HTTPS con TLS 1.2, clave de la cuenta como secreto de Container Apps,
  papelera de 7 días. **Limitación:** la autenticación es con la clave de la cuenta; una identidad administrada
  sería más fina pero exige asignar roles RBAC en la suscripción de estudiante.
- **Alternativas descartadas:** mantener todo en PostgreSQL (se aparta del diagrama de despliegue) y GridFS en
  Cosmos DB (consume RU del plan gratuito y no es el uso habitual).
- **Impacto financiero:** la cuenta de Azure incluye 5 GB de Blob Storage LRS (nivel Hot) los primeros 12 meses; el
  volumen del prototipo (configuraciones de texto, CSV y respaldos de pocos MB) queda muy por debajo. Verificar el
  consumo en *Cost Management* después del primer mes.
- **Prueba:** `RepositorioArchivosServiceTest`, `FirmaSharedKeyTest`, casos en `ConfiguracionServiceTest` y
  `ReporteServiceTest`; el adaptador se probó contra Azurite (subida, descarga, no reemplazo, clave incorrecta).

## D-36 · Respaldos lógicos con restauración probada (Bloque 7)

- **Decisión:** el administrador genera desde el panel un respaldo de la base: datos de todas las tablas en CSV
  (COPY dentro de una transacción de solo lectura REPEATABLE READ, consistente aunque se esté usando el sistema), la
  versión del esquema de cada servicio (Flyway), un manifiesto con el SHA-256 de cada archivo y el guion
  `restaurar.sql`. Se guarda en el repositorio de archivos y se registra en la bitácora de transacciones
  (RESPALDO/EXPORTACION, operación nueva en ambos servicios).
- **Restauración:** `restaurar.sql` verifica que la base tenga la misma versión de esquema, desactiva los triggers de
  solo inserción solo mientras carga, reemplaza los datos en orden de claves foráneas, ajusta las secuencias y
  vuelve a activar los triggers, todo en una transacción. Procedimiento en `docs/database/RESPALDOS.md`.
- **Estructura + datos a través del tiempo:** la estructura de cada versión está en las migraciones (Git + historial
  de Flyway, D-05) y los datos en los respaldos, que indican a qué versión corresponden.
- **Complementa** los respaldos automáticos de Azure (PostgreSQL 7 días, Cosmos DB cada 4 horas); no los reemplaza.
- **Límite:** 100 MB por respaldo (el prototipo está muy por debajo); para bases más grandes se usaría pg_dump o la
  restauración a un punto en el tiempo de Azure.
- **Prueba:** `RespaldoServiceTest`, `GuionRestauracionTest`; exportación y restauración reales sobre PostgreSQL 16
  (20 tablas idénticas, secuencias y triggers correctos, rechazo con versión distinta).

## Pendientes detectados (para migraciones futuras)

- `auditorias_configuracion.estado` solo admite `FINALIZADA` (CHECK de Hibernate). Alcanza mientras la auditoría sea
  sincrónica; se amplía si se agregan auditorías programadas o en curso. Los estados de hallazgo se resolvieron en el
  Bloque 4 (D-20) y los tipos de regla en el Bloque 3 (D-18).
- La PK de `tipos_dispositivo` se llama `id` (el resto usa `id_<entidad>`). **Propuesta:** mantenerla; renombrarla
  no aporta funcionalidad y obliga a recrear tres claves foráneas. Se documenta como excepción en el DER.
- Desactivar la única baseline activa de un alcance está permitido (con confirmación en la interfaz): las auditorías
  de ese tipo de dispositivo quedan sin baseline aplicable hasta activar otra.
- "Una baseline activa por alcance" se controla en el servicio, no con una restricción de la base: dos solicitudes
  exactamente simultáneas podrían dejar dos activas (el motor usa la de versión más alta). Se acepta por la escala
  del prototipo. Las violaciones de unicidad por concurrencia (identificador, nombre y versión) responden 409.
- Las consultas de hallazgos y del historial de auditorías no están paginadas (devuelven todo lo que cumple los
  filtros). Alcanza para el volumen del prototipo; si crece, agregar `page`/`size` en Bloque 8.
- La regla "siempre queda un administrador activo" se controla en el servicio: dos administradores que se deshabilitan
  mutuamente en el mismo instante podrían dejar cero. Se acepta por la escala del prototipo (D-29).
- Si se audita varias veces un equipo sin corregir, cada auditoría conserva su hallazgo (evidencia) y todos cuentan
  como pendientes hasta que la regla vuelve a cumplirse (D-26). Las alertas no se duplican.
