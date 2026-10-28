## Audit reports

`REP` menu. A report gathers audits with their executive summary, findings, severity, impact and evidence. It is saved
as a **snapshot**: it reflects the status of the findings at the time it was issued and does not change even if the
data changes later.

![Criteria for a new report and generated reports](reportes.png)

### Generate a report

1. In **Criteria** enter the date range (**from** and **to**) or the numbers of **specific audits** separated by
   commas (for example `12, 15`; in that case the dates are optional).
2. Optionally, narrow it down by **device**, finding **severity** and **status**. Unchecked = all.
3. Click **Generate report**.

The system administrator also chooses the organization (or all of them). A report includes up to 500 audits: if the
criteria cover more, narrow the dates. The title is built in the language of the person who generates it.

### View and download

![Document view of the report](reporte-documento.png)

When you choose a report, the document is shown: general data, executive summary with compliance, scope and criteria,
distribution of findings, included audits, details of each finding and notes.

- **Download PDF:** prints the document on A4 with page numbers. It comes out in the console language.
- **Download CSV:** for Excel, with one row per finding. In Spanish and Portuguese it uses `;` as separator and in
  English `,`.
