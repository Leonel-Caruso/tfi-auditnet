## Continuous integration and deployment on Azure

### GitHub Actions

`.github/workflows/imagenes.yml` runs on every push to `entrega-2` or `main` that touches code, tests or the
workflow:

| Job | What it does |
|---|---|
| Tests | `mvn test` of both services |
| Frontend | `npm ci`, `npm run check` and build |
| Screen tests | Playwright against the compiled site, and the demo data tests |
| Images | Publishes the 3 images to GitHub Container Registry with the `sha-<commit>` and branch tags |
| Deploy to Azure | Only from `entrega-2`: waits for the **approval** of the `azure` environment, signs in to Azure with OIDC (no passwords), updates the 3 Container Apps to `sha-<commit>` and runs a smoke test |

To approve: *Actions → the run → Review deployments → azure → Approve and deploy*.

### Azure resources

| Resource | Name | Purpose |
|---|---|---|
| Resource group | `rg-auditnet` (Brazil South) | Everything for the platform |
| PostgreSQL Flexible Server | `psql-auditnet-lcaruso`, database `tfi_auditnet` | Data and logs (B1ms, 7-day backups) |
| Container Apps | `ca-auditnet-web`, `ca-auditnet-management`, `ca-auditnet-core` in `cae-auditnet` | The console with public ingress and the two services with internal ingress |
| Cosmos DB for MongoDB | `cosmos-auditnet-lcaruso` | Exception log (free tier) |
| Storage | `stauditnetlcaruso`, container `auditnet-archivos` | File repository |
| Communication Services | Azure managed domain | Email (5 per minute and 10 per hour) |
| Logic App | daily review at 6:00 | Periodic review |

### Create everything from scratch

In **Azure Cloud Shell (Bash)**, with the repository cloned:

```bash
git clone -b entrega-2 https://github.com/Leonel-Caruso/tfi-auditnet.git
cd tfi-auditnet/infra/azure
bash 01-crear-base.sh            # group, PostgreSQL, database and Container Apps environment
bash 02-crear-apps.sh            # the 3 Container Apps from the published images
bash 04-crear-cosmos.sh          # exception log
bash 05-crear-storage.sh         # file repository
bash 06-configurar-github-oidc.sh  # deployment from GitHub without passwords
bash 08-crear-email.sh           # email
bash 09-crear-revision.sh        # periodic review
```

The scripts ask for the passwords when they run and store them as Container Apps *secrets*; there are no secrets in
the repository. They can be run again. `variables.sh` has the resource names and registers the required providers.

### Deploy a version by hand

```bash
cd ~/tfi-auditnet/infra/azure && bash 03-actualizar-imagenes.sh sha-<commit>
```

Each service applies its migrations on startup (management first). The administration panel shows the deployed tag.

### Deployment security

- HTTPS required with HSTS; nginx adds `Content-Security-Policy`, `Permissions-Policy`, `X-Frame-Options` and
  `Referrer-Policy`.
- The services have no public ingress: they can only be reached through nginx. `/internal` is not published, except
  the periodic review, which requires a 256-bit secret.
- PostgreSQL accepts connections from Azure services (including Cloud Shell). To connect from a PC, a temporary
  firewall rule with its IP is needed.
- GitHub Actions can only update the 3 apps, and every deployment requires manual approval.
