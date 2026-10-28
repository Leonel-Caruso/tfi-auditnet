## Findings

`LOG` menu. A **finding** is a rule that was not met in an audit, with its evidence. The list is **prioritized**:
pending findings first, from highest to lowest severity, taking the device criticality into account.

![Prioritized findings with the deadline of each one](hallazgos.png)

Use the filters by status (including *overdue*), severity, device, baseline and date, or the search by rule, device or
evidence. The status column shows the deadline: *N days left*, *due today* or *overdue by N days*.

When you choose a finding you see the rule, the device, the audit that detected it, the evidence, the recommendation
and the **tracking** (each status change with user, date and comment). Your profile can view it; the tracking is
updated by the network analyst and the technical auditor.

| Status | Meaning |
|---|---|
| open | detected, nobody has taken it yet |
| in review | someone is working on the fix |
| resolved | fixed; usually confirmed by a new audit |
| risk accepted | it was decided not to fix it, with a justification |
