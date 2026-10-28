## Administration panel

`ADM` menu. Platform status, email, periodic review, files, backups and account security.

![Administration panel](administracion.png)

| Panel | What it shows or allows |
|---|---|
| Platform status | whether the services respond, their version, memory and components (PostgreSQL, MongoDB, storage). **Refresh** checks again. |
| Plans | how many plan change requests are waiting to be resolved, with a link to *Plans and requests*. |
| Email | the outbox: pending, sent and failed. Notices are sent automatically and failed ones are retried; **Send pending** forces the sending. |
| Periodic review | the last run (re-audits, overdue findings, outdated configurations, emails). It runs on its own every day at 6:00; **Run review now** starts it immediately. |
| File repository | imported configurations and reports with their SHA-256 hash. **Upload pending** uploads those that do not have their file yet. |
| Backups | **Create backup** creates a ZIP with the data of all tables and the restore script. It contains personal data: download it only to a safe place. |
| Security | active users, locked accounts, sign-ins and rejections in the last 24 hours, with a link to the system log. |
