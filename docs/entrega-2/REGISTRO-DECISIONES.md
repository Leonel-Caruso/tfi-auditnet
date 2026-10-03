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
