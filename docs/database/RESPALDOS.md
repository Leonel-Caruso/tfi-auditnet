# Respaldos y restauración de la base de datos

Bloque 7 · D-36. Cubre RNF-009 (respaldo) y el punto mandatorio "histórico del modelo de datos: recuperar datos y
estructura a través del tiempo".

## Qué respalda AuditNet

| Mecanismo | Qué guarda | Dónde | Retención |
|---|---|---|---|
| Respaldo lógico (panel ADM → "Generar respaldo") | Datos de todas las tablas de PostgreSQL (CSV), versión del esquema de cada servicio (Flyway) y el guion `restaurar.sql` | Repositorio de archivos (Blob Storage, contenedor privado, carpeta `respaldos/AAAA/MM/`) con su SHA-256 en el catálogo | No se borran (papelera de 7 días ante un borrado accidental) |
| Respaldo automático de PostgreSQL Flexible Server | Toda la base, restaurable a cualquier momento | Azure | 7 días |
| Respaldo periódico de Cosmos DB | Bitácora de excepciones (MongoDB) | Azure | Cada 4 horas, se conservan las 2 últimas copias (sin costo) |
| Migraciones Flyway en Git | Estructura de cada versión | Repositorio de código | Permanente |

El respaldo lógico **no** incluye la bitácora de excepciones (MongoDB): tiene sus propios respaldos en Cosmos DB.

## Contenido del ZIP

```text
auditnet-respaldo-20261011-193015.zip
├── manifiesto.json          fecha, usuario, versión de la aplicación (sha-...), versión del esquema,
│                            filas y SHA-256 de cada archivo
├── LEEME.txt
├── restaurar.sql            guion de psql
├── datos/<tabla>.csv        una tabla por archivo (CSV con encabezado, UTF-8)
└── esquema/flyway_historial_*.csv   historial de migraciones de cada servicio
```

Contiene datos personales y hashes de contraseñas (BCrypt): se descarga solo a lugares autorizados. Al descargarlo,
el servicio verifica que el archivo coincida con el SHA-256 registrado al generarlo.

## Restaurar

`restaurar.sql` **reemplaza todos los datos** de la base por los del respaldo, en una sola transacción: si algo falla,
la base queda como estaba. Antes de cargar nada verifica que la base tenga la **misma versión de esquema** que el
respaldo (por ejemplo management V4 y audit-core V7); si no, se detiene con un mensaje.

Requisitos:

- `psql` (viene en Azure Cloud Shell y en el contenedor de PostgreSQL local).
- Conectarse con el usuario dueño de las tablas (el mismo que usan los servicios): el guion desactiva los triggers
  de solo inserción de las bitácoras únicamente durante la restauración y los vuelve a activar al final.
- Una base con el esquema en esa versión: la base actual, o una nueva en la que se iniciaron los servicios una vez
  (Flyway crea la estructura; los datos iniciales que cargan los servicios se reemplazan).

### En local (Docker)

PowerShell, en la carpeta donde se descomprimió el respaldo:

```powershell
docker cp . tfi-postgres:/tmp/respaldo
docker exec -it -w /tmp/respaldo tfi-postgres psql -U <USUARIO_DE_LA_BASE> -d tfi_auditnet_e2 -f restaurar.sql
```

### En Azure

1. Detener el uso de la plataforma (es una restauración completa).
2. En Azure Cloud Shell, subir el ZIP (botón "Cargar archivos") y descomprimirlo: `unzip auditnet-respaldo-....zip -d respaldo && cd respaldo`.
3. Habilitar temporalmente el acceso desde Cloud Shell en el firewall del servidor PostgreSQL (Portal → servidor →
   Redes → "Agregar la dirección IP del cliente actual") y quitarlo al terminar.
4. Ejecutar:
   ```bash
   psql "host=psql-auditnet-lcaruso.postgres.database.azure.com dbname=tfi_auditnet user=auditnetadmin sslmode=require" -f restaurar.sql
   ```
   (la contraseña se pide al conectar; no se escribe en la línea de comandos).
5. Verificar en el panel ADM y en las pantallas que los datos sean los esperados.

Después de restaurar, el catálogo vuelve al estado del respaldo: los respaldos y archivos generados después siguen en
el contenedor (Portal → cuenta de almacenamiento → `auditnet-archivos`), pero ya no aparecen en el panel.

Para volver a una versión de esquema anterior (por ejemplo, un respaldo de V6 sobre una base en V7), primero se
despliega la versión de AuditNet que corresponde (`sha-...` del manifiesto) sobre una base nueva y después se restaura.

## Prueba realizada

Base con datos de todas las tablas (textos con comas, comillas, saltos de línea, acentos, NULL y cadenas vacías) →
respaldo → restauración sobre otra base con el mismo esquema y datos distintos: las 20 tablas quedaron idénticas, las
secuencias continúan desde el último id, los triggers de solo inserción volvieron a quedar activos, y con una versión
de esquema distinta el guion se detuvo sin modificar nada.
