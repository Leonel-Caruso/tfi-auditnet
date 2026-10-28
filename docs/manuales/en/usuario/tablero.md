## Dashboard and client status

### Dashboard

It is the first screen after signing in. It summarizes the status of your organization (the system administrator
chooses the organization at the top right or sees all of them together).

- **Overall risk level:** high, medium or low, with the reason.
  - **High:** there are pending critical findings or current alerts on devices of high or critical criticality.
  - **Medium:** there are pending high-severity findings on devices of medium or low criticality.
  - **Low:** only medium or low severity findings remain pending, or none.
- **Indicators:** audited devices, current compliance, pending findings, critical and high, current alerts, audits in
  the last 30 days, overdue findings and outdated configurations. **Each indicator opens the detail** (for example,
  *overdue findings* opens Findings filtered by overdue).
- **Pending findings by severity** and **Deviations per week** (detected and closed in the last 8 weeks). Hover over
  a bar to see the numbers, or open *show table*.
- **Current alerts**, **Recent audits** and **Highest-risk devices**, with links to each one.

### Client status

From **Status by site →** or the `CLI` menu. It shows the risk of each site, pending findings by severity, recent
audits, devices sorted by risk level and the organization's available reports.

![Client status: overall risk and the organization's indicators](estado-cliente.png)

> **Current compliance** is the percentage of rules met in the last audit of each device. An **overdue finding** is a
> pending one that exceeded the deadline for its severity (by default: critical 7 days, high 15, medium 30 and low 60;
> each organization can adjust them).
