// Bitácoras de sistema, de transacciones y de excepciones.
import { authFetch } from '../lib/auth';
import { readApiError } from '../lib/api';
import { code as codeBase, escapeHtml, formatDate as formatDateBase } from '../lib/format';
import { t, type Clave } from '../lib/i18n';
import { crearPaginador } from '../lib/paginador';
import type {
  ExceptionLogRecord,
  Organization,
  SystemLogRecord,
  TransactionLogRecord,
} from '../lib/api';
import { show } from '../lib/ui';

type Tab = 'sistema' | 'transacciones' | 'excepciones';
type LogRecord = SystemLogRecord | TransactionLogRecord | ExceptionLogRecord;

type TabConfig = {
  title: Clave;
  eyebrow: Clave;
  endpoint: string; // ruta de la API (no se muestra en pantalla)
  params: string[];
  columns: Clave[];
};

const TABS: Record<Tab, TabConfig> = {
  sistema: {
    title: 'bitacoras.auditoriaSistema',
    eyebrow: 'bitacoras.bitacoraSistema',
    endpoint: '/api/admin/bitacoras/sistema',
    params: ['evento', 'resultado', 'actor', 'desde', 'hasta', 'limite'],
    columns: ['bitacoras.colId', 'general.colFecha', 'bitacoras.colEvento', 'general.colResultado', 'bitacoras.colActor',
      'bitacoras.colOrigen', 'bitacoras.colSeguimiento'],
  },
  transacciones: {
    title: 'bitacoras.transacciones',
    eyebrow: 'bitacoras.bitacoraTransacciones',
    endpoint: '/api/admin/bitacoras/transacciones',
    params: ['entidad', 'entidadId', 'operacion', 'actor', 'desde', 'hasta', 'limite'],
    columns: ['bitacoras.colId', 'general.colFecha', 'bitacoras.colEntidad', 'bitacoras.colOperacion', 'bitacoras.colActor',
      'bitacoras.colServicio', 'bitacoras.colSeguimiento'],
  },
  excepciones: {
    title: 'bitacoras.excepciones',
    eyebrow: 'bitacoras.bitacoraExcepciones',
    endpoint: '/api/admin/bitacoras/excepciones',
    params: ['servicio', 'nivel', 'desde', 'hasta', 'limite'],
    columns: ['general.colFecha', 'bitacoras.colNivel', 'bitacoras.colExcepcion', 'bitacoras.colSolicitud', 'bitacoras.colHttp',
      'bitacoras.colServicio', 'bitacoras.colSeguimiento'],
  },
};

const isTab = (value: string): value is Tab => value in TABS;

const state: Record<Tab, { records: LogRecord[]; loaded: boolean }> = {
  sistema: { records: [], loaded: false },
  transacciones: { records: [], loaded: false },
  excepciones: { records: [], loaded: false },
};

const initialHash = window.location.hash.replace('#', '');
let activeTab: Tab = isTab(initialHash) ? initialHash : 'sistema';
let selectedIndex: number | null = null;
let organizations = new Map<number, string>();
let requestSequence = 0;

const form = document.querySelector<HTMLFormElement>('#log-filters');
const search = document.querySelector<HTMLInputElement>('#log-search');
// Los registros se traen hasta el "límite" elegido y se paginan en el navegador.
const paginador = crearPaginador(document.querySelector<HTMLElement>('#log-pager'), { alCambiar: () => render() });

// En las bitácoras la hora va con segundos y los números con cuatro cifras.
const formatDate = (value: string) => formatDateBase(value, true);
const code = (prefix: string, id: number | null | undefined) => codeBase(prefix, id, 4);

const shortCorrelation = (value: string | null) => (value ? value.slice(0, 8) : '—');

const shortType = (value: string) => value.split('.').pop() ?? value;

const orgLabel = (id: number | null) =>
  id == null ? '—' : `${organizations.get(id) ?? t('general.organizacion')} (#${id})`;

const prettyJson = (value: string | null) => {
  if (value == null) return null;
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

const resultBadge = (value: string) =>
  value === 'FALLO'
    ? `<span class="severity-badge severity-critical">FALLO</span>`
    : `<span class="status-badge status-ready">${escapeHtml(value)}</span>`;

const levelBadge = (value: string) =>
  `<span class="severity-badge ${value === 'ERROR' ? 'severity-critical' : 'severity-medium'}">${escapeHtml(value)}</span>`;

const showError = (message: string | null) => show('#log-error', message);

// Filtros: solo se envían los de la bitácora activa. Las fechas se cargan en hora local y se envían en UTC.
const buildQuery = (tab: Tab): string => {
  if (!form) return '';
  const data = new FormData(form);
  const params = new URLSearchParams();

  for (const name of TABS[tab].params) {
    const raw = String(data.get(name) ?? '').trim();
    if (!raw) continue;

    if (name === 'desde' || name === 'hasta') {
      const date = new Date(raw);
      if (Number.isNaN(date.getTime())) throw new Error(t('bitacoras.fechaInvalida', { campo: t(`general.${name}` as Clave) }));
      params.set(name, date.toISOString());
    } else if (name === 'entidadId') {
      if (!/^[1-9]\d*$/.test(raw)) throw new Error(t('bitacoras.idInvalido'));
      params.set(name, raw);
    } else {
      params.set(name, raw);
    }
  }

  const desde = params.get('desde');
  const hasta = params.get('hasta');
  if (desde && hasta && desde > hasta) {
    throw new Error(t('bitacoras.rangoInvalido'));
  }

  const query = params.toString();
  return query ? `?${query}` : '';
};

const syncFilterVisibility = () => {
  document.querySelectorAll<HTMLElement>('.log-filter[data-tabs]').forEach((field) => {
    const tabs = (field.dataset.tabs ?? '').split(',');
    field.classList.toggle('is-hidden', !tabs.includes(activeTab));
  });
};

// Tabla
const haystack = (record: LogRecord) =>
  Object.values(record).map((value) => String(value ?? '')).join(' ').toLowerCase();

const rowCells = (record: LogRecord): string[] => {
  if (activeTab === 'sistema') {
    const r = record as SystemLogRecord;
    return [
      `<code>${code('SYS', r.id)}</code>`,
      `<span class="log-nowrap">${escapeHtml(formatDate(r.fecha))}</span>`,
      `<strong>${escapeHtml(r.evento)}</strong>`,
      resultBadge(r.resultado),
      escapeHtml(r.actor ?? '—'),
      escapeHtml(r.origenIp ?? '—'),
      `<code title="${escapeHtml(r.correlacion)}">${escapeHtml(shortCorrelation(r.correlacion))}</code>`,
    ];
  }
  if (activeTab === 'transacciones') {
    const r = record as TransactionLogRecord;
    return [
      `<code>${code('TRX', r.id)}</code>`,
      `<span class="log-nowrap">${escapeHtml(formatDate(r.fecha))}</span>`,
      `<strong>${escapeHtml(r.entidad)}</strong><small>id ${escapeHtml(r.entidadId ?? '—')}</small>`,
      `<span class="status-badge status-strong">${escapeHtml(r.operacion)}</span>`,
      escapeHtml(r.actor),
      `<small>${escapeHtml(r.servicio)}</small>`,
      `<code title="${escapeHtml(r.correlacion)}">${escapeHtml(shortCorrelation(r.correlacion))}</code>`,
    ];
  }
  const r = record as ExceptionLogRecord;
  return [
    `<span class="log-nowrap">${escapeHtml(formatDate(r.fecha))}</span>`,
    levelBadge(r.nivel),
    `<strong title="${escapeHtml(r.tipo)}">${escapeHtml(shortType(r.tipo))}</strong><small>${escapeHtml(r.mensaje ?? '')}</small>`,
    `<small>${escapeHtml([r.metodoHttp, r.ruta].filter(Boolean).join(' ') || '—')}</small>`,
    `<code>${escapeHtml(r.estadoHttp)}</code>`,
    `<small>${escapeHtml(r.servicio)}</small>`,
    `<code title="${escapeHtml(r.correlacion)}">${escapeHtml(shortCorrelation(r.correlacion))}</code>`,
  ];
};

const render = () => {
  const head = document.querySelector<HTMLElement>('#log-head');
  const body = document.querySelector<HTMLElement>('#log-body');
  const empty = document.querySelector<HTMLElement>('#log-empty');
  const count = document.querySelector<HTMLElement>('#log-count');
  if (!head || !body) return;

  const config = TABS[activeTab];
  head.innerHTML = `<tr>${config.columns.map((column) => `<th>${escapeHtml(t(column))}</th>`).join('')}</tr>`;

  const term = search?.value.trim().toLowerCase() ?? '';
  const records = state[activeTab].records;
  const visible = records
    .map((record, index) => ({ record, index }))
    .filter(({ record }) => !term || haystack(record).includes(term));

  body.innerHTML = paginador.recortar(visible).map(({ record, index }) => `
    <tr class="finding-row ${index === selectedIndex ? 'is-selected' : ''}" data-index="${index}" tabindex="0">
      ${rowCells(record).map((cell) => `<td>${cell}</td>`).join('')}
    </tr>`).join('');

  if (count) {
    count.textContent = term && state[activeTab].loaded
      ? `${visible.length}/${records.length}`
      : state[activeTab].loaded ? String(records.length) : '—';
  }
  empty?.classList.toggle('is-hidden', !state[activeTab].loaded || visible.length > 0);

  body.querySelectorAll<HTMLElement>('[data-index]').forEach((row) => {
    const select = () => selectRecord(Number(row.dataset.index));
    row.addEventListener('click', select);
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        select();
      }
    });
  });
};

// Inspector
const definition = (label: string, value: unknown) =>
  `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value ?? '—')}</dd></div>`;

const block = (label: string, value: string | null, empty: string) => `
  <div class="config-preview">
    <span>${escapeHtml(label)}</span>
    ${value ? `<pre>${escapeHtml(value)}</pre>` : `<p class="log-muted">${escapeHtml(empty)}</p>`}
  </div>`;

const correlationAction = (value: string | null) => value ? `
  <div class="rule-expression-block">
    <span>${t('bitacoras.codigoSeguimiento')}</span>
    <code>${escapeHtml(value)}</code>
  </div>
  <button type="button" class="button button-ghost button-small button-full" data-follow="${escapeHtml(value)}">
    ${t('bitacoras.filtrarCodigo')}
  </button>` : '';

const inspectorHtml = (record: LogRecord): string => {
  if (activeTab === 'sistema') {
    const r = record as SystemLogRecord;
    return `
      <div class="inspector-identity">
        <span class="inspector-code">${code('SYS', r.id)}</span>
        <h3>${escapeHtml(r.evento)}</h3>
        ${resultBadge(r.resultado)}
      </div>
      <dl class="technical-definition-list">
        ${definition(t('bitacoras.fecha'), formatDate(r.fecha))}
        ${definition(t('bitacoras.actor'), r.actor)}
        ${definition(t('bitacoras.usuarioAfectado'), r.usuarioAfectadoId == null ? null : `#${r.usuarioAfectadoId}`)}
        ${definition(t('general.organizacion'), orgLabel(r.organizacionId))}
        ${definition(t('bitacoras.ipOrigen'), r.origenIp)}
        ${definition(t('bitacoras.navegador'), r.userAgent)}
      </dl>
      ${block(t('bitacoras.detalle'), r.detalle, t('bitacoras.sinDetalle'))}
      ${correlationAction(r.correlacion)}`;
  }
  if (activeTab === 'transacciones') {
    const r = record as TransactionLogRecord;
    return `
      <div class="inspector-identity">
        <span class="inspector-code">${code('TRX', r.id)}</span>
        <h3>${escapeHtml(r.entidad)} #${escapeHtml(r.entidadId ?? '—')}</h3>
        <span class="status-badge status-strong">${escapeHtml(r.operacion)}</span>
      </div>
      <dl class="technical-definition-list">
        ${definition(t('bitacoras.fecha'), formatDate(r.fecha))}
        ${definition(t('bitacoras.servicio'), r.servicio)}
        ${definition(t('bitacoras.actor'), r.actor)}
        ${definition(t('general.organizacion'), orgLabel(r.organizacionId))}
      </dl>
      ${r.detalle ? block(t('bitacoras.detalle'), r.detalle, '') : ''}
      ${block(t('bitacoras.valorAnterior'), prettyJson(r.valorAnterior),
        t(r.operacion === 'ALTA' ? 'bitacoras.altaSinAnterior' : 'bitacoras.sinValorAnterior'))}
      ${block(t('bitacoras.valorNuevo'), prettyJson(r.valorNuevo), t('bitacoras.sinValorNuevo'))}
      ${correlationAction(r.correlacion)}`;
  }
  const r = record as ExceptionLogRecord;
  return `
    <div class="inspector-identity">
      <span class="inspector-code">${escapeHtml(r.servicio)}</span>
      <h3>${escapeHtml(shortType(r.tipo))}</h3>
      ${levelBadge(r.nivel)}
    </div>
    <dl class="technical-definition-list">
      ${definition(t('bitacoras.fecha'), formatDate(r.fecha))}
      ${definition(t('bitacoras.tipo'), r.tipo)}
      ${definition(t('bitacoras.solicitud'), [r.metodoHttp, r.ruta].filter(Boolean).join(' ') || null)}
      ${definition(t('bitacoras.codigoHttp'), r.estadoHttp)}
      ${definition(t('bitacoras.usuario'), r.usuario)}
    </dl>
    <div class="finding-evidence-block">
      <span>${t('bitacoras.mensaje')}</span>
      <p>${escapeHtml(r.mensaje ?? t('bitacoras.sinMensaje'))}</p>
    </div>
    ${block(t('bitacoras.traza'), r.traza, t('bitacoras.sinTraza'))}
    ${correlationAction(r.correlacion)}`;
};

const clearInspector = () => {
  selectedIndex = null;
  document.querySelector('#log-inspector-empty')?.classList.remove('is-hidden');
  const content = document.querySelector<HTMLElement>('#log-inspector-content');
  if (content) {
    content.innerHTML = '';
    content.classList.add('is-hidden');
  }
};

const selectRecord = (index: number) => {
  const record = state[activeTab].records[index];
  if (!record) return;
  selectedIndex = index;
  render();

  document.querySelector('#log-inspector-empty')?.classList.add('is-hidden');
  const content = document.querySelector<HTMLElement>('#log-inspector-content');
  if (!content) return;
  content.innerHTML = inspectorHtml(record);
  content.classList.remove('is-hidden');

  content.querySelector<HTMLButtonElement>('[data-follow]')?.addEventListener('click', (event) => {
    const value = (event.currentTarget as HTMLButtonElement).dataset.follow ?? '';
    if (search) search.value = value;
    paginador.reiniciar();
    render();
    search?.focus();
  });
};

// Carga
const setLoading = (loading: boolean) => {
  document.querySelector('#log-loading')?.classList.toggle('is-hidden', !loading);
  const submit = document.querySelector<HTMLButtonElement>('#log-submit');
  if (submit) submit.disabled = loading;
};

const load = async (tab: Tab) => {
  let query: string;
  try {
    query = buildQuery(tab);
  } catch (error) {
    showError(error instanceof Error ? error.message : t('bitacoras.filtrosInvalidos'));
    return;
  }

  const sequence = ++requestSequence;
  showError(null);
  setLoading(true);
  try {
    const response = await authFetch(`${TABS[tab].endpoint}${query}`);
    if (sequence !== requestSequence) return;
    if (!response.ok) {
      showError(response.status === 403
        ? t('bitacoras.soloAdministrador')
        : await readApiError(response));
      return;
    }
    state[tab].records = await response.json() as LogRecord[];
    state[tab].loaded = true;
    if (tab === activeTab) paginador.reiniciar();
  } catch (error) {
    if (sequence === requestSequence) {
      showError(error instanceof Error ? error.message : t('bitacoras.errorConsultar'));
    }
  } finally {
    if (sequence === requestSequence) {
      setLoading(false);
      if (tab === activeTab) {
        clearInspector();
        render();
      }
    }
  }
};

const loadOrganizations = async () => {
  try {
    const response = await authFetch('/api/client-organizations');
    if (!response.ok) return;
    const list = await response.json() as Organization[];
    organizations = new Map(list.map((item) => [item.id, item.razonSocial]));
  } catch {
    // Solo se usa para mostrar nombres; sin esto se muestra el id.
  }
};

// Pestañas
const tabButtons = Array.from(document.querySelectorAll<HTMLButtonElement>('.log-tab'));

const activate = (tab: Tab, focus = false) => {
  activeTab = tab;
  const config = TABS[tab];

  tabButtons.forEach((button) => {
    const selected = button.dataset.tab === tab;
    button.setAttribute('aria-selected', String(selected));
    button.tabIndex = selected ? 0 : -1;
    button.classList.toggle('is-active', selected);
    if (selected && focus) button.focus();
  });

  document.querySelector('#log-panel')?.setAttribute('aria-labelledby', `tab-${tab}`);
  const title = document.querySelector('#log-title');
  const eyebrow = document.querySelector('#log-eyebrow');
  if (title) title.textContent = t(config.title);
  if (eyebrow) eyebrow.textContent = t(config.eyebrow);

  history.replaceState(null, '', `#${tab}`);
  syncFilterVisibility();
  clearInspector();
  paginador.reiniciar();
  render();
  if (!state[tab].loaded) load(tab);
};

tabButtons.forEach((button, position) => {
  button.addEventListener('click', () => {
    const tab = button.dataset.tab ?? '';
    if (isTab(tab)) activate(tab);
  });
  button.addEventListener('keydown', (event) => {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
    event.preventDefault();
    const next = tabButtons[(position + (event.key === 'ArrowRight' ? 1 : tabButtons.length - 1)) % tabButtons.length];
    const tab = next.dataset.tab ?? '';
    if (isTab(tab)) activate(tab, true);
  });
});

form?.addEventListener('submit', (event) => {
  event.preventDefault();
  // Los filtros cambiaron: las otras pestañas se vuelven a consultar al abrirlas.
  (Object.keys(state) as Tab[]).forEach((tab) => { if (tab !== activeTab) state[tab].loaded = false; });
  load(activeTab);
});

document.querySelector('#log-clear')?.addEventListener('click', () => {
  form?.reset();
  if (search) search.value = '';
  (Object.keys(state) as Tab[]).forEach((tab) => { state[tab].loaded = false; });
  load(activeTab);
});

search?.addEventListener('input', () => {
  paginador.reiniciar();
  render();
});

// Filtros que llegan desde el panel de administración (ej: /logs?evento=CUENTA_BLOQUEADA#sistema).
const urlFilters = new URLSearchParams(window.location.search);
['evento', 'resultado'].forEach((name) => {
  const value = urlFilters.get(name);
  const field = form?.querySelector<HTMLSelectElement>(`[name=${name}]`);
  if (field && value && Array.from(field.options).some((option) => option.value === value)) field.value = value;
});
const desdeUrl = urlFilters.get('desde');
const desdeField = form?.querySelector<HTMLInputElement>('[name=desde]');
if (desdeField && desdeUrl && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(desdeUrl)) desdeField.value = desdeUrl;

activate(activeTab);
loadOrganizations();
