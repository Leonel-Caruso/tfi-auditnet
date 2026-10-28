## Critical alerts

`ALR` menu. Each finding of **high or critical** severity generates an alert (at most one current alert per device and
rule). Their purpose is to get someone to take charge quickly.

![Prioritized alerts with the chosen alert](alertas.png)

Filter by status (current ones by default), severity, device criticality, device and site. The indicators show the
current, unattended, assigned and critical alerts.

| Status | Meaning |
|---|---|
| active | nobody has taken it yet |
| in progress | it was assigned to a technical profile |
| closed | its finding was resolved or the risk was accepted |

### Assign or return an alert

![Details of the alert with the finding that triggered it](alerta-derivar.png)

1. Choose the alert: you see the finding that triggered it, the device and its evidence.
2. In **action** choose:
   - **assign to Network analyst** or **assign to Technical auditor**: it changes to *in progress*; the comment is
     optional (what needs to be checked);
   - **return to active (unassigned):** the reason is required.
3. Click **Update alert**.

An alert is not closed by hand: it closes on its own when its finding is resolved or the risk is accepted. To
reactivate it, reopen the finding.
