## Operación

### Modo demo y costos

Las Container Apps escalan a cero cuando no hay tráfico: la primera solicitud tarda unos segundos en despertarlas.
PostgreSQL puede quedar detenido para ahorrar crédito. Antes de una presentación:

```bash
cd ~/tfi-auditnet/infra/azure
bash 07-modo-demo.sh on       # 1 réplica mínima y PostgreSQL encendido
bash 07-modo-demo.sh estado
bash 07-modo-demo.sh off      # al terminar: vuelve a escalar a cero
```

El modo `on` consume la cuota aunque no haya tráfico. Los costos se controlan en *Cost Management* del portal.

### Respaldos

Desde el **Panel de administración → Generar respaldo**: un ZIP con los datos de todas las tablas, la versión del
esquema y el guion de restauración, guardado en el repositorio de archivos. Azure conserva además copias automáticas
de PostgreSQL por 7 días. Cómo restaurar: `docs/database/RESPALDOS.md`.

### Revisión periódica

Una Logic App llama todos los días a las 6:00 (hora de Argentina) a `POST /internal/review/run`. Por cada organización
activa: re-audita los equipos cuya última auditoría superó el plazo, avisa los hallazgos vencidos y los equipos sin
configuración nueva, y la primera revisión de cada mes envía el resumen del mes anterior. Se puede lanzar a mano desde el panel de
administración. Si PostgreSQL estaba detenido, la ejecución de ese día falla y la siguiente se pone al día.

### Correo

Los avisos pasan por la bandeja de salida (`correos_salientes`); el panel de administración muestra los pendientes,
enviados y fallidos, y **Enviar pendientes** fuerza el envío. El dominio administrado de Azure admite pocos envíos por
hora: con muchas altas seguidas, los correos salen de a poco. En local se leen en Mailpit (`http://localhost:8025`).

### Diagnóstico

1. **Panel de administración:** estado de cada servicio y de sus componentes.
2. **Bitácoras:** buscar el código de seguimiento de la operación y filtrar las tres bitácoras con él.
3. **Registros de las apps:** `az containerapp logs show -g rg-auditnet -n ca-auditnet-management --tail 100`
   (o `ca-auditnet-core`, `ca-auditnet-web`).

### Datos de demostración en Azure

Desde Cloud Shell (el directorio no se conserva entre sesiones: clonar o actualizar el repositorio cada vez):

```bash
cd ~ && git clone -b main https://github.com/Leonel-Caruso/tfi-auditnet.git; cd ~/tfi-auditnet && git pull
SITIO=https://$(az containerapp show -g rg-auditnet -n ca-auditnet-web --query properties.configuration.ingress.fqdn -o tsv)
python3 scripts/datos-demo/cargar.py --url $SITIO --admin <usuario_admin> --correo <casilla>@gmail.com
export PGPASSWORD=$(az containerapp secret show -g rg-auditnet -n ca-auditnet-management --secret-name db-password --query value -o tsv)
psql "host=psql-auditnet-lcaruso.postgres.database.azure.com dbname=tfi_auditnet user=auditnetadmin sslmode=require" -f scripts/datos-demo/historia.sql
unset PGPASSWORD
python3 scripts/datos-demo/cargar.py --url $SITIO --admin <usuario_admin> --correo <casilla>@gmail.com --reportes
```
