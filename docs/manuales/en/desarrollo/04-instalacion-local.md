## Local installation

### Requirements

| Tool | Version |
|---|---|
| Docker Desktop | with Docker Compose |
| Java (JDK) | 21 |
| Maven | 3.9 or later |
| Node.js | 24 |
| Python | 3.11 or later (screen tests, demo data and guides) |
| PowerShell | 5 or later (Windows); on Linux or macOS the same steps with `bash` |

### 1. Environment variables

Copy `.env.example` to `.env` and fill in the local values: database, `JWT_SECRET`, the initial administrator
(`BOOTSTRAP_ADMIN_*`) and the initial organization (`BOOTSTRAP_ORG_*`). `.env` is not pushed to Git.

### 2. Databases and support services

```powershell
.\scripts\load-env.ps1
docker compose --env-file .env -f docker/compose.database.yaml up -d
docker ps
```

It starts `tfi-postgres` (5432), `tfi-mongo` (27017), `tfi-azurite` (10000, storage) and `tfi-mailpit` (emails are
read at `http://localhost:8025`).

### 3. Services

In a terminal, **management-service first** (it applies its migrations and creates the profiles, the initial
organization and the initial administrator):

```powershell
.\scripts\load-env.ps1
cd .\backend\management-service
mvn quarkus:dev
```

When `Listening on: http://localhost:8081` appears, in another terminal:

```powershell
.\scripts\load-env.ps1
cd .\backend\audit-core-service
mvn quarkus:dev
```

### 4. Console

```powershell
cd .\frontend\web-app
npm install
npm run dev -- --host localhost
```

Open `http://localhost:4321` and sign in with the administrator from `.env`. In development the browser calls the
services on ports 8081 and 8082 (it does not go through nginx).

### 5. Demo data (optional)

`scripts/datos-demo` loads 6 fictional organizations with sites, users, devices, baselines, rules and about 100
audits, using the API as an administrator would:

```powershell
python scripts\datos-demo\cargar.py --local --admin <admin_user> --correo <mailbox>@gmail.com
docker cp .\scripts\datos-demo\historia.sql tfi-postgres:/tmp/historia.sql
docker exec tfi-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB -f /tmp/historia.sql'
python scripts\datos-demo\cargar.py --local --admin <admin_user> --correo <mailbox>@gmail.com --reportes
```

- `cargar.py` asks for the administrator's password and the one for the new users; it does not store them. User
  emails are aliases of the mailbox (`mailbox+user@domain`) and their audit notices are turned off.
- `historia.sql` spreads the newly loaded audits, with their findings and alerts, over the last 8 weeks.
- `--reportes` generates the reports once the dates are spread.
- It can be run again: whatever already exists is reused.
