// Tablero de control: indicadores, gráficos, alertas y auditorías recientes.
import { auditFetch, authFetch, getStoredUser, hasRole } from '../lib/auth';
import { readApiError, type CriticalAlert, type DashboardSummary, type NetworkDevice, type Organization, type ReviewStatus, type Site, type User } from '../lib/api';
import { alertStatusBadge, code, escapeHtml, severityClass, severityCode } from '../lib/format';
import { t } from '../lib/i18n';
import {
  deviceRiskRowsHtml,
  evolutionChartHtml,
  kpiHtml,
  recentAuditsHtml,
  riskBannerHtml,
  severityChartHtml,
  wireTooltips,
} from '../lib/dashboard-view';
import { enlacesManual } from '../lib/manual';
import { loadVisibleSites } from '../lib/sites';
import { setHtml, show } from '../lib/ui';

const isAdmin = hasRole('ADMINISTRADOR_SISTEMA');
let devices: NetworkDevice[] = [];
let sites: Site[] = [];
let organizations: Organization[] = [];
let loadSeq = 0;

const showError = (message: string | null) => show('#dash-error', message);
const deviceName = (id: number) => devices.find((device) => device.id === id)?.identificador ?? `#${id}`;
const selectedOrg = () => Number(document.querySelector<HTMLSelectElement>('#dash-org')?.value) || null;

const alertsHtml = (alerts: CriticalAlert[]) => alerts.length === 0
  ? `<p class="log-muted">${escapeHtml(t('tablero.sinEventos'))}</p>`
  : `<ul class="compare-list">${alerts.slice(0, 5).map((alert) => `
      <li class="compare-item ${alert.severidad === 'CRITICA' ? 'compare-nuevo_hallazgo' : ''}">
        <a class="inline-link" href="/alerts?alerta=${alert.id}"><code>${code('ALR', alert.id)}</code></a>
        <span>${escapeHtml(alert.codigoRegla ?? '')} · ${escapeHtml(alert.dispositivoIdentificador ?? `#${alert.dispositivoId}`)}</span>
        <span class="badge-row"><span class="severity-badge ${severityClass(alert.severidad)}">${escapeHtml(severityCode(alert.severidad))}</span>${alertStatusBadge(alert.estado)}</span>
      </li>`).join('')}</ul>${alerts.length > 5 ? `<p class="log-muted">${escapeHtml(t('tablero.yMas', { n: alerts.length - 5 }))}</p>` : ''}`;

const load = async () => {
  showError(null);
  const org = selectedOrg();
  const seq = ++loadSeq;
  const [summaryResponse, alertResponse, reviewResponse] = await Promise.all([
    auditFetch(`/api/dashboard/summary${org ? `?organizacionId=${org}` : ''}`),
    auditFetch('/api/alerts/critical'),
    auditFetch(`/api/review/status${org ? `?organizationId=${org}` : ''}`),
  ]);
  if (seq !== loadSeq) return;
  if (!summaryResponse.ok) {
    showError(await readApiError(summaryResponse));
    setHtml('#dash-kpis', `<article class="metric-card"><span class="metric-label">${escapeHtml(t('tablero.sinDatos'))}</span><strong>—</strong><small>${escapeHtml(t('tablero.errorResumen'))}</small></article>`);
    return;
  }
  const summary = await summaryResponse.json() as DashboardSummary;
  const review = reviewResponse.ok ? await reviewResponse.json() as ReviewStatus : null;
  if (seq !== loadSeq) return;

  const orgName = org
    ? organizations.find((item) => item.id === org)?.razonSocial ?? t('tablero.organizacionNumero', { id: org })
    : null;
  const scope = isAdmin ? orgName ?? t('tablero.todasOrganizaciones') : t('tablero.tuOrganizacion');
  const title = document.querySelector('#dash-scope-title');
  if (title) title.textContent = isAdmin && !org ? t('tablero.estadoPlataforma') : t('tablero.estadoDe', { alcance: scope });
  const clientLink = document.querySelector<HTMLAnchorElement>('#dash-client-link');
  if (clientLink) {
    clientLink.href = org ? `/client-status?organizacion=${org}` : '/client-status';
  }

  setHtml('#dash-risk', riskBannerHtml(summary, scope));
  setHtml('#dash-kpis', kpiHtml(summary, review));
  setHtml('#dash-severity', severityChartHtml(summary));
  setHtml('#dash-evolution', evolutionChartHtml(summary.evolucion));
  setHtml('#dash-recent', recentAuditsHtml(summary.auditoriasRecientes, deviceName));
  setHtml('#dash-risk-devices', deviceRiskRowsHtml(summary.dispositivosEnRiesgo, sites));
  document.querySelector('#dash-risk-empty')?.classList.toggle('is-hidden', summary.dispositivosEnRiesgo.length > 0);

  // Si una sección falla, el resto del tablero se muestra igual.
  if (alertResponse.ok) {
    const alerts = (await alertResponse.json() as CriticalAlert[])
      .filter((alert) => !org || alert.organizacionId === org);
    setHtml('#dash-alerts', alertsHtml(alerts));
  } else {
    setHtml('#dash-alerts', `<p class="log-muted">${escapeHtml(t('tablero.errorAlertas', { error: await readApiError(alertResponse) }))}</p>`);
  }
  wireTooltips(document);
};

const loadReferences = async () => {
  const [deviceResponse, visibleSites] = await Promise.all([auditFetch('/api/devices'), loadVisibleSites()]);
  if (deviceResponse.ok) devices = await deviceResponse.json() as NetworkDevice[];
  sites = visibleSites;
  if (!isAdmin) return;

  document.querySelector('#dash-org-filter')?.removeAttribute('hidden');
  document.querySelectorAll('[data-admin-section]').forEach((node) => node.removeAttribute('hidden'));
  const [orgResponse, userResponse] = await Promise.all([authFetch('/api/client-organizations'), authFetch('/api/users')]);
  if (orgResponse.ok) {
    organizations = await orgResponse.json() as Organization[];
    const select = document.querySelector<HTMLSelectElement>('#dash-org');
    organizations.forEach((organization) => select?.add(new Option(organization.razonSocial, String(organization.id))));
    setHtml('#metric-organizations', String(organizations.length));
    setHtml('#metric-sites', String(sites.length));
  }
  if (userResponse.ok) setHtml('#metric-users', String((await userResponse.json() as User[]).length));
};

document.querySelector('#dash-org')?.addEventListener('change', () => void load());

if (!isAdmin && !getStoredUser()?.organizationId) {
  showError(t('tablero.sinOrganizacion'));
}
loadReferences()
  .catch(() => {})
  .then(load)
  .catch((error) => showError(error instanceof Error ? error.message : t('tablero.errorResumen')));

// Manual de uso arriba del tablero hasta que se oculta (sigue en el menú del usuario).
const MANUAL_OCULTO = 'auditnet.manual.oculto';
const manual = document.querySelector<HTMLElement>('#dash-manual');
const manualOculto = () => {
  try {
    return localStorage.getItem(MANUAL_OCULTO) === '1';
  } catch {
    return false;
  }
};
const enlaces = enlacesManual('button button-primary button-small', 'manual.descargar');
if (manual && enlaces && !manualOculto()) {
  setHtml('#dash-manual-links', enlaces);
  manual.hidden = false;
}
document.querySelector('#dash-manual-hide')?.addEventListener('click', () => {
  try {
    localStorage.setItem(MANUAL_OCULTO, '1');
  } catch {
    // Sin almacenamiento se oculta solo en esta visita.
  }
  if (manual) manual.hidden = true;
});
