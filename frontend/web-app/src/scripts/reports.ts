// Reportes: generación, documento (PDF) y descarga en CSV.
import { auditFetch, authFetch, getStoredUser, hasRole } from '../lib/auth';
import {
  jsonRequest,
  readApiError,
  type AuditReport,
  type NetworkDevice,
  type Organization,
  type ReportSummary,
  type Site,
} from '../lib/api';
import { code, escapeHtml, formatDate } from '../lib/format';
import { t } from '../lib/i18n';
import { printPageStyle, reportDocumentHtml } from '../lib/report-document';
import { loadVisibleSites } from '../lib/sites';
import { scrollToView, show } from '../lib/ui';

const isAdmin = hasRole('ADMINISTRADOR_SISTEMA');
// Generan el administrador, el analista de redes y el auditor técnico; el resto consulta.
const canGenerate = isAdmin || hasRole('ANALISTA_RED') || hasRole('AUDITOR_TECNICO');
const params = new URLSearchParams(window.location.search);

let reports: ReportSummary[] = [];
let devices: NetworkDevice[] = [];
let sites: Site[] = [];
let organizations: Organization[] = [];
let selectedId: number | null = null;

const form = document.querySelector<HTMLFormElement>('#report-form');
const view = document.querySelector<HTMLElement>('#report-view');

const showError = (message: string | null) => show('#report-error', message);
const showSuccess = (message: string) => {
  show('#report-success', message);
  window.setTimeout(() => show('#report-success', null), 5000);
};

const isoDate = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};
/** Fecha del input (día local) → ISO-8601: inicio del día para "desde" y fin del día para "hasta". */
const dayToIso = (value: string, endOfDay: boolean) => {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  const date = endOfDay ? new Date(year, month - 1, day, 23, 59, 59, 999) : new Date(year, month - 1, day, 0, 0, 0, 0);
  return date.toISOString();
};
const orgName = (id: number | null) => id == null
  ? t('tablero.todasOrganizaciones')
  : organizations.find((organization) => organization.id === id)?.razonSocial ?? t('tablero.organizacionNumero', { id });
const siteName = (id: number | null) => id == null
  ? t('tablero.sinSede')
  : sites.find((site) => site.id === id)?.nombre ?? t('tablero.sedeNumero', { id });

const renderList = () => {
  const body = document.querySelector<HTMLTableSectionElement>('#report-list');
  if (!body) return;
  body.innerHTML = reports.map((report) => `
    <tr class="finding-row ${report.id === selectedId ? 'is-selected' : ''}" data-report-id="${report.id}" tabindex="0">
      <td><code class="log-nowrap">${code('REP', report.id)}</code></td>
      <td><strong>${escapeHtml(report.titulo)}</strong>${isAdmin ? `<small>${escapeHtml(orgName(report.organizacionId))}</small>` : ''}</td>
      <td>${report.totalAuditorias}</td>
      <td>${report.totalHallazgos}</td>
      <td><span class="log-nowrap">${escapeHtml(formatDate(report.fechaGeneracion))}</span><small>${escapeHtml(report.generadoPor)}</small></td>
    </tr>`).join('');
  const count = document.querySelector('#report-count');
  if (count) count.textContent = String(reports.length);
  document.querySelector('#report-empty')?.classList.toggle('is-hidden', reports.length > 0);
  body.querySelectorAll<HTMLElement>('[data-report-id]').forEach((row) => {
    const open = () => void openReport(Number(row.dataset.reportId));
    row.addEventListener('click', open);
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        open();
      }
    });
  });
};

const reportContext = {
  orgName,
  siteName,
  deviceName: (id: number) => devices.find((device) => device.id === id)?.identificador ?? `#${id}`,
};

const renderReport = (report: AuditReport) => {
  if (!view) return;
  const rep = code('REP', report.id);
  view.innerHTML = `
    <div class="report-toolbar report-no-print">
      <div>
        <span class="eyebrow">${t('reportes.vistaDocumento')}</span>
        <strong>${rep}</strong>
      </div>
      <div class="report-actions">
        <button id="report-print" type="button" class="button button-primary button-small">${t('reportes.descargarPdf')}</button>
        <button id="report-csv" type="button" class="button button-ghost button-small">${t('reportes.descargarCsv')}</button>
      </div>
    </div>
    ${reportDocumentHtml(report, reportContext)}`;
  view.classList.remove('is-hidden');

  // Pie de página del PDF con el número de reporte (se reemplaza al abrir otro reporte).
  let printStyle = document.querySelector<HTMLStyleElement>('#report-print-style');
  if (!printStyle) {
    printStyle = document.createElement('style');
    printStyle.id = 'report-print-style';
    document.head.append(printStyle);
  }
  printStyle.textContent = printPageStyle(report);

  document.querySelector('#report-print')?.addEventListener('click', () => {
    // El título de la página es el nombre sugerido del PDF al guardarlo.
    const previous = document.title;
    document.title = `AuditNet ${rep} - ${report.fechaGeneracion.slice(0, 10)}`;
    window.addEventListener('afterprint', () => { document.title = previous; }, { once: true });
    window.print();
  });
  document.querySelector('#report-csv')?.addEventListener('click', () => void downloadCsv(report.id));
};

const downloadCsv = async (id: number) => {
  showError(null);
  const response = await auditFetch(`/api/reports/${id}/download`);
  if (!response.ok) return showError(await readApiError(response));
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = `${t('reportes.archivoCsv')}-${code('REP', id)}.csv`;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
};

const openReport = async (id: number) => {
  selectedId = id;
  renderList();
  const response = await auditFetch(`/api/reports/${id}`);
  if (id !== selectedId) return;
  if (!response.ok) return showError(await readApiError(response));
  const report = await response.json() as AuditReport;
  if (id !== selectedId) return;
  renderReport(report);
  scrollToView(view);
};

const loadReports = async () => {
  const response = await auditFetch('/api/reports');
  if (!response.ok) throw new Error(await readApiError(response));
  reports = await response.json() as ReportSummary[];
  renderList();
};

const loadReferences = async () => {
  const [deviceResponse, visibleSites] = await Promise.all([auditFetch('/api/devices'), loadVisibleSites()]);
  if (deviceResponse.ok) devices = await deviceResponse.json() as NetworkDevice[];
  sites = visibleSites;
  const deviceSelect = form?.querySelector<HTMLSelectElement>('[name=dispositivoId]');
  devices.forEach((device) => deviceSelect?.add(new Option(`${device.identificador} · ${device.nombre}`, String(device.id))));
  if (!isAdmin) {
    const own = getStoredUser()?.organizationId;
    const response = own ? await authFetch(`/api/client-organizations/${own}`) : null;
    if (response?.ok) organizations = [await response.json() as Organization];
  } else {
    const response = await authFetch('/api/client-organizations');
    if (response.ok) organizations = await response.json() as Organization[];
    const orgSelect = form?.querySelector<HTMLSelectElement>('[name=organizacionId]');
    organizations.forEach((organization) => orgSelect?.add(new Option(organization.razonSocial, String(organization.id))));
    document.querySelector('#report-org-filter')?.removeAttribute('hidden');
  }
};

const setupForm = () => {
  if (!form) return;
  const today = new Date();
  const monthAgo = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 30);
  form.querySelector<HTMLInputElement>('[name=desde]')!.value = isoDate(monthAgo);
  form.querySelector<HTMLInputElement>('[name=hasta]')!.value = isoDate(today);
  const presetAudit = params.get('auditoria');
  if (presetAudit) form.querySelector<HTMLInputElement>('[name=auditorias]')!.value = presetAudit;
  if (!canGenerate) {
    document.querySelector('#report-readonly')?.classList.remove('is-hidden');
    form.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLButtonElement>('input, select, button')
      .forEach((field) => { field.disabled = true; });
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    showError(null);
    const data = new FormData(form);
    const auditText = String(data.get('auditorias') ?? '').trim();
    const auditIds = auditText ? auditText.split(/[\s,;]+/).filter(Boolean).map((value) => Number(value.replace(/^AUD-0*/i, ''))) : [];
    if (auditIds.some((id) => !Number.isInteger(id) || id <= 0)) {
      return showError(t('reportes.auditoriasInvalidas'));
    }
    const desde = dayToIso(String(data.get('desde') ?? ''), false);
    const hasta = dayToIso(String(data.get('hasta') ?? ''), true);
    if (auditIds.length === 0 && (!desde || !hasta)) {
      return showError(t('reportes.faltaRango'));
    }
    if (desde && hasta && desde > hasta) return showError(t('bitacoras.rangoInvalido'));

    const device = Number(data.get('dispositivoId')) || null;
    const submit = document.querySelector<HTMLButtonElement>('#report-submit');
    if (submit) submit.disabled = true;
    try {
      const response = await auditFetch('/api/reports/audits', jsonRequest('POST', {
        organizacionId: Number(data.get('organizacionId')) || null,
        desde,
        hasta,
        dispositivoIds: device ? [device] : [],
        severidades: data.getAll('severidad'),
        estados: data.getAll('estado'),
        auditoriaIds: auditIds,
      }));
      if (!response.ok) return showError(await readApiError(response));
      if (response.status !== 201) {
        const body = await response.json() as { message?: string };
        return showError(body.message ?? t('reportes.sinResultados'));
      }
      const report = await response.json() as AuditReport;
      showSuccess(t('reportes.generado', { codigo: code('REP', report.id), auditorias: report.totalAuditorias, hallazgos: report.totalHallazgos }));
      await loadReports();
      selectedId = report.id;
      renderList();
      renderReport(report);
      scrollToView(view);
    } finally {
      if (submit) submit.disabled = !canGenerate;
    }
  });
};

setupForm();
loadReferences()
  .catch(() => {})
  .then(loadReports)
  .then(() => {
    const initial = Number(params.get('reporte')) || null;
    if (initial) void openReport(initial);
  })
  .catch((error) => showError(error instanceof Error ? error.message : t('reportes.errorCargar')));
