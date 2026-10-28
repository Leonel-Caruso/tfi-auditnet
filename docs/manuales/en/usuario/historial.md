## Audit history

`HIS` menu. All audits, with the evaluated configuration, the baseline and the rules used. It is read-only.

![History with the details and trend of an audit](historial.png)

### Search

Filter by device, baseline, highest severity, result (compliant or with findings), who ran it and date range, and
click **Search**. Audits run by the periodic review appear as **automatic**.

### Details and trend

When you choose an audit you see the configuration (version and who imported it), the baseline with its version, the
rules applied and the findings generated with their current status. The **trend** compares it with the previous audit
of the same device:

| Category | Meaning |
|---|---|
| new deviations | rules that passed before and now do not |
| fixed | rules that failed and now pass |
| persisting | rules that keep failing |
| new rules / removed rules | changes in the baseline between one audit and the other |

**See only the history of this device** filters the list by that device and **See findings of this audit** opens its
findings.
