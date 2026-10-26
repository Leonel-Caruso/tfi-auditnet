// Baselines: alta, versiones, descripción y activación.
import { auditFetch, authFetch } from '../lib/auth';
import {
  jsonRequest,
  readApiError,
  type BaselineRule,
  type ConfigurationBaseline,
  type DeviceType,
  type Organization,
} from '../lib/api';
import { plural, recordStatusCode } from '../lib/format';
import { t } from '../lib/i18n';
import { confirmAction, notify, setText } from '../lib/ui';

const state = {
  baselines: [] as ConfigurationBaseline[],
  rules: [] as BaselineRule[],
  deviceTypes: [] as DeviceType[],
  organizations: [] as Organization[],
  current: null as ConfigurationBaseline | null,
  changeMode: 'description' as 'description' | 'version',
};

const body = document.querySelector<HTMLTableSectionElement>('#baselines-body');
const empty = document.querySelector<HTMLElement>('#baselines-empty');
const count = document.querySelector<HTMLElement>('#baseline-count');
const search = document.querySelector<HTMLInputElement>('#baseline-search');
const statusFilter = document.querySelector<HTMLSelectElement>('#baseline-status-filter');
const dialog = document.querySelector<HTMLDialogElement>('#baseline-dialog');
const form = document.querySelector<HTMLFormElement>('#baseline-form');
const formError = document.querySelector<HTMLElement>('#baseline-form-error');
const typeSelect = document.querySelector<HTMLSelectElement>('#baseline-device-type');
const organizationSelect = document.querySelector<HTMLSelectElement>('#baseline-organization');
const changeDialog = document.querySelector<HTMLDialogElement>('#baseline-change-dialog');
const changeDescription = document.querySelector<HTMLTextAreaElement>('#baseline-change-description');
const changeError = document.querySelector<HTMLElement>('#baseline-change-error');

const showMessage = (text: string, kind: 'success' | 'error' = 'success') => notify('#baseline-message', text, kind, 5200);

const code = (baseline: ConfigurationBaseline) => `BL-${String(baseline.id).padStart(3, '0')}`;
const typeFor = (id: number) => state.deviceTypes.find((type) => type.id === id);
const organizationFor = (id: number) => state.organizations.find((org) => org.id === id);
const activeRulesFor = (baselineId: number) =>
  state.rules.filter((rule) => rule.baselineId === baselineId && rule.estado === 'ACTIVA');
const versionsOf = (baseline: ConfigurationBaseline) => state.baselines
  .filter((item) => item.organizacionId === baseline.organizacionId
    && item.nombre.toLowerCase() === baseline.nombre.toLowerCase())
  .sort((a, b) => b.version - a.version);
const statusClass = (estado: ConfigurationBaseline['estado']) => estado === 'ACTIVO' ? 'status-ready' : 'status-next';

function filteredBaselines(): ConfigurationBaseline[] {
  const query = search?.value.trim().toLowerCase() ?? '';
  const status = statusFilter?.value ?? '';
  return state.baselines.filter((baseline) => {
    const type = typeFor(baseline.tipoDispositivoId);
    const org = organizationFor(baseline.organizacionId);
    const haystack = [baseline.nombre, baseline.descripcion, type?.nombre ?? '', org?.razonSocial ?? ''].join(' ').toLowerCase();
    return (!query || haystack.includes(query)) && (!status || baseline.estado === status);
  });
}

function buildRow(baseline: ConfigurationBaseline): HTMLTableRowElement {
  const row = document.createElement('tr');
  row.className = 'policy-row';
  row.classList.toggle('is-selected', state.current?.id === baseline.id);
  row.tabIndex = 0;
  row.dataset.baselineId = String(baseline.id);
  const org = organizationFor(baseline.organizacionId);
  const type = typeFor(baseline.tipoDispositivoId);

  const name = document.createElement('td');
  const strong = document.createElement('strong');
  strong.textContent = baseline.nombre;
  const small = document.createElement('small');
  small.textContent = code(baseline);
  name.append(strong, small);

  const organization = document.createElement('td');
  organization.textContent = org?.razonSocial ?? t('dispositivos.organizacionNumero', { id: baseline.organizacionId });

  const deviceType = document.createElement('td');
  deviceType.textContent = type?.nombre ?? `#${baseline.tipoDispositivoId}`;

  const version = document.createElement('td');
  const versionCode = document.createElement('code');
  versionCode.textContent = `v${baseline.version}`;
  version.appendChild(versionCode);

  const rules = document.createElement('td');
  rules.textContent = String(activeRulesFor(baseline.id).length);

  const usage = document.createElement('td');
  const usageText = document.createElement('small');
  usageText.textContent = baseline.auditorias > 0 ? plural(baseline.auditorias, 'tablero.auditoria') : t('baselines.sinUso');
  usage.appendChild(usageText);

  const status = document.createElement('td');
  const badge = document.createElement('span');
  badge.className = `status-badge ${statusClass(baseline.estado)}`;
  badge.textContent = recordStatusCode(baseline.estado);
  status.appendChild(badge);

  row.append(name, organization, deviceType, version, rules, usage, status);
  const inspect = () => void inspectBaseline(baseline.id);
  row.addEventListener('click', inspect);
  row.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') inspect();
  });
  return row;
}

function render(): void {
  const baselines = filteredBaselines();
  if (count) count.textContent = plural(state.baselines.length, 'baselines.cantidad');
  if (!body || !empty) return;
  body.replaceChildren(...baselines.map(buildRow));
  empty.classList.toggle('is-hidden', baselines.length > 0);
  document.querySelector('.policy-table')?.classList.toggle('is-hidden', baselines.length === 0);
}

function renderVersions(baseline: ConfigurationBaseline): void {
  const container = document.querySelector('#baseline-inspector-versions');
  if (!container) return;
  container.replaceChildren(...versionsOf(baseline).map((version) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'baseline-version-item';
    button.classList.toggle('is-current', version.id === baseline.id);
    const label = document.createElement('code');
    label.textContent = `v${version.version}`;
    const detail = document.createElement('span');
    detail.textContent = t('baselines.versionResumen', { estado: recordStatusCode(version.estado).toLowerCase(), n: version.auditorias });
    button.append(label, detail);
    button.addEventListener('click', () => void inspectBaseline(version.id));
    return button;
  }));
}

async function inspectBaseline(id: number): Promise<void> {
  const [baselineResponse, rulesResponse] = await Promise.all([
    auditFetch(`/api/baselines/${id}`),
    auditFetch(`/api/baselines/${id}/rules`),
  ]);
  if (!baselineResponse.ok) return showMessage(await readApiError(baselineResponse), 'error');
  if (!rulesResponse.ok) return showMessage(await readApiError(rulesResponse), 'error');

  const baseline = await baselineResponse.json() as ConfigurationBaseline;
  const rules = await rulesResponse.json() as BaselineRule[];
  state.current = baseline;
  document.querySelectorAll<HTMLElement>('.policy-row').forEach((row) => {
    row.classList.toggle('is-selected', row.dataset.baselineId === String(baseline.id));
  });

  const org = organizationFor(baseline.organizacionId);
  const type = typeFor(baseline.tipoDispositivoId);
  const activeRules = rules.filter((rule) => rule.estado === 'ACTIVA').length;
  const frozen = baseline.auditorias > 0;

  setText('#baseline-inspector-code', code(baseline));
  setText('#baseline-inspector-name', baseline.nombre);
  setText('#baseline-inspector-status', recordStatusCode(baseline.estado));
  setText('#baseline-inspector-description', baseline.descripcion);
  setText('#baseline-inspector-organization', org?.razonSocial ?? `#${baseline.organizacionId}`);
  setText('#baseline-inspector-type', type ? `${type.nombre} / ${type.familia}` : `#${baseline.tipoDispositivoId}`);
  setText('#baseline-inspector-version', `v${baseline.version}`);
  setText('#baseline-inspector-rules', t('baselines.reglasActivas', { activas: activeRules, total: rules.length }));
  setText('#baseline-inspector-audits', String(baseline.auditorias));
  setText('#baseline-inspector-id', String(baseline.id));
  setText('#baseline-inspector-policy', t(frozen ? 'baselines.reglasCongeladas' : 'baselines.baselineReglas'));
  setText('#baseline-inspector-policy-text', t(frozen
    ? 'baselines.congeladaAyuda'
    : baseline.estado === 'ACTIVO' ? 'baselines.activaAyuda' : 'baselines.inactivaAyuda'));
  setText('#baseline-toggle', t(baseline.estado === 'ACTIVO' ? 'baselines.desactivar' : 'baselines.activar'));

  const status = document.querySelector('#baseline-inspector-status');
  if (status) status.className = `status-badge ${statusClass(baseline.estado)}`;
  renderVersions(baseline);
  document.querySelector('#baseline-inspector-empty')?.classList.add('is-hidden');
  document.querySelector('#baseline-inspector-content')?.classList.remove('is-hidden');
}

async function load(): Promise<void> {
  const [baselineResponse, rulesResponse, typesResponse, organizationsResponse] = await Promise.all([
    auditFetch('/api/baselines'),
    auditFetch('/api/rules'),
    auditFetch('/api/device-types'),
    authFetch('/api/client-organizations'),
  ]);
  for (const response of [baselineResponse, rulesResponse, typesResponse, organizationsResponse]) {
    if (!response.ok) throw new Error(await readApiError(response));
  }
  state.baselines = await baselineResponse.json() as ConfigurationBaseline[];
  state.rules = await rulesResponse.json() as BaselineRule[];
  state.deviceTypes = (await typesResponse.json() as DeviceType[]).filter((type) => type.activo);
  state.organizations = await organizationsResponse.json() as Organization[];

  typeSelect?.replaceChildren(new Option(t('dispositivos.seleccionarTipo'), ''));
  state.deviceTypes.forEach((type) => typeSelect?.add(new Option(`${type.nombre} · ${type.fabricante} · ${type.familia}`, String(type.id))));
  organizationSelect?.replaceChildren(new Option(t('dispositivos.seleccionarOrganizacion'), ''));
  state.organizations.forEach((org) => organizationSelect?.add(new Option(org.razonSocial, String(org.id))));
  if (organizationSelect && state.organizations.length === 1) organizationSelect.value = String(state.organizations[0].id);
  render();
}

async function reloadAndInspect(id: number): Promise<void> {
  await load();
  await inspectBaseline(id);
}

function openDialog(): void {
  form?.reset();
  formError?.classList.add('is-hidden');
  if (organizationSelect && state.organizations.length === 1) organizationSelect.value = String(state.organizations[0].id);
  dialog?.showModal();
}

document.querySelector('#open-baseline-dialog')?.addEventListener('click', openDialog);
document.querySelector('#close-baseline-dialog')?.addEventListener('click', () => dialog?.close());
document.querySelector('#cancel-baseline-dialog')?.addEventListener('click', () => dialog?.close());
document.querySelectorAll<HTMLButtonElement>('[data-close-dialog]').forEach((button) => {
  button.addEventListener('click', () => button.closest('dialog')?.close());
});
search?.addEventListener('input', render);
statusFilter?.addEventListener('change', render);

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  formError?.classList.add('is-hidden');
  const payload = {
    nombre: (document.querySelector<HTMLInputElement>('#baseline-name')?.value ?? '').trim(),
    descripcion: (document.querySelector<HTMLTextAreaElement>('#baseline-description')?.value ?? '').trim(),
    tipoDispositivoId: Number(typeSelect?.value),
    organizacionId: Number(organizationSelect?.value),
  };

  const response = await auditFetch('/api/baselines', jsonRequest('POST', payload));
  if (!response.ok) {
    if (formError) {
      formError.textContent = await readApiError(response);
      formError.classList.remove('is-hidden');
    }
    return;
  }

  const created = await response.json() as ConfigurationBaseline;
  dialog?.close();
  showMessage(t('baselines.creada', { nombre: created.nombre }));
  await reloadAndInspect(created.id);
});

// Editar descripción / nueva versión
function openChangeDialog(mode: 'description' | 'version'): void {
  const baseline = state.current;
  if (!baseline || !changeDescription) return;
  state.changeMode = mode;
  changeError?.classList.add('is-hidden');
  changeDescription.value = baseline.descripcion;

  const next = Math.max(...versionsOf(baseline).map((item) => item.version)) + 1;
  const copied = activeRulesFor(baseline.id).length;
  const current = state.baselines.find((item) => item.estado === 'ACTIVO'
    && item.organizacionId === baseline.organizacionId
    && item.tipoDispositivoId === baseline.tipoDispositivoId);

  if (mode === 'description') {
    setText('#baseline-change-eyebrow', t('baselines.editarBaseline'));
    setText('#baseline-change-title', t('baselines.editarTitulo', { nombre: baseline.nombre, version: baseline.version }));
    setText('#baseline-change-subtitle', t('baselines.descripcionNoVersiona'));
    setText('#baseline-change-code', 'operacion = MODIFICACION');
    setText('#baseline-change-text', t('baselines.cambioBitacora'));
    setText('#baseline-change-submit', t('baselines.guardarDescripcion'));
  } else {
    setText('#baseline-change-eyebrow', t('baselines.nuevaVersionBaseline'));
    setText('#baseline-change-title', t('baselines.nuevaVersionTitulo', { nombre: baseline.nombre, version: next }));
    setText('#baseline-change-subtitle', t('baselines.generaDesde', { version: baseline.version }));
    setText('#baseline-change-code', t('baselines.reglasCopiadas', { n: copied }));
    setText('#baseline-change-text', current
      ? t('baselines.reemplaza', { version: next, nombre: current.nombre, anterior: current.version })
      : t('baselines.quedaActiva', { version: next }));
    setText('#baseline-change-submit', t('baselines.generarVersion', { version: next }));
  }
  changeDialog?.showModal();
}

document.querySelector('#baseline-edit')?.addEventListener('click', () => openChangeDialog('description'));
document.querySelector('#baseline-version')?.addEventListener('click', () => openChangeDialog('version'));

document.querySelector<HTMLFormElement>('#baseline-change-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const baseline = state.current;
  if (!baseline) return;
  changeError?.classList.add('is-hidden');
  const descripcion = (changeDescription?.value ?? '').trim();

  const response = state.changeMode === 'description'
    ? await auditFetch(`/api/baselines/${baseline.id}`, jsonRequest('PUT', { descripcion }))
    : await auditFetch(`/api/baselines/${baseline.id}/versions`, jsonRequest('POST', { descripcion }));
  if (!response.ok) {
    if (changeError) {
      changeError.textContent = await readApiError(response);
      changeError.classList.remove('is-hidden');
    }
    return;
  }
  const saved = await response.json() as ConfigurationBaseline;
  changeDialog?.close();
  showMessage(state.changeMode === 'description'
    ? t('baselines.descripcionActualizada', { nombre: saved.nombre, version: saved.version })
    : t('baselines.versionGenerada', { nombre: saved.nombre, version: saved.version }));
  await reloadAndInspect(saved.id);
});

// Activar / desactivar
document.querySelector('#baseline-toggle')?.addEventListener('click', async () => {
  const baseline = state.current;
  if (!baseline) return;
  const deactivate = baseline.estado === 'ACTIVO';
  const question = deactivate
    ? t('baselines.confirmarDesactivar', { nombre: baseline.nombre, version: baseline.version })
    : t('baselines.confirmarActivar', { nombre: baseline.nombre, version: baseline.version });
  if (!await confirmAction(t('general.confirmasOperacion'), question, t(deactivate ? 'baselines.desactivar' : 'baselines.activar'))) return;

  const response = await auditFetch(`/api/baselines/${baseline.id}/status`,
    jsonRequest('PATCH', { estado: deactivate ? 'INACTIVO' : 'ACTIVO' }));
  if (!response.ok) {
    showMessage(await readApiError(response), 'error');
    return;
  }
  const saved = await response.json() as ConfigurationBaseline;
  showMessage(t('baselines.ahoraEsta', { nombre: saved.nombre, version: saved.version, estado: recordStatusCode(saved.estado) }));
  await reloadAndInspect(saved.id);
});

load().catch((error) => {
  showMessage(error instanceof Error ? error.message : t('baselines.errorCargar'), 'error');
});
