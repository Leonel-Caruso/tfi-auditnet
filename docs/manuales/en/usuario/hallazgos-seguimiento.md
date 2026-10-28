## Findings

`LOG` menu. A **finding** is a rule that was not met in an audit, with its evidence. The list is **prioritized**:
pending findings first, from highest to lowest severity, taking the device criticality into account.

![Prioritized findings with the deadline of each one](hallazgos.png)

### Search

At the top there are filters by status (including *overdue*), severity, device, baseline and detection date, and a
search by rule, device or evidence. The indicators show pending, critical and high, in review and closed findings.
The status column shows the **deadline**: *N days left*, *due today* or *overdue by N days*.

### Details and tracking

When you choose a finding you see the rule, the condition, the device, the baseline, the audit that detected it, the
deadline, the pattern searched, the **evidence** and the **tracking** (each status change with user, date and
comment).

![Details of a finding with the tracking form](hallazgo-seguimiento.png)

**Change the status**

1. Choose the **new status**.
2. Write a **comment**: it is required except when moving to *in review*.
3. Click **Update status**.

| Status | When to use it | Can move to |
|---|---|---|
| open | just detected or reopened | in review, resolved, risk accepted |
| in review | someone is working on the fix | open, resolved, risk accepted |
| resolved | it was fixed on the device | open (reopen) |
| risk accepted | it is decided not to fix it, with a justification (exception or false positive) | open (reopen) |

> The usual path is to fix the device, import the new configuration and audit: if the rule is now met, the finding
> changes to **resolved** on its own with the SISTEMA user and a comment that names the audit. Closing a finding
> (resolved or risk accepted) closes its alert; reopening it makes the alert current again.
