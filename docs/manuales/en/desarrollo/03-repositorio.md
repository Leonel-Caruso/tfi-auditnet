## Repository and conventions

```text
backend/management-service/   Quarkus: access, users, organizations, plans, email
backend/audit-core-service/   Quarkus: inventory, baselines, auditing, findings, reports
frontend/web-app/             Astro: pages, scripts, dictionaries, styles and nginx
docker/                       PostgreSQL, MongoDB, Azurite and Mailpit for local development
infra/azure/                  Azure creation and operation scripts (01 to 09)
scripts/                      load-env.ps1 and datos-demo/ (demo data load)
tests/pantallas/              Screen tests (Playwright and mock API)
tests/datos_demo/             Demo data tests
tests/carga/                  Load test with k6
docs/                         Decisions, migrations, deployment and guides
.github/workflows/            CI, images and deployment
```

### Layers of each service

```text
ar/edu/uai/tfi/<service>/
  api/rest/         JAX-RS resources and DTOs: input validation, @RolesAllowed and scope
  application/      use cases and ports (application/port), no infrastructure
  domain/           model (records and enums) and repositories as interfaces
  infrastructure/   adapters: Panache, security, email, logs, files, i18n
```

Dependencies point inwards: `api → application → domain`, and `infrastructure` implements the domain and application
ports. That is why the application services are tested with in-memory implementations (`src/test/.../support`).

### Conventions

- **Spanish names** in the domain, the services and the texts; API routes in English (`/api/devices`,
  `/api/findings`).
- **Simple, defensible code:** no long comments; only those that explain a why.
- **Flyway migrations** in `src/main/resources/db/migration` of each service: `V<n>__<description>.sql`. A migration
  that has already been applied is never modified; changes go in a new one. History tables:
  `flyway_historial_management` and `flyway_historial_audit_core`.
- **Decisions** in `REGISTRO-DECISIONES.md` with a `D-xx` number: what was decided, why, alternatives and test.
- **Interface texts** are never written in the HTML or the scripts: always from the dictionaries (see *Extending the
  platform*).
- **No secrets in Git:** `.env` is ignored; `.env.example` lists the variables without values.
