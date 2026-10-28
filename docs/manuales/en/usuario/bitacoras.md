## Logs

`BIT` menu. Three read-only records, each in its own tab:

| Log | What it records | Where it is stored |
|---|---|---|
| System audit | access and security: sign-ins, rejections, locks, account changes | PostgreSQL |
| Transactions | business operations with the previous and new values (creations, changes, audits, plans) | PostgreSQL |
| Exceptions | unexpected service errors, with their trace | MongoDB |

![System audit log](bitacoras.png)

Filter by event, result, user, entity or service and by dates, and click **Search**. When you choose a record you see
all its fields. The **tracking code** identifies the same operation in the three logs: from the details, *Filter by
this code in the 3 logs* shows everything that happened in that operation.
