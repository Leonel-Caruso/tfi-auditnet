## Your organization and its sites

`ORG` menu. You only see your organization: its sites on the right and its plan and review deadlines below.

![Your organization with its sites, the plan and its usage](organizacion-propia.png)

### Sites

1. Click **+ site**.
2. Enter the **name** (unique within the organization) and the **location**.
3. Click **Register site**.

**Edit** changes that data. **Disable** on a site prevents assigning new devices to it; the devices it already has
keep their history. **Enable** makes it available again.

### Plan and usage

It shows the current plan and how many active devices and users are in use out of the allowed total. **see My plan →**
opens the details and the change request (chapter *My plan*).

### Review deadlines

![Review deadlines of the organization](plazos-revision.png)

| Field | What it defines | Default |
|---|---|---|
| critical / high / medium / low · days | Within how many days a pending finding of that severity becomes overdue, counted from its detection | 7 / 15 / 30 / 60 |
| re-audit every · days | How often the latest configuration of a device is audited again | 30 |
| configuration outdated after · days | When a device without a new configuration counts as outdated | 30 |
| Re-audit automatically | Whether the daily review audits again on its own the devices whose deadline has passed | on |

Severity deadlines must go from lowest to highest (critical ≤ high ≤ medium ≤ low) and each value goes from 1 to 365
days. Click **Save deadlines**: the change immediately updates the due dates of all findings.

> The periodic review runs every day at 6:00 (Argentina time): it re-audits what is due and emails overdue findings
> and outdated devices. AuditNet does not connect to the devices: re-auditing means evaluating the latest uploaded
> configuration against the current baseline.
