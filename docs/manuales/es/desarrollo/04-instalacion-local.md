## Instalación local

### Requisitos

| Herramienta | Versión |
|---|---|
| Docker Desktop | con Docker Compose |
| Java (JDK) | 21 |
| Maven | 3.9 o superior |
| Node.js | 24 |
| Python | 3.11 o superior (pruebas de pantalla, datos de demostración y manuales) |
| PowerShell | 5 o superior (Windows); en Linux o macOS los mismos pasos con `bash` |

### 1. Variables de entorno

Copiá `.env.example` a `.env` y completá los valores locales: base de datos, `JWT_SECRET`, el administrador inicial
(`BOOTSTRAP_ADMIN_*`) y la organización inicial (`BOOTSTRAP_ORG_*`). `.env` no se sube a Git.

### 2. Bases y servicios de apoyo

```powershell
.\scripts\load-env.ps1
docker compose --env-file .env -f docker/compose.database.yaml up -d
docker ps
```

Levanta `tfi-postgres` (5432), `tfi-mongo` (27017), `tfi-azurite` (10000, almacenamiento) y `tfi-mailpit`
(los correos se leen en `http://localhost:8025`).

### 3. Servicios

En una terminal, **primero management-service** (aplica sus migraciones y crea los perfiles, la organización y el
administrador inicial):

```powershell
.\scripts\load-env.ps1
cd .\backend\management-service
mvn quarkus:dev
```

Cuando aparezca `Listening on: http://localhost:8081`, en otra terminal:

```powershell
.\scripts\load-env.ps1
cd .\backend\audit-core-service
mvn quarkus:dev
```

### 4. Consola

```powershell
cd .\frontend\web-app
npm install
npm run dev -- --host localhost
```

Abrí `http://localhost:4321` e ingresá con el administrador del `.env`. En desarrollo el navegador llama a los
servicios en los puertos 8081 y 8082 (no pasa por nginx).

### 5. Datos de demostración (opcional)

`scripts/datos-demo` carga 6 organizaciones ficticias con sedes, usuarios, equipos, baselines, reglas y unas 100
auditorías, usando la API como lo haría un administrador:

```powershell
python scripts\datos-demo\cargar.py --local --admin <usuario_admin> --correo <casilla>@gmail.com
docker cp .\scripts\datos-demo\historia.sql tfi-postgres:/tmp/historia.sql
docker exec tfi-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB -f /tmp/historia.sql'
python scripts\datos-demo\cargar.py --local --admin <usuario_admin> --correo <casilla>@gmail.com --reportes
```

- `cargar.py` pide la contraseña del administrador y la de los usuarios nuevos; no las guarda. Los correos de los
  usuarios son alias de la casilla (`casilla+usuario@dominio`) y sus avisos de auditoría quedan apagados.
- `historia.sql` reparte en las últimas 8 semanas las auditorías recién cargadas con sus hallazgos y alertas.
- `--reportes` genera los reportes una vez repartidas las fechas.
- Se puede volver a ejecutar: lo que ya existe se reutiliza.
