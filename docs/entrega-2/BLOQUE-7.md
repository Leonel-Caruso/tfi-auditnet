# Bloque 7 — Repositorio de archivos y respaldos

Cubre: diagrama de despliegue 10.5.9 ("las configuraciones importadas y los reportes generados se almacenan en un
repositorio seguro de archivos"), RNF-009 (respaldo), RNF-012 (protección del historial auditado) y el punto
mandatorio "histórico del modelo de datos: recuperar datos y estructura a través del tiempo".
Decisiones: D-35 y D-36 en `REGISTRO-DECISIONES.md`. Migración audit-core V7. Procedimiento: `docs/database/RESPALDOS.md`.

## API nueva (audit-core-service)

| Método y ruta | Perfiles | Qué hace |
|---|---|---|
| `GET /api/configurations/{id}/file` | Todos (no administradores: solo su organización) | Archivo original de la configuración, verificado contra su SHA-256. Encabezados `X-AuditNet-SHA256` y `X-AuditNet-Integridad: VERIFICADO`. 404 si todavía no tiene archivo, 409 si no coincide con el hash |
| `GET /api/system/storage` | Administrador | Estado del repositorio: configurado, contenedor, cantidad y bytes por tipo, pendientes |
| `POST /api/system/storage/sync` | Administrador | Sube los archivos pendientes (hasta 50 de cada tipo por vez). Bitácora: ARCHIVO/ALTA |
| `GET /api/system/backups` | Administrador | Últimos 50 respaldos |
| `POST /api/system/backups` | Administrador | Genera un respaldo (201). Bitácora: RESPALDO/EXPORTACION |
| `GET /api/system/backups/{id}/download` | Administrador | Descarga el ZIP, verificado contra su SHA-256 |

Cambios en APIs existentes:

- `POST /api/devices/{id}/configurations` y `POST /api/reports/audits` también guardan el archivo en el repositorio.
  Si el repositorio no responde: 503 y no se guarda nada (se puede reintentar).
- `GET /api/reports/{id}/download` entrega el CSV guardado (verificado); los reportes anteriores, o si el repositorio no
  responde, se arman de nuevo con los mismos datos (`X-AuditNet-Integridad: SIN_VERIFICAR`).
- `GET /api/system/health` suma el componente "Repositorio de archivos".
- Bitácora de transacciones: entidades nuevas RESPALDO y ARCHIVO, operación nueva EXPORTACION.

## Configuración

| Variable | Dónde | Valor |
|---|---|---|
| `AZURE_STORAGE_CONNECTION_STRING` | audit-core en Azure (secreto `storage-conexion`) | La pone `infra/azure/05-crear-storage.sh` |
| `AZURE_STORAGE_CONTAINER` | audit-core | `auditnet-archivos` (valor por defecto) |
| — | Local (`mvn quarkus:dev`) | Sin variables: usa el Azurite del docker compose (puerto 10000) |

Organización del contenedor:

```text
auditnet-archivos/
├── configuraciones/org-<id>/dispositivo-<id>/CFG-001-v1-<hash>.cfg
├── reportes/org-<id>/REP-001-<hash>.csv    (reportes/todas/ si el administrador abarcó todas las organizaciones)
└── respaldos/AAAA/MM/auditnet-respaldo-AAAAMMDD-HHMMSS.zip
```

## Pantallas

- **Panel de administración (ADM):** "Repositorio de archivos" (estado, cantidad y tamaño por tipo, pendientes y
  botón "Subir pendientes") y "Copias de la base de datos" (generar, listar y descargar). La salud de audit-core
  muestra el repositorio.
- **Auditorías → seleccionar una configuración:** "Descargar archivo original" con el resultado de la verificación
  SHA-256.
- **Bitácoras → Transacciones:** filtros RESPALDO, ARCHIVO y EXPORTACION.
- **Detalles visuales:** ícono de la pestaña con el "A_" naranja (favicon SVG y PNG) y, en el login, el "_" titila y
  cada unos segundos escribe "AuditNet_" y se vuelve a borrar (quieto si el sistema tiene "reducir movimiento").

## Casos de prueba manuales (para la matriz de CP)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B7-01 | Abrir el login y esperar unos segundos | El "_" titila; aparece "AuditNet_", queda un momento y se borra hasta "A_" |
| CP-B7-02 | Mirar la pestaña del navegador | Ícono "A_" naranja sobre fondo oscuro |
| CP-B7-03 | Panel ADM | audit-core muestra "Repositorio de archivos" operativo; la tarjeta del repositorio dice "configurado" |
| CP-B7-04 | Panel ADM con configuraciones anteriores al bloque → "Subir pendientes" | Pendientes en 0; Bitácoras → TRX → ARCHIVO muestra la subida |
| CP-B7-05 | Importar una configuración nueva → Auditorías → seleccionarla → "Descargar archivo original" | Baja el archivo con su contenido exacto e indica "Integridad verificada" con el SHA-256 |
| CP-B7-06 | Comparar ese SHA-256 con el de Bitácoras → TRX → CONFIGURACION/IMPORTACION | Son iguales |
| CP-B7-07 | Generar un reporte → Descargar CSV | Se descarga el CSV guardado en el repositorio |
| CP-B7-08 | Panel ADM → "Generar respaldo" | Aparece en la lista con tablas, filas y versión del esquema; Bitácoras → TRX → RESPALDO/EXPORTACION |
| CP-B7-09 | Descargar el respaldo y abrir el ZIP | Contiene manifiesto.json, LEEME.txt, restaurar.sql, datos/ y esquema/ |
| CP-B7-10 | Restaurar el respaldo en local (RESPALDOS.md) después de cambiar algún dato | Los datos vuelven a como estaban al generar el respaldo |
| CP-B7-11 | Detener Azurite (`docker stop tfi-azurite`) e importar una configuración | Responde que el repositorio no está disponible y no se guarda la configuración; al volver a iniciarlo funciona |
| CP-B7-12 | Como no administrador, desde la consola (F12) pedir `/api/system/backups` | 403 |
| CP-B7-13 | En Azure Portal, abrir la cuenta de almacenamiento → contenedor `auditnet-archivos` | Nivel de acceso "Privado"; están las carpetas configuraciones, reportes y respaldos |

Para CP-B7-12 (F12, con la sesión iniciada):
`fetch('http://localhost:8082/api/system/backups', {headers: {Authorization: 'Bearer ' + sessionStorage.getItem('tfi.auth.token')}}).then(r => r.status)`

## Pendiente de actualizar en la carpeta de tesis

- 10.5.9 Diagrama de despliegue: el repositorio de archivos es Azure Blob Storage (contenedor privado) y además
  guarda los respaldos; agregar Azurite como equivalente local.
- Tabla 46 (diccionario): `archivos_repositorio`.
- RNF-009: mecanismos de respaldo concretos (respaldo lógico desde el panel + automáticos de Azure) y procedimiento
  de restauración probado.
- RNF-012: verificación SHA-256 de los archivos al descargarlos.
- Matriz de Puntos Mandatorios: "histórico del modelo de datos" (Flyway para la estructura + respaldos para los datos).
- Presupuesto (Excel): Blob Storage dentro de los 5 GB gratuitos del primer año (impacto financiero a confirmar
  con el consumo real en Cost Management).
