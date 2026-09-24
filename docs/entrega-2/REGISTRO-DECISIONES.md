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

## Pendientes detectados (para migraciones futuras)

- Los CHECK generados por Hibernate limitan los estados (`hallazgos_auditoria.estado` solo admite `ABIERTO`;
  `auditorias_configuracion.estado` solo admite `FINALIZADA`; los tipos de regla solo admiten
  `DEBE_CONTENER`/`NO_DEBE_CONTENER`). Se amplían con migraciones en los Bloques 3 y 4.
- `baselines_configuracion` tiene la restricción única `(id_organizacion, nombre)`, que impide versionar. Se resuelve en el Bloque 3.
- `dispositivos_red.identificador` es único a nivel global, no por organización. Decisión pendiente en el Bloque 3.
- La PK de `tipos_dispositivo` se llama `id` (el resto usa `id_<entidad>`). Se evalúa en el Bloque 3.
