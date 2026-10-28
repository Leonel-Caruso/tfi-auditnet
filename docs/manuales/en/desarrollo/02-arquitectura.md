## Architecture

```text
Browser ──HTTPS──► web (nginx: Astro site + API gateway)
                     ├─ /api/auth, users, roles, client-organizations, sites,
                     │  plans, admin ──────────────────────────► management-service
                     └─ the rest of /api ──────────────────────► audit-core-service

management-service ──┬─► PostgreSQL (data, logs, email outbox)
audit-core-service ──┤
                     └─► MongoDB / Cosmos DB (exception log)
audit-core-service ────► Blob Storage (configurations, reports and backups)
management-service ────► Azure Communication Services (email; Mailpit locally)
Logic App (6:00) ──────► POST /internal/review/run (periodic review)
```

### Components

| Component | Technology | Responsibility |
|---|---|---|
| web | nginx + Astro 5 (static site) | Serves the console and routes `/api/...` to each service on the same origin: the services are not exposed and CORS is not needed. Publishes the content security policy. |
| management-service (port 8081) | Java 21, Quarkus 3 | Sign-in and JWT, users and profiles, organizations and sites, plans, email, system log. |
| audit-core-service (port 8082) | Java 21, Quarkus 3 | Devices, configurations, baselines and rules, audit engine, findings, alerts, dashboard, reports, periodic review, files and backups. |
| PostgreSQL 17 | Flyway per service | One shared database; each service migrates its tables with its own history table. |
| MongoDB / Cosmos DB | MongoDB API 4.2 | Exception log (the NoSQL entity). |
| Blob Storage / Azurite | private container | Files with their SHA-256. |

### Key decisions

- **Two services and one database.** They are split by responsibility (access and administration on one side,
  auditing on the other) and share PostgreSQL so that scope queries (organization, plan) stay simple. audit-core reads
  the plan with a native query; that is why management migrates first.
- **Security.** Sign-in returns a signed JWT (HS256) with the user, their organization and their profiles. Each
  endpoint declares the allowed profiles with `@RolesAllowed` and each service enforces the organization scope.
  Changing the profile, the organization or the password invalidates open sessions.
- **Three logs.** System (access and security) and transactions (previous and new value of each business operation)
  in PostgreSQL; exceptions in MongoDB with a configurable level. A tracking code (`X-Request-ID`, set by nginx) links
  the same operation across the three.
- **Tamper-proof evidence.** Configurations, audits, finding tracking and reports are not modified: there are
  insert-only triggers and reports are a snapshot.
- **Email with an outbox.** Notices are saved in `correos_salientes` within the same transaction as the event that
  triggers them and are dispatched afterwards, with retries. This way a provider failure does not lose notices.
- **Multi-language.** The console translates in the browser with dictionaries; error messages are translated with a
  catalog according to `Accept-Language`; emails use the recipient's language.
