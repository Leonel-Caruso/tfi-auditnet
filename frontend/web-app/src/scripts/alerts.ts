// Alertas críticas: listado priorizado, detalle y derivación a un perfil técnico.
import { auditFetch, hasRole } from '../lib/auth';
import {
  jsonRequest,
  readApiError,
  type CriticalAlert,
  type CriticalAlertDetail,
  type NetworkDevice,
  type Site,
} from '../lib/api';
import { alertStatusBadge, code, escapeHtml, findingStatusBadge, formatDate, severityClass, severityCode } from '../lib/format';
import { etiqueta, t } from '../lib/i18n';
import { loadVisibleSites } from '../lib/sites';
import { show } from '../lib/ui';
import { evidencia } from '../lib/evidencia';

let alerts: CriticalAlert[] = [];
let devices: NetworkDevice[] = [];
let sites: Site[] = [];
let selectedId: number | null = null;
let loadSeq = 0;

// El tablero y la vista del cliente llegan con filtros (?severidad=CRITICA, ?dispositivo=ID, ?alerta=ID).
const params = new URLSearchParams(window.location.search);
const initialAlert = Number(params.get('alerta')) || null;

// Derivan o devuelven el administrador, el responsable de gestión IT y el analista de redes.
const canManage = hasRole('ADMINISTRADOR_SISTEMA') || hasRole('RESPONSABLE_GESTION_IT') || hasRole('ANALISTA_RED');

const form = document.querySelector<HTMLFormElement>('#alert-filters');
const siteName = (id: number | null) => id == null
  ? t('tablero.sinSede')
  : sites.find((site) => site.id === id)?.nombre ?? t('tablero.sedeNumero', { id });
const profileName = (profile: string | null) => (profile ? etiqueta('perfil', profile) : t('alertas.sinDerivar'));

const showError = (message: string | null) => show('#alert-error', message);
const showSuccess = (message: string) => {
  show('#alert-success', message);
  window.setTimeout(() => show('#alert-success', null), 4200);
};

const buildQuery = () => {
  const query = new URLSearchParams();
  if (form) {
    const data = new FormData(form);
    for (const name of ['estado', 'severidad', 'criticidad', 'dispositivoId', 'sedeId']) {
      const value = String(data.get(name) ?? '');
      if (value) query.set(name, value);
    }
  }
  const text = query.toString();
  return text ? `?${text}` : '';
};

const renderMetrics = (all: CriticalAlert[]) => {
  const set = (selector: string, value: number) => {
    const node = document.querySelector<HTMLElement>(selector);
    if (node) node.textContent = String(value);
  };
  const open = all.filter((alert) => alert.estado !== 'CERRADA');
  set('#metric-alert-open', open.length);
  set('#metric-alert-active', open.filter((alert) => alert.estado === 'ACTIVA').length);
  set('#metric-alert-progress', open.filter((alert) => alert.estado === 'EN_ATENCION').length);
  set('#metric-alert-critical', open.filter((alert) => alert.severidad === 'CRITICA').length);
};

const render = () => {
  const body = document.querySelector<HTMLTableSectionElement>('#alert-table-body');
  if (!body) return;
  body.innerHTML = alerts.map((alert) => {
    const classes = ['finding-row'];
    if (alert.id === selectedId) classes.push('is-selected');
    if (alert.severidad === 'CRITICA' && alert.estado !== 'CERRADA') classes.push('is-critical');
    if (alert.estado === 'CERRADA') classes.push('is-closed');
    return `
      <tr class="${classes.join(' ')}" data-alert-id="${alert.id}" tabindex="0">
        <td><code>${code('ALR', alert.id)}</code></td>
        <td><strong>${escapeHtml(alert.codigoRegla ?? t('alertas.hallazgoNumero', { id: alert.hallazgoId }))}</strong><small>${escapeHtml(alert.nombreRegla ?? '')}</small></td>
        <td>
          <strong>${escapeHtml(alert.dispositivoIdentificador ?? `#${alert.dispositivoId}`)}</strong>
          <small>${escapeHtml(t('hallazgos.criticidad', { criticidad: severityCode(alert.criticidadDispositivo) }))} · ${escapeHtml(siteName(alert.sedeId))}</small>
        </td>
        <td><span class="severity-badge ${severityClass(alert.severidad)}">${escapeHtml(severityCode(alert.severidad))}</span></td>
        <td>${alertStatusBadge(alert.estado)}${alert.perfilAsignado ? `<small>${escapeHtml(profileName(alert.perfilAsignado))}</small>` : ''}</td>
        <td><span class="log-nowrap">${escapeHtml(formatDate(alert.fechaGeneracion))}</span></td>
      </tr>`;
  }).join('');

  const count = document.querySelector('#alert-count');
  if (count) count.textContent = String(alerts.length);
  const data = form ? new FormData(form) : null;
  const onlyDefault = !data || (String(data.get('estado')) === 'VIGENTES'
    && ['severidad', 'criticidad', 'dispositivoId', 'sedeId'].every((name) => !data.get(name)));
  const emptyText = document.querySelector('#alert-empty-text');
  if (emptyText) {
    emptyText.textContent = onlyDefault
      ? t('alertas.hayAlertasCriticasPendientes')
      : t('alertas.sinCoincidencias');
  }
  document.querySelector('#alert-empty')?.classList.toggle('is-hidden', alerts.length > 0);

  body.querySelectorAll<HTMLElement>('[data-alert-id]').forEach((row) => {
    const select = () => void selectAlert(Number(row.dataset.alertId));
    row.addEventListener('click', select);
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        select();
      }
    });
  });
};

const manageFormHtml = (alert: CriticalAlert) => {
  if (alert.estado === 'CERRADA') {
    return `<p class="log-muted">${escapeHtml(t('alertas.cerradaAyuda'))}</p>`;
  }
  if (!canManage) {
    return `<p class="log-muted">${escapeHtml(t('alertas.soloConsulta'))}</p>`;
  }
  const giveBack = alert.estado === 'EN_ATENCION'
    ? `<option value="ACTIVA">${escapeHtml(t('alertas.devolver'))}</option>`
    : '';
  return `
    <form id="alert-form" class="tracking-form" novalidate>
      <label class="log-filter">
        <span>${t('alertas.accion')}</span>
        <select id="alert-action" class="technical-select">
          <option value="ANALISTA_RED">${escapeHtml(t('alertas.derivarA', { perfil: etiqueta('perfil', 'ANALISTA_RED') }))}</option>
          <option value="AUDITOR_TECNICO">${escapeHtml(t('alertas.derivarA', { perfil: etiqueta('perfil', 'AUDITOR_TECNICO') }))}</option>
          ${giveBack}
        </select>
      </label>
      <label class="log-filter">
        <span id="alert-comment-label">${t('hallazgos.comentarioOpcional')}</span>
        <textarea id="alert-comment" class="technical-input tracking-comment" rows="3" maxlength="1000"
          placeholder="${t('alertas.comentarioEjemplo')}"></textarea>
      </label>
      <div id="alert-form-error" class="alert alert-error is-hidden"></div>
      <button class="button button-primary button-small button-full" type="submit">${t('alertas.actualizar')}</button>
    </form>`;
};

const selectAlert = async (id: number) => {
  selectedId = id;
  render();
  const response = await auditFetch(`/api/alerts/${id}`);
  if (id !== selectedId) return;
  if (!response.ok) return showError(await readApiError(response));
  const detail = await response.json() as CriticalAlertDetail;
  if (id !== selectedId) return;
  const { alerta: alert, hallazgo: finding } = detail;

  document.querySelector('#alert-inspector-empty')?.classList.add('is-hidden');
  const content = document.querySelector<HTMLElement>('#alert-inspector-content');
  if (!content) return;
  content.innerHTML = `
    <div class="inspector-identity">
      <span class="inspector-code">${code('ALR', alert.id)} · ${escapeHtml(formatDate(alert.fechaGeneracion))}</span>
      <h3>${escapeHtml(finding?.nombreRegla ?? alert.nombreRegla ?? t('alertas.hallazgoNoDisponible'))}</h3>
      <div class="badge-row">
        <span class="severity-badge ${severityClass(alert.severidad)}">${escapeHtml(severityCode(alert.severidad))}</span>
        ${alertStatusBadge(alert.estado)}
      </div>
    </div>
    ${finding ? '' : `<div class="alert alert-error">${escapeHtml(t('alertas.errorHallazgo'))}</div>`}
    <dl class="technical-definition-list">
      <div><dt>${t('general.dispositivo')}</dt><dd>${escapeHtml(alert.dispositivoIdentificador ?? `#${alert.dispositivoId}`)} · ${escapeHtml(t('hallazgos.criticidad', { criticidad: severityCode(alert.criticidadDispositivo) }))}</dd></div>
      <div><dt>${t('general.sede')}</dt><dd>${escapeHtml(siteName(alert.sedeId))}</dd></div>
      <div><dt>${t('alertas.hallazgo')}</dt><dd><a class="inline-link" href="/results?hallazgo=${alert.hallazgoId}">${code('FND', alert.hallazgoId)} →</a> ${finding ? findingStatusBadge(finding.estado) : ''}</dd></div>
      <div><dt>${t('hallazgos.auditoria')}</dt><dd><a class="inline-link" href="/history?auditoria=${alert.auditoriaId}">${code('AUD', alert.auditoriaId)} →</a></dd></div>
      <div><dt>${t('alertas.derivadaA')}</dt><dd>${escapeHtml(profileName(alert.perfilAsignado))}</dd></div>
      <div><dt>${t('hallazgos.ultimoCambio')}</dt><dd>${alert.fechaEstado
        ? `${escapeHtml(formatDate(alert.fechaEstado))} · ${escapeHtml(alert.usuarioEstado)}`
        : escapeHtml(t('hallazgos.sinCambios'))}</dd></div>
    </dl>
    ${alert.comentario ? `<div class="rule-recommendation"><span>${t('alertas.ultimoComentario')}</span><p>${escapeHtml(alert.comentario)}</p></div>` : ''}
    ${finding ? `
    <div class="finding-evidence-block">
      <span>${t('hallazgos.evidencia')}</span>
      <p>${escapeHtml(evidencia(finding.evidencia))}</p>
    </div>
    ${finding.impacto ? `<div class="finding-evidence-block"><span>${t('reglas.impacto')}</span><p>${escapeHtml(finding.impacto)}</p></div>` : ''}
    <div class="rule-recommendation">
      <span>${t('reglas.recomendacion')}</span>
      <p>${escapeHtml(finding.recomendacion)}</p>
    </div>` : ''}
    ${manageFormHtml(alert)}
  `;
  content.classList.remove('is-hidden');
  wireForm(alert);
};

const wireForm = (alert: CriticalAlert) => {
  const alertForm = document.querySelector<HTMLFormElement>('#alert-form');
  const action = document.querySelector<HTMLSelectElement>('#alert-action');
  const comment = document.querySelector<HTMLTextAreaElement>('#alert-comment');
  if (!alertForm || !action || !comment) return;

  const syncLabel = () => {
    const label = document.querySelector('#alert-comment-label');
    if (label) label.textContent = t(action.value === 'ACTIVA' ? 'alertas.motivoObligatorio' : 'hallazgos.comentarioOpcional');
  };
  action.addEventListener('change', syncLabel);
  syncLabel();

  alertForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    show('#alert-form-error', null);
    const giveBack = action.value === 'ACTIVA';
    if (giveBack && !comment.value.trim()) {
      show('#alert-form-error', t('alertas.faltaMotivo'));
      return;
    }
    const response = await auditFetch(`/api/alerts/${alert.id}/status`, jsonRequest('PATCH', {
      estado: giveBack ? 'ACTIVA' : 'EN_ATENCION',
      perfil: giveBack ? null : action.value,
      comentario: comment.value.trim() || null,
    }));
    if (!response.ok) {
      show('#alert-form-error', await readApiError(response));
      return;
    }
    const updated = await response.json() as CriticalAlertDetail;
    showSuccess(giveBack
      ? t('alertas.devuelta', { codigo: code('ALR', updated.alerta.id) })
      : t('alertas.derivada', { codigo: code('ALR', updated.alerta.id), perfil: profileName(updated.alerta.perfilAsignado) }));
    await loadAlerts();
    await selectAlert(updated.alerta.id);
  });
};

const loadAlerts = async () => {
  showError(null);
  const query = buildQuery();
  const seq = ++loadSeq;
  // Las métricas muestran siempre el total vigente; la lista, lo filtrado.
  const [response, openResponse] = await Promise.all([
    auditFetch(`/api/alerts/critical${query}`),
    auditFetch('/api/alerts/critical'),
  ]);
  if (seq !== loadSeq) return;
  if (!response.ok) {
    showError(await readApiError(response));
    return;
  }
  const data = await response.json() as CriticalAlert[];
  const open = openResponse.ok ? await openResponse.json() as CriticalAlert[] : data;
  if (seq !== loadSeq) return;
  alerts = data;
  renderMetrics(open);
  render();
};

const loadReferences = async () => {
  const [deviceResponse, visibleSites] = await Promise.all([auditFetch('/api/devices'), loadVisibleSites()]);
  if (!deviceResponse.ok) throw new Error(await readApiError(deviceResponse));
  devices = await deviceResponse.json() as NetworkDevice[];
  sites = visibleSites;

  const deviceSelect = form?.querySelector<HTMLSelectElement>('[name=dispositivoId]');
  devices.forEach((device) => deviceSelect?.add(new Option(`${device.identificador} · ${device.nombre}`, String(device.id))));
  const siteSelect = form?.querySelector<HTMLSelectElement>('[name=sedeId]');
  sites.forEach((site) => siteSelect?.add(new Option(site.nombre, String(site.id))));

  // Filtros que llegan desde el tablero o la vista del cliente.
  const preset: Record<string, string | null> = {
    estado: params.get('estado'),
    severidad: params.get('severidad'),
    criticidad: params.get('criticidad'),
    dispositivoId: params.get('dispositivo'),
    sedeId: params.get('sede'),
  };
  for (const [name, value] of Object.entries(preset)) {
    const field = form?.querySelector<HTMLSelectElement>(`[name=${name}]`);
    if (field && value && Array.from(field.options).some((option) => option.value === value)) field.value = value;
  }
};

form?.addEventListener('submit', (event) => {
  event.preventDefault();
  void loadAlerts();
});
document.querySelector('#alert-clear')?.addEventListener('click', () => {
  form?.reset();
  window.history.replaceState(null, '', window.location.pathname);
  void loadAlerts();
});

loadReferences()
  .then(loadAlerts)
  .then(() => {
    if (initialAlert) void selectAlert(initialAlert);
  })
  .catch((error) => showError(error instanceof Error ? error.message : t('alertas.errorCargar')));
