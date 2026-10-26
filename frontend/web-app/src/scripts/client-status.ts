// Estado del cliente: riesgo por sede y por dispositivo de una organización.
import { auditFetch, authFetch, getStoredUser, hasRole } from '../lib/auth';
import { readApiError, type ClientAuditStatus, type NetworkDevice, type Organization, type ReviewStatus, type Site, type SiteRisk } from '../lib/api';
import { code, escapeHtml, formatDate, riskBadge } from '../lib/format';
import { deviceRiskRowsHtml, kpiHtml, recentAuditsHtml, riskBannerHtml, severityChartHtml, wireTooltips } from '../lib/dashboard-view';
import { t } from '../lib/i18n';
import { setHtml, show } from '../lib/ui';

const isAdmin = hasRole('ADMINISTRADOR_SISTEMA');
const params = new URLSearchParams(window.location.search);
let organizations: Organization[] = [];
let devices: NetworkDevice[] = [];
let sites: Site[] = [];
let loadSeq = 0;

const showError = (message: string | null) => show('#client-error', message);

const siteRowsHtml = (rows: SiteRisk[]) => rows.map((row) => {
  const name = row.sedeId == null
    ? t('cliente.sinSedeAsignada')
    : sites.find((site) => site.id === row.sedeId)?.nombre ?? t('tablero.sedeNumero', { id: row.sedeId });
  return `
    <tr class="${row.nivel === 'ALTO' ? 'is-critical' : ''}">
      <td><strong>${escapeHtml(name)}</strong></td>
      <td>${riskBadge(row.nivel)}</td>
      <td>${row.dispositivos}</td>
      <td>${row.auditados}/${row.dispositivos}</td>
      <td>${row.pendientes}</td>
      <td>${row.criticosPendientes}</td>
      <td>${row.alertasVigentes > 0 && row.sedeId != null
        ? `<a class="inline-link" href="/alerts?sede=${row.sedeId}">${row.alertasVigentes} →</a>`
        : row.alertasVigentes}</td>
    </tr>`;
}).join('');

const load = async (organizationId: number) => {
  showError(null);
  const seq = ++loadSeq;
  const [statusResponse, siteResponse, deviceResponse, reviewResponse] = await Promise.all([
    auditFetch(`/api/client-organizations/${organizationId}/audit-status`),
    authFetch(`/api/client-organizations/${organizationId}/sites`),
    auditFetch('/api/devices'),
    auditFetch(`/api/review/status?organizationId=${organizationId}`),
  ]);
  if (seq !== loadSeq) return;
  if (!statusResponse.ok) {
    showError(await readApiError(statusResponse));
    return;
  }
  const status = await statusResponse.json() as ClientAuditStatus;
  sites = siteResponse.ok ? await siteResponse.json() as Site[] : [];
  devices = deviceResponse.ok ? await deviceResponse.json() as NetworkDevice[] : [];
  const review = reviewResponse.ok ? await reviewResponse.json() as ReviewStatus : null;
  if (seq !== loadSeq) return;

  const organization = organizations.find((item) => item.id === organizationId);
  const title = document.querySelector('#client-title');
  if (title) title.textContent = organization ? organization.razonSocial : t('dispositivos.organizacionNumero', { id: organizationId });

  const summary = status.resumen;
  setHtml('#client-risk', riskBannerHtml(summary, organization?.razonSocial ?? t('general.organizacion')));
  setHtml('#client-kpis', kpiHtml(summary, review));
  setHtml('#client-sites', siteRowsHtml(status.sedes));
  document.querySelector('#client-sites-empty')?.classList.toggle('is-hidden', status.sedes.length > 0);
  setHtml('#client-severity', severityChartHtml(summary));
  setHtml('#client-recent', recentAuditsHtml(summary.auditoriasRecientes,
    (id) => devices.find((device) => device.id === id)?.identificador ?? `#${id}`));
  setHtml('#client-devices', deviceRiskRowsHtml(status.dispositivos, sites));
  setHtml('#client-reports', status.reportes.length === 0
    ? `<p class="log-muted">${escapeHtml(t('cliente.sinReportes'))}</p>`
    : `<ul class="compare-list">${status.reportes.map((report) => `
        <li class="compare-item">
          <a class="inline-link" href="/reports?reporte=${report.id}"><code>${code('REP', report.id)}</code></a>
          <span>${escapeHtml(report.titulo)}</span>
          <small>${escapeHtml(formatDate(report.fechaGeneracion))}</small>
        </li>`).join('')}</ul>`);
  if (!siteResponse.ok) showError(t('cliente.errorSedes'));
  wireTooltips(document);
};

const start = async () => {
  if (isAdmin) {
    const response = await authFetch('/api/client-organizations');
    if (!response.ok) throw new Error(await readApiError(response));
    organizations = await response.json() as Organization[];
    if (organizations.length === 0) throw new Error(t('cliente.sinOrganizaciones'));
    const select = document.querySelector<HTMLSelectElement>('#client-org');
    organizations.forEach((organization) => select?.add(new Option(organization.razonSocial, String(organization.id))));
    const requested = Number(params.get('organizacion'));
    const initial = organizations.some((item) => item.id === requested) ? requested : organizations[0].id;
    if (select) {
      select.value = String(initial);
      select.addEventListener('change', () => {
        window.history.replaceState(null, '', `?organizacion=${select.value}`);
        void load(Number(select.value));
      });
    }
    document.querySelector('#client-org-filter')?.removeAttribute('hidden');
    await load(initial);
    return;
  }
  const own = getStoredUser()?.organizationId;
  if (!own) throw new Error(t('tablero.sinOrganizacion'));
  const response = await authFetch(`/api/client-organizations/${own}`);
  if (response.ok) organizations = [await response.json() as Organization];
  await load(own);
};

start().catch((error) => showError(error instanceof Error ? error.message : t('cliente.errorEstado')));
