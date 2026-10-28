## Introduction

AuditNet is a B2B web platform to audit the configurations of Cisco IOS/IOS-XE network devices. It compares the real
configuration of each device (running-config) with an expected configuration (baseline), detects deviations,
prioritizes them by severity and by device criticality, generates alerts and reports, and keeps the evidence of every
audit. It is multi-organization: each client sees only its own data.

This guide is for whoever has to **install, operate or modify** the platform: developers joining the project and
whoever deploys it on Azure. It covers:

- the architecture and why it is built this way;
- how the repository is organized and which conventions it follows;
- how to run it on a local computer and load demo data;
- how to run the tests;
- how to add an endpoint, a migration, a screen, a translation or an email;
- the integration and deployment pipeline on Azure, and day-to-day operation;
- the real problems that came up during development and how they were solved;
- what each profile can do and where it is enforced in the code.

| Item | Value |
|---|---|
| Repository | `github.com/Leonel-Caruso/tfi-auditnet` |
| Working branch | `entrega-2` (merged into `main` when the delivery closes) |
| Design decisions | `docs/entrega-2/REGISTRO-DECISIONES.md` (D-01 onwards) |
| Migrations | `docs/database/MIGRACIONES.md` |
| Deployment | `docs/deploy/AZURE.md` |

> In the commands, whatever goes between `<` and `>` is replaced by the real value (without the signs). Passwords and
> secrets are never written in the repository or on the command line: the scripts ask for them when they run.
