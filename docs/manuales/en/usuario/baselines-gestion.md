## Baselines and rules

A **baseline** is the expected configuration for a device type in an organization. Its **rules** are the controls the
engine checks in every audit. There can be only **one active baseline per device type and organization**: it is the
one used when auditing.

### Baselines

`BL` menu. When you choose a baseline you see its organization, type, version, number of rules and audits, and its
previous versions.

![Baselines with the details of the current version](baselines.png)

**Create a baseline**

1. Click **+ new baseline**.
2. Fill in **name**, **description**, **device type** and **organization**.
3. Click **Create baseline**. It is created as version 1, active.

**Changes to a baseline**

| Action | What it does |
|---|---|
| **Edit** | Changes the description. It does not change the criteria, so no new version is created. |
| **New version** | Copies the active rules into a new version, which becomes active; the previous one becomes inactive and keeps its audits. |
| **Deactivate / Activate** | If you deactivate the only active baseline of a type, new audits of that type have no applicable baseline. |

> **Frozen rules:** once a version has been used in audits, its rules are not changed, to preserve the evidence. To
> change them, create a new version.

### Rules

`RUL` menu. It lists the controls with their baseline, condition, severity and status. When you choose one you see
its pattern, impact and recommendation.

![Control rules with the details of a rule](reglas.png)

**Create a rule**

1. Click **+ new rule**.
2. Choose the **baseline** and fill in **code** (letters, numbers, hyphens, dots or underscores, for example
   `SEC-SSH-01`), **name** and **severity** (low, medium, high or critical).
3. Choose the **condition**:

| Condition | Passes when | Example |
|---|---|---|
| must contain | the pattern appears in the configuration | `service password-encryption` |
| must not contain | the pattern does not appear | `transport input telnet` |
| expected value | a line starting with the parameter has exactly that value | parameter `ip ssh version`, value `2` |

4. Fill in **description**, **impact** (what risk it implies) and **recommendation** (how to fix it).
5. Click **Create rule**.

![New rule](regla-alta.png)

The comparison is case-insensitive and repeated spaces count as one. A baseline cannot have two active rules that
check the same thing, nor one that requires the opposite of another.

**Edit** changes everything except the code and the baseline. **Deactivate** removes the rule from new audits;
previous audits keep their result.
