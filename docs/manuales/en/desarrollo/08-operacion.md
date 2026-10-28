## Operation

### Demo mode and costs

Container Apps scale to zero when there is no traffic: the first request takes a few seconds to wake them up.
PostgreSQL can be left stopped to save credit. Before a presentation:

```bash
cd ~/tfi-auditnet/infra/azure
bash 07-modo-demo.sh on       # 1 minimum replica and PostgreSQL started
bash 07-modo-demo.sh estado
bash 07-modo-demo.sh off      # when done: scales to zero again
```

`on` mode consumes quota even without traffic. Costs are monitored in the portal's *Cost Management*.

### Backups

From the **Administration panel → Create backup**: a ZIP with the data of all tables, the schema version and the
restore script, stored in the file repository. Azure also keeps automatic PostgreSQL backups for 7 days. How to
restore: `docs/database/RESPALDOS.md`.

### Periodic review

A Logic App calls `POST /internal/review/run` every day at 6:00 (Argentina time). For each active organization: it
re-audits the devices whose last audit is past the deadline, notifies overdue findings and devices without a new
configuration, and the first review of each month sends the summary of the previous month. It can be started by hand
from the administration panel. If PostgreSQL was stopped, that day's run fails and the next one catches up.

### Email

Notices go through the outbox (`correos_salientes`); the administration panel shows pending, sent and failed emails,
and **Send pending** forces the sending. The Azure managed domain allows few emails per hour: with many new users in a
row, emails go out gradually. Locally they are read in Mailpit (`http://localhost:8025`).

### Diagnosis

1. **Administration panel:** status of each service and its components.
2. **Logs:** find the tracking code of the operation and filter the three logs with it.
3. **App logs:** `az containerapp logs show -g rg-auditnet -n ca-auditnet-management --tail 100`
   (or `ca-auditnet-core`, `ca-auditnet-web`).

### Demo data on Azure

From Cloud Shell (the directory is not kept between sessions: clone or update the repository every time):

```bash
cd ~ && git clone -b main https://github.com/Leonel-Caruso/tfi-auditnet.git; cd ~/tfi-auditnet && git pull
SITIO=https://$(az containerapp show -g rg-auditnet -n ca-auditnet-web --query properties.configuration.ingress.fqdn -o tsv)
python3 scripts/datos-demo/cargar.py --url $SITIO --admin <admin_user> --correo <mailbox>@gmail.com
export PGPASSWORD=$(az containerapp secret show -g rg-auditnet -n ca-auditnet-management --secret-name db-password --query value -o tsv)
psql "host=psql-auditnet-lcaruso.postgres.database.azure.com dbname=tfi_auditnet user=auditnetadmin sslmode=require" -f scripts/datos-demo/historia.sql
unset PGPASSWORD
python3 scripts/datos-demo/cargar.py --url $SITIO --admin <admin_user> --correo <mailbox>@gmail.com --reportes
```
