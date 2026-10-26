// Historial de auditorías: evidencia de cada auditoría y comparación con la anterior.
import { auditFetch } from '../lib/auth';
import {
  readApiError,
  type AuditComparison,
  type AuditDetail,
  type AuditSummary,
  type ConfigurationBaseline,
  type DeviceConfiguration,
  type NetworkDevice,
  type RuleChangeCategory,
} from '../lib/api';
import { code, escapeHtml, findingStatusBadge, formatDate, localToIso, severityClass, severityCode } from '../lib/format';
import { etiqueta, t } from '../lib/i18n';
import { crearPaginador, totalDeRespuesta } from '../lib/paginador';
import { show } from '../lib/ui';
import { evidencia } from '../lib/evidencia';

let audits: AuditSummary[] = [];
let devices: NetworkDevice[] = [];
let baselines: ConfigurationBaseline[] = [];
let selectedId: number | null = null;
// Evita que una respuesta lenta pise a una más nueva (filtros o selección cambiados rápido).
let loadSeq = 0;

const params = new URLSearchParams(window.location.search);
const initialAudit = Number(params.get('auditoria')) || null;
const initialDevice = Number(params.get('dispositivo')) || null;

const form = document.querySelector<HTMLFormElement>('#history-filters');
// El historial crece con cada auditoría: se pide al servidor de a una página.
const paginador = crearPaginador(document.querySelector<HTMLElement>('#history-pager'), { alCambiar: () => void load() });
let total = 0;
const deviceById = (id: number) => devices.find((item) => item.id === id);
const baselineById = (id: number) => baselines.find((item) => item.id === id);
const automaticBadge = (audit: AuditSummary) => audit.origen === 'AUTOMATICA'
  ? ` <span class="status-badge status-next" title="${t('historial.automaticaAyuda')}">${t('historial.automatica')}</span>`
  : '';

const showError = (message: string | null) => show('#history-error', message);

const CATEGORIES: RuleChangeCategory[] = ['NUEVO_HALLAZGO', 'CORREGIDO', 'PERSISTENTE', 'SIN_CAMBIO', 'REGLA_NUEVA', 'REGLA_RETIRADA'];
const categoryLabel = (category: RuleChangeCategory) => etiqueta('cambio', category);

const buildQuery = () => {
  const query = new URLSearchParams();
  if (!form) return '';
  const data = new FormData(form);
  for (const name of ['dispositivoId', 'baselineId', 'severidad', 'resultado', 'ejecutadoPor']) {
    const value = String(data.get(name) ?? '').trim();
    if (value) query.set(name, value);
  }
  const desde = localToIso(String(data.get('desde') ?? ''));
  const hasta = localToIso(String(data.get('hasta') ?? ''), true);
  if (desde) query.set('desde', desde);
  if (hasta) query.set('hasta', hasta);
  if (desde && hasta && desde > hasta) throw new Error(t('bitacoras.rangoInvalido'));
  const text = query.toString();
  return text ? `?${text}` : '';
};

const render = () => {
  const body = document.querySelector<HTMLTableSectionElement>('#history-body');
  if (!body) return;
  body.innerHTML = audits.map((audit) => {
    const device = deviceById(audit.dispositivoId);
    const baseline = baselineById(audit.baselineId);
    return `
      <tr class="finding-row ${audit.id === selectedId ? 'is-selected' : ''}" data-audit-id="${audit.id}" tabindex="0">
        <td><code>${code('AUD', audit.id)}</code></td>
        <td><span class="log-nowrap">${escapeHtml(formatDate(audit.fechaEjecucion))}</span></td>
        <td><strong>${escapeHtml(device?.identificador ?? `#${audit.dispositivoId}`)}</strong><small>${escapeHtml(device?.nombre ?? '')}</small></td>
        <td>${escapeHtml(baseline ? `${baseline.nombre} v${baseline.version}` : `#${audit.baselineId}`)}</td>
        <td><code>${audit.reglasCumplidas}/${audit.totalReglas}</code></td>
        <td><strong>${audit.totalHallazgos}</strong></td>
        <td>${audit.severidadMaxima === 'NINGUNA'
          ? `<span class="status-badge status-ready">${escapeHtml(severityCode('NINGUNA'))}</span>`
          : `<span class="severity-badge ${severityClass(audit.severidadMaxima)}">${escapeHtml(severityCode(audit.severidadMaxima))}</span>`}</td>
        <td><small>${escapeHtml(audit.ejecutadoPor)}</small>${automaticBadge(audit)}</td>
      </tr>`;
  }).join('');

  const count = document.querySelector('#history-count');
  if (count) count.textContent = String(total);
  document.querySelector('#history-empty')?.classList.toggle('is-hidden', audits.length > 0);

  body.querySelectorAll<HTMLElement>('[data-audit-id]').forEach((row) => {
    const select = () => void selectAudit(Number(row.dataset.auditId));
    row.addEventListener('click', select);
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        select();
      }
    });
  });
};

const comparisonHtml = (comparison: AuditComparison) => {
  if (!comparison.anterior) {
    return `<p class="log-muted">${escapeHtml(t('historial.primeraAuditoria'))}</p>`;
  }
  const chips = CATEGORIES
    .filter((category) => (comparison.resumen[category] ?? 0) > 0)
    .map((category) => `<span class="compare-chip compare-${category.toLowerCase()}">${comparison.resumen[category]} ${escapeHtml(categoryLabel(category))}</span>`)
    .join('');
  const relevant = comparison.cambios.filter((change) => change.categoria !== 'SIN_CAMBIO');
  const items = relevant.map((change) => `
    <li class="compare-item compare-${change.categoria.toLowerCase()}">
      <code>${escapeHtml(change.codigoRegla)}</code>
      <span>${escapeHtml(change.nombreRegla)}</span>
      <small>${escapeHtml(categoryLabel(change.categoria))}${change.severidad ? ` · ${escapeHtml(severityCode(change.severidad))}` : ''}</small>
    </li>`).join('');
  return `
    <p class="log-muted">${escapeHtml(t(comparison.anterior.baselineId !== comparison.actual.baselineId ? 'historial.respectoDeOtraBaseline' : 'historial.respectoDe', {
      auditoria: code('AUD', comparison.anterior.id), fecha: formatDate(comparison.anterior.fechaEjecucion) }))}</p>
    <div class="compare-chips">${chips}</div>
    ${items ? `<ul class="compare-list">${items}</ul>` : `<p class="log-muted">${escapeHtml(t('historial.mismoResultado'))}</p>`}`;
};

const selectAudit = async (id: number) => {
  selectedId = id;
  render();
  const [detailResponse, comparisonResponse] = await Promise.all([
    auditFetch(`/api/audits/${id}`),
    auditFetch(`/api/audits/${id}/comparison`),
  ]);
  if (id !== selectedId) return; // el usuario ya eligió otra auditoría
  if (!detailResponse.ok) return showError(await readApiError(detailResponse));
  if (!comparisonResponse.ok) return showError(await readApiError(comparisonResponse));
  const detail = await detailResponse.json() as AuditDetail;
  const comparison = await comparisonResponse.json() as AuditComparison;
  if (id !== selectedId) return;
  const audit = detail.auditoria;
  const device = deviceById(audit.dispositivoId);

  const evaluations = detail.evaluaciones.map((evaluation) => `
    <div class="evaluation-item ${evaluation.cumple ? 'is-pass' : 'is-fail'}">
      <div>
        <code>${escapeHtml(evaluation.codigoRegla)}</code>
        <strong>${escapeHtml(evaluation.nombreRegla)}</strong>
      </div>
      <span class="status-badge ${evaluation.cumple ? 'status-ready' : 'status-strong'}">${t(evaluation.cumple ? 'auditorias.cumple' : 'auditorias.hallazgo')}</span>
      <small>${escapeHtml(evaluation.patron)} — ${escapeHtml(evidencia(evaluation.evidencia))}</small>
    </div>`).join('');

  const findings = detail.hallazgos.map((finding) => `
    <li class="compare-item">
      <code>${code('FND', finding.id)}</code>
      <span>${escapeHtml(finding.codigoRegla)}</span>
      ${findingStatusBadge(finding.estado)}
    </li>`).join('');

  const incomplete = !detail.configuracion || !detail.baseline;
  const content = document.querySelector<HTMLElement>('#history-inspector-content');
  if (!content) return;
  document.querySelector('#history-inspector-empty')?.classList.add('is-hidden');
  content.innerHTML = `
    <div class="inspector-identity">
      <span class="inspector-code">${code('AUD', audit.id)} · ${escapeHtml(formatDate(audit.fechaEjecucion))}</span>
      <h3>${escapeHtml(device?.identificador ?? t('auditorias.dispositivoNumero', { id: audit.dispositivoId }))}</h3>
      <span class="status-badge ${audit.resultado === 'CUMPLE' ? 'status-ready' : 'status-strong'}">${escapeHtml(etiqueta('resultado', audit.resultado))}</span>
    </div>
    ${incomplete ? `<div class="alert alert-error">${escapeHtml(t('historial.incompleta'))}</div>` : ''}
    <dl class="technical-definition-list">
      <div><dt>${t('general.baseline')}</dt><dd>${detail.baseline ? `${escapeHtml(detail.baseline.nombre)} v${detail.baseline.version}` : `#${audit.baselineId}`}</dd></div>
      <div><dt>${t('auditorias.configuracion')}</dt><dd>${detail.configuracion
        ? `${code('CFG', detail.configuracion.id)} · v${detail.configuracion.version} · ${escapeHtml(detail.configuracion.nombreFuente)}`
        : code('CFG', audit.configuracionId)}</dd></div>
      <div><dt>${t('historial.importada')}</dt><dd>${detail.configuracion
        ? `${escapeHtml(formatDate(detail.configuracion.fechaImportacion))} · ${escapeHtml(detail.configuracion.usuarioResponsable)}`
        : '—'}</dd></div>
      <div><dt>${t('auditorias.reglasCumplidas')}</dt><dd>${audit.reglasCumplidas}/${audit.totalReglas}</dd></div>
      <div><dt>${t('auditorias.ejecuto')}</dt><dd>${escapeHtml(audit.ejecutadoPor)} ${automaticBadge(audit)}</dd></div>
    </dl>

    <div class="baseline-versions">
      <span>${t('historial.evolucion')}</span>
      ${comparisonHtml(comparison)}
    </div>

    <div class="evaluation-list">
      <span class="eyebrow">${t('historial.reglasAplicadas')}</span>
      ${evaluations}
    </div>

    ${findings ? `
    <div class="baseline-versions">
      <span>${t('historial.hallazgosGenerados')}</span>
      <ul class="compare-list">${findings}</ul>
      <a class="button button-ghost button-small button-full" href="/results?auditoria=${audit.id}">${t('historial.verHallazgos')}</a>
    </div>` : ''}

    ${detail.configuracion ? `
    <button id="show-config" class="button button-ghost button-small button-full" type="button">Ver configuración evaluada</button>
    <div id="config-block" class="config-preview is-hidden">
      <span>configuración normalizada (${code('CFG', detail.configuracion.id)})</span>
      <pre id="config-content">cargando...</pre>
    </div>` : ''}
    <button id="filter-device" class="button button-ghost button-small button-full" type="button">${t('historial.soloDispositivo')}</button>
    <a class="button button-ghost button-small button-full" href="/reports?auditoria=${audit.id}">Generar reporte de esta auditoría →</a>
  `;
  content.classList.remove('is-hidden');

  document.querySelector('#show-config')?.addEventListener('click', async () => {
    const block = document.querySelector('#config-block');
    const pre = document.querySelector('#config-content');
    block?.classList.toggle('is-hidden');
    if (!detail.configuracion || !pre || pre.textContent !== 'cargando...') return;
    const response = await auditFetch(`/api/configurations/${detail.configuracion.id}`);
    pre.textContent = response.ok
      ? (await response.json() as DeviceConfiguration).contenidoNormalizado
      : await readApiError(response);
  });
  document.querySelector('#filter-device')?.addEventListener('click', () => {
    const select = form?.querySelector<HTMLSelectElement>('[name=dispositivoId]');
    if (select) select.value = String(audit.dispositivoId);
    paginador.reiniciar();
    void load();
  });
};

const load = async () => {
  showError(null);
  let query: string;
  try {
    query = buildQuery();
  } catch (error) {
    showError(error instanceof Error ? error.message : t('bitacoras.filtrosInvalidos'));
    return;
  }
  const pagina = `pagina=${paginador.pagina}&porPagina=${paginador.porPagina}`;
  const seq = ++loadSeq;
  const response = await auditFetch(`/api/audits/history${query ? `${query}&` : '?'}${pagina}`);
  if (seq !== loadSeq) return;
  if (!response.ok) {
    showError(await readApiError(response));
    return;
  }
  const data = await response.json() as AuditSummary[];
  if (seq !== loadSeq) return;
  total = totalDeRespuesta(response, data.length);
  const paginaPedida = paginador.pagina;
  paginador.actualizar(total);
  // La página pedida quedó después del final (por ejemplo, cambió el total): se pide la última.
  if (data.length === 0 && total > 0 && paginador.pagina !== paginaPedida) return load();
  audits = data;
  render();
};

const loadReferences = async () => {
  const [deviceResponse, baselineResponse] = await Promise.all([
    auditFetch('/api/devices'),
    auditFetch('/api/baselines'),
  ]);
  for (const response of [deviceResponse, baselineResponse]) {
    if (!response.ok) throw new Error(await readApiError(response));
  }
  devices = await deviceResponse.json() as NetworkDevice[];
  baselines = await baselineResponse.json() as ConfigurationBaseline[];
  const deviceSelect = form?.querySelector<HTMLSelectElement>('[name=dispositivoId]');
  devices.forEach((device) => deviceSelect?.add(new Option(`${device.identificador} · ${device.nombre}`, String(device.id))));
  const baselineSelect = form?.querySelector<HTMLSelectElement>('[name=baselineId]');
  baselines.forEach((baseline) => baselineSelect?.add(new Option(`${baseline.nombre} v${baseline.version}`, String(baseline.id))));
  if (initialDevice && deviceSelect) deviceSelect.value = String(initialDevice);
};

form?.addEventListener('submit', (event) => {
  event.preventDefault();
  paginador.reiniciar();
  void load();
});
document.querySelector('#history-clear')?.addEventListener('click', () => {
  form?.reset();
  paginador.reiniciar();
  window.history.replaceState(null, '', window.location.pathname);
  void load();
});

loadReferences()
  .then(load)
  .then(() => {
    if (initialAudit) void selectAudit(initialAudit);
  })
  .catch((error) => showError(error instanceof Error ? error.message : t('historial.errorCargar')));
