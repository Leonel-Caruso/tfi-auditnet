// Auditorías: importación de configuraciones y ejecución de auditorías.
import { auditFetch, hasRole } from '../lib/auth';
import { readApiError } from '../lib/api';
import { saveResponse } from '../lib/download';
import { code, escapeHtml, formatDate, plural, severityCode } from '../lib/format';
import { etiqueta, t } from '../lib/i18n';
import type {
  AuditDetail,
  AuditSummary,
  BaselineRule,
  ConfigurationBaseline,
  DeviceConfiguration,
  NetworkDevice,
} from '../lib/api';
import { evidencia } from '../lib/evidencia';

let devices: NetworkDevice[] = [];
let baselines: ConfigurationBaseline[] = [];
let rules: BaselineRule[] = [];
let configurations: DeviceConfiguration[] = [];
let audits: AuditSummary[] = [];
let selectedConfigurationId: number | null = null;
let selectedAuditId: number | null = null;

const canOperate =
  hasRole('ADMINISTRADOR_SISTEMA') ||
  hasRole('ANALISTA_RED') ||
  hasRole('AUDITOR_TECNICO');

const openConfigButton = document.querySelector<HTMLButtonElement>('#open-config-dialog');
if (!canOperate) openConfigButton?.setAttribute('hidden', '');

const configDialog = document.querySelector<HTMLDialogElement>('#config-dialog');
const configForm = document.querySelector<HTMLFormElement>('#config-form');
const configDevice = document.querySelector<HTMLSelectElement>('#config-device');
const configFile = document.querySelector<HTMLInputElement>('#config-file');
const configContent = document.querySelector<HTMLTextAreaElement>('#config-content');
const searchInput = document.querySelector<HTMLInputElement>('#audit-search');

const showMessage = (kind: 'success' | 'error', message: string) => {
  const success = document.querySelector<HTMLElement>('#audit-success');
  const error = document.querySelector<HTMLElement>('#audit-error');
  success?.classList.add('is-hidden');
  error?.classList.add('is-hidden');
  const target = kind === 'success' ? success : error;
  if (target) {
    target.textContent = message;
    target.classList.remove('is-hidden');
  }
};

const deviceById = (id: number) => devices.find((item) => item.id === id);
const baselineById = (id: number) => baselines.find((item) => item.id === id);

const applicableBaseline = (configuration: DeviceConfiguration) => {
  const device = deviceById(configuration.dispositivoId);
  if (!device) return undefined;
  return baselines
    .filter((b) =>
      b.organizacionId === configuration.organizacionId &&
      b.tipoDispositivoId === device.tipoDispositivoId &&
      b.estado === 'ACTIVO'
    )
    .sort((a, b) => b.version - a.version || b.id - a.id)[0];
};

const refreshAll = async () => {
  const [deviceResponse, baselineResponse, rulesResponse, configResponse, auditResponse] = await Promise.all([
    auditFetch('/api/devices'),
    auditFetch('/api/baselines'),
    auditFetch('/api/rules'),
    auditFetch('/api/configurations/auditable'),
    auditFetch('/api/audits/history'),
  ]);

  for (const response of [deviceResponse, baselineResponse, rulesResponse, configResponse, auditResponse]) {
    if (!response.ok) throw new Error(await readApiError(response));
  }

  devices = await deviceResponse.json() as NetworkDevice[];
  baselines = await baselineResponse.json() as ConfigurationBaseline[];
  rules = await rulesResponse.json() as BaselineRule[];
  configurations = await configResponse.json() as DeviceConfiguration[];
  audits = await auditResponse.json() as AuditSummary[];

  renderDeviceOptions();
  renderConfigurations();
  renderAudits();
};

const renderDeviceOptions = () => {
  if (!configDevice) return;
  configDevice.innerHTML = `<option value="">${escapeHtml(t('auditorias.seleccionarDispositivo'))}</option>` +
    devices
      .filter((device) => device.estado === 'ACTIVO')
      .map((device) => `<option value="${device.id}">${escapeHtml(device.identificador)} · ${escapeHtml(device.nombre)}</option>`)
      .join('');
};

const renderConfigurations = () => {
  const body = document.querySelector<HTMLTableSectionElement>('#configuration-table-body');
  const empty = document.querySelector<HTMLElement>('#configuration-empty');
  const count = document.querySelector<HTMLElement>('#configuration-count');
  if (!body) return;

  const term = searchInput?.value.trim().toLowerCase() ?? '';
  const filtered = configurations.filter((configuration) => {
    const device = deviceById(configuration.dispositivoId);
    const haystack = [
      code('CFG', configuration.id),
      device?.identificador,
      device?.nombre,
      configuration.version,
      configuration.usuarioResponsable,
      configuration.nombreFuente,
    ].join(' ').toLowerCase();
    return !term || haystack.includes(term);
  });

  if (count) {
    count.textContent = plural(configurations.length, 'auditorias.configuraciones');
  }

  body.innerHTML = filtered.map((configuration) => {
    const device = deviceById(configuration.dispositivoId);
    const selected = configuration.id === selectedConfigurationId ? 'is-selected' : '';
    return `
      <tr class="audit-row ${selected}" data-config-id="${configuration.id}" tabindex="0">
        <td><code>${code('CFG', configuration.id)}</code></td>
        <td>
          <strong>${escapeHtml(device?.identificador ?? `#${configuration.dispositivoId}`)}</strong>
          <small>${escapeHtml(device?.nombre ?? '')}</small>
        </td>
        <td><code>v${configuration.version}</code></td>
        <td>${escapeHtml(configuration.formato)}</td>
        <td>${escapeHtml(configuration.usuarioResponsable)}</td>
        <td>${escapeHtml(formatDate(configuration.fechaImportacion))}</td>
      </tr>`;
  }).join('');

  empty?.classList.toggle('is-hidden', filtered.length > 0);
  body.querySelectorAll<HTMLElement>('[data-config-id]').forEach((row) => {
    const select = () => selectConfiguration(Number(row.dataset.configId));
    row.addEventListener('click', select);
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') select();
    });
  });
};

const renderAudits = () => {
  const body = document.querySelector<HTMLTableSectionElement>('#audit-table-body');
  const empty = document.querySelector<HTMLElement>('#audit-empty');
  if (!body) return;

  body.innerHTML = audits.map((audit) => {
    const device = deviceById(audit.dispositivoId);
    const baseline = baselineById(audit.baselineId);
    const selected = audit.id === selectedAuditId ? 'is-selected' : '';
    const resultClass = audit.resultado === 'CUMPLE' ? 'status-ready' : 'status-strong';
    return `
      <tr class="audit-row ${selected}" data-audit-id="${audit.id}" tabindex="0">
        <td><code>${code('AUD', audit.id)}</code></td>
        <td>${escapeHtml(device?.identificador ?? `#${audit.dispositivoId}`)}</td>
        <td>${escapeHtml(baseline?.nombre ?? `#${audit.baselineId}`)}</td>
        <td>${audit.totalReglas}</td>
        <td>${audit.reglasCumplidas}</td>
        <td><strong>${audit.totalHallazgos}</strong></td>
        <td><span class="status-badge ${resultClass}">${escapeHtml(etiqueta('resultado', audit.resultado))}</span></td>
      </tr>`;
  }).join('');

  empty?.classList.toggle('is-hidden', audits.length > 0);
  body.querySelectorAll<HTMLElement>('[data-audit-id]').forEach((row) => {
    const select = () => selectAudit(Number(row.dataset.auditId));
    row.addEventListener('click', select);
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') select();
    });
  });
};

const showInspector = (html: string) => {
  document.querySelector('#audit-inspector-empty')?.classList.add('is-hidden');
  const content = document.querySelector<HTMLElement>('#audit-inspector-content');
  if (content) {
    content.innerHTML = html;
    content.classList.remove('is-hidden');
  }
};

/** Baja el archivo original del repositorio; el servidor lo verifica contra su SHA-256. */
const downloadOriginal = async (id: number) => {
  const box = document.querySelector<HTMLElement>('#config-file-status');
  const message = document.querySelector<HTMLElement>('#config-file-message');
  const button = document.querySelector<HTMLButtonElement>('#config-file-download');
  if (button) button.disabled = true;
  try {
    const response = await auditFetch(`/api/configurations/${id}/file`);
    if (!box || !message) return;
    if (!response.ok) {
      box.className = `file-integrity ${response.status === 409 ? 'is-bad' : ''}`;
      message.textContent = await readApiError(response);
      return;
    }
    const hash = response.headers.get('X-AuditNet-SHA256') ?? '';
    await saveResponse(response, `${code('CFG', id)}.txt`);
    box.className = 'file-integrity is-ok';
    message.innerHTML = `${escapeHtml(t('auditorias.integridadVerificada'))}<br><code>SHA-256 ${escapeHtml(hash)}</code>`;
  } finally {
    if (button) button.disabled = false;
  }
};

const selectConfiguration = (id: number) => {
  selectedConfigurationId = id;
  selectedAuditId = null;
  renderConfigurations();
  renderAudits();

  const configuration = configurations.find((item) => item.id === id);
  if (!configuration) return;
  const device = deviceById(configuration.dispositivoId);
  const baseline = applicableBaseline(configuration);
  const ruleCount = baseline ? rules.filter((rule) => rule.baselineId === baseline.id && rule.estado === 'ACTIVA').length : 0;
  const preview = configuration.contenidoNormalizado.slice(0, 900);

  showInspector(`
    <div class="inspector-identity">
      <span class="inspector-code">${code('CFG', configuration.id)} · v${configuration.version}</span>
      <h3>${escapeHtml(device?.identificador ?? t('auditorias.dispositivoNumero', { id: configuration.dispositivoId }))}</h3>
      <span class="status-badge status-ready">${t('auditorias.auditable')}</span>
    </div>
    <dl class="technical-definition-list">
      <div><dt>${t('general.dispositivo')}</dt><dd>${escapeHtml(device?.nombre ?? '—')}</dd></div>
      <div><dt>${t('auditorias.origen')}</dt><dd>${escapeHtml(configuration.nombreFuente)}</dd></div>
      <div><dt>${t('auditorias.formato')}</dt><dd>${escapeHtml(configuration.formato)}</dd></div>
      <div><dt>${t('baselines.versionDetalle')}</dt><dd>v${configuration.version}</dd></div>
      <div><dt>${t('auditorias.importo')}</dt><dd>${escapeHtml(configuration.usuarioResponsable)}</dd></div>
      <div><dt>${t('general.baseline')}</dt><dd>${escapeHtml(baseline?.nombre ?? t('auditorias.sinBaselineAplicable'))}</dd></div>
      <div><dt>${t('auditorias.reglasActivas')}</dt><dd>${ruleCount}</dd></div>
    </dl>
    <div class="config-preview">
      <span>${t('auditorias.vistaPrevia')}</span>
      <pre>${escapeHtml(preview)}</pre>
    </div>
    <div id="config-file-status" class="file-integrity">
      <span id="config-file-message">${t('auditorias.archivoOriginal')}</span>
      <button id="config-file-download" class="button button-ghost button-small" type="button">${t('auditorias.descargarOriginal')}</button>
    </div>
    ${canOperate ? `
      <button id="execute-selected-audit" class="button button-primary button-full" type="button"
        ${!baseline || ruleCount === 0 ? 'disabled' : ''}>
        ▶ ${t('auditorias.ejecutar')}
      </button>` : ''}
    ${!baseline ? `<div class="alert alert-error">${t('auditorias.sinBaselineActiva')}</div>` : ''}
  `);

  document.querySelector<HTMLButtonElement>('#execute-selected-audit')?.addEventListener('click', () => executeAudit(configuration.id));
  document.querySelector<HTMLButtonElement>('#config-file-download')?.addEventListener('click', () => downloadOriginal(configuration.id));
};

const selectAudit = async (id: number) => {
  selectedAuditId = id;
  selectedConfigurationId = null;
  renderConfigurations();
  renderAudits();

  const response = await auditFetch(`/api/audits/${id}`);
  if (!response.ok) {
    showMessage('error', await readApiError(response));
    return;
  }

  const detail = await response.json() as AuditDetail;
  renderAuditDetail(detail);
};

const renderAuditDetail = (detail: AuditDetail) => {
  const audit = detail.auditoria;
  const device = deviceById(audit.dispositivoId);
  const baseline = baselineById(audit.baselineId);
  const evaluations = detail.evaluaciones.map((evaluation) => `
    <div class="evaluation-item ${evaluation.cumple ? 'is-pass' : 'is-fail'}">
      <div>
        <code>${escapeHtml(evaluation.codigoRegla)}</code>
        <strong>${escapeHtml(evaluation.nombreRegla)}</strong>
      </div>
      <span class="status-badge ${evaluation.cumple ? 'status-ready' : 'status-strong'}">
        ${t(evaluation.cumple ? 'auditorias.cumple' : 'auditorias.hallazgo')}
      </span>
      <small>${escapeHtml(evidencia(evaluation.evidencia))}</small>
    </div>
  `).join('');

  showInspector(`
    <div class="inspector-identity">
      <span class="inspector-code">${code('AUD', audit.id)}</span>
      <h3>${escapeHtml(device?.identificador ?? t('auditorias.dispositivoNumero', { id: audit.dispositivoId }))}</h3>
      <span class="status-badge ${audit.resultado === 'CUMPLE' ? 'status-ready' : 'status-strong'}">${escapeHtml(etiqueta('resultado', audit.resultado))}</span>
    </div>
    <dl class="technical-definition-list">
      <div><dt>${t('general.baseline')}</dt><dd>${escapeHtml(baseline?.nombre ?? `#${audit.baselineId}`)}</dd></div>
      <div><dt>${t('auditorias.configuracion')}</dt><dd>${code('CFG', audit.configuracionId)}</dd></div>
      <div><dt>${t('auditorias.reglasEvaluadas')}</dt><dd>${audit.totalReglas}</dd></div>
      <div><dt>${t('auditorias.reglasCumplidas')}</dt><dd>${audit.reglasCumplidas}</dd></div>
      <div><dt>${t('auditorias.hallazgos')}</dt><dd>${audit.totalHallazgos}</dd></div>
      <div><dt>${t('auditorias.severidadMaxima')}</dt><dd>${escapeHtml(severityCode(audit.severidadMaxima))}</dd></div>
      <div><dt>${t('auditorias.ejecuto')}</dt><dd>${escapeHtml(audit.ejecutadoPor)}</dd></div>
      <div><dt>${t('bitacoras.fecha')}</dt><dd>${escapeHtml(formatDate(audit.fechaEjecucion))}</dd></div>
    </dl>
    <div class="evaluation-list">
      <span class="eyebrow">${t('auditorias.evaluacionReglas')}</span>
      ${evaluations}
    </div>
    ${audit.totalHallazgos > 0 ? `<a class="button button-ghost button-full" href="/results?auditoria=${audit.id}">${t('auditorias.verHallazgos')}</a>` : ''}
    <a class="button button-ghost button-full" href="/history?auditoria=${audit.id}">${t('auditorias.verHistorial')}</a>
  `);
};

const executeAudit = async (configurationId: number) => {
  const button = document.querySelector<HTMLButtonElement>('#execute-selected-audit');
  if (button) button.disabled = true;

  try {
    const response = await auditFetch('/api/audits', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ configuracionId: configurationId }),
    });

    if (!response.ok) throw new Error(await readApiError(response));

    const detail = await response.json() as AuditDetail;
    showMessage(
      'success',
      t('auditorias.finalizada', { codigo: code('AUD', detail.auditoria.id), hallazgos: plural(detail.auditoria.totalHallazgos, 'informe.hallazgo') }),
    );
    await refreshAll();
    selectedAuditId = detail.auditoria.id;
    selectedConfigurationId = null;
    renderConfigurations();
    renderAudits();
    renderAuditDetail(detail);
  } catch (error) {
    showMessage('error', error instanceof Error ? error.message : t('auditorias.errorEjecutar'));
  } finally {
    if (button) button.disabled = false;
  }
};

const openDialog = () => {
  configForm?.reset();
  configDialog?.showModal();
};

const closeDialog = () => configDialog?.close();

openConfigButton?.addEventListener('click', openDialog);
document.querySelector('#close-config-dialog')?.addEventListener('click', closeDialog);
document.querySelector('#cancel-config')?.addEventListener('click', closeDialog);

configFile?.addEventListener('change', async () => {
  const file = configFile.files?.[0];
  if (!file || !configContent) return;
  configContent.value = await file.text();
});

configForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const deviceId = Number(configDevice?.value);
  const content = configContent?.value ?? '';
  const file = configFile?.files?.[0];
  const extension = file?.name.split('.').pop()?.toLowerCase();
  const formato = extension === 'cfg' ? 'CFG' : extension === 'txt' ? 'TXT' : 'TEXTO';
  const nombreFuente = file?.name ?? 'entrada-manual';

  try {
    const response = await auditFetch(`/api/devices/${deviceId}/configurations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contenido: content,
        formato,
        nombreFuente,
      }),
    });

    if (!response.ok) throw new Error(await readApiError(response));

    const created = await response.json() as DeviceConfiguration;
    closeDialog();
    showMessage('success', t('auditorias.importada', { codigo: code('CFG', created.id), version: created.version }));
    await refreshAll();
    selectConfiguration(created.id);
  } catch (error) {
    showMessage('error', error instanceof Error ? error.message : t('auditorias.errorImportar'));
  }
});

searchInput?.addEventListener('input', renderConfigurations);

refreshAll().catch((error) => {
  showMessage('error', error instanceof Error ? error.message : t('auditorias.errorCargar'));
});
