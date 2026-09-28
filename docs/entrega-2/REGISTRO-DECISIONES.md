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

## Pendientes detectados (para migraciones futuras)

- Los CHECK generados por Hibernate limitan los estados (`hallazgos_auditoria.estado` solo admite `ABIERTO`;
  `auditorias_configuracion.estado` solo admite `FINALIZADA`; los tipos de regla solo admiten
  `DEBE_CONTENER`/`NO_DEBE_CONTENER`). Se amplían con migraciones en los Bloques 3 y 4.
- `baselines_configuracion` tiene la restricción única `(id_organizacion, nombre)`, que impide versionar. Se resuelve en el Bloque 3.
- `dispositivos_red.identificador` es único a nivel global, no por organización. Decisión pendiente en el Bloque 3.
- La PK de `tipos_dispositivo` se llama `id` (el resto usa `id_<entidad>`). Se evalúa en el Bloque 3.
