## Audits

`RUN` menu. The imported **configurations** are at the top and the **audits run** below. On the right, the context of
whatever you choose.

### Import a configuration

1. Click **+ load configuration**.
2. Choose the **device**.
3. Paste the **running-config** or upload a `.txt` or `.cfg` file (the content appears in the box).
4. Click **Import configuration**.

![Loading a configuration](configuracion-carga.png)

Each import is a **new version** of the device (v1, v2, …). The original is kept exactly as received and a normalized
copy is created for comparison. The original file stays in the file repository with its SHA-256 hash: it can be
downloaded from the configuration context and AuditNet verifies that it has not changed.

### Run an audit

1. Choose the configuration in the list. The context shows the **applicable baseline** and how many active rules it
   has.
2. Click **run audit**.

If no applicable baseline appears, there is no active baseline for that device type in the organization.

### Read the result

![Result of an audit with the evaluation of each rule](auditoria-resultado.png)

- **rules evaluated / rules met**, **findings** and **highest severity**.
- **Rule evaluation:** each rule with PASSED or FINDING and its evidence (the line found or what is missing).
- **See findings →** opens the findings of this audit; **See in history (trend) →** compares it with the previous one.

What else happens when auditing:

- Each rule that is not met generates a **finding**.
- Findings of **high or critical** severity generate a **critical alert** (only one per device and rule) and an email
  to those who have that notice turned on.
- Pending findings from previous audits of the same device whose rule is now met change to **resolved** on their own
  and their alert is closed.
