## Baselines and rules

A **baseline** is the expected configuration for a device type in an organization. Its **rules** are the controls the
engine checks in every audit. There is only one active baseline per device type and organization: it is the one used
when auditing. The technical auditor creates and versions them.

### Baselines

`BL` menu. When you choose one you see its organization, type, version, number of rules and audits, and its versions.

![Baselines with the details of the current version](baselines.png)

### Rules

`RUL` menu. Each rule has a code, a condition, a severity, the impact and the recommendation to fix it.

![Control rules with the details of a rule](reglas.png)

| Condition | Passes when |
|---|---|
| must contain | the pattern appears in the configuration |
| must not contain | the pattern does not appear |
| expected value | a line starting with the parameter has exactly that value |
