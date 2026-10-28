// Reglas: alta, edición y activación de los controles de cada baseline.
import { auditFetch, authFetch } from '../lib/auth';
import {
  jsonRequest,
  readApiError,
  type BaselineRule,
  type ConfigurationBaseline,
  type Organization,
  type RuleType,
} from '../lib/api';
import { plural, severityCode } from '../lib/format';
import { etiqueta, t } from '../lib/i18n';
import { confirmAction, notify, setText } from '../lib/ui';

const state = {
  rules: [] as BaselineRule[],
  baselines: [] as ConfigurationBaseline[],
  organizations: [] as Organization[],
  current: null as BaselineRule | null,
  editing: null as BaselineRule | null,
};

const body = document.querySelector<HTMLTableSectionElement>('#rules-body');
const empty = document.querySelector<HTMLElement>('#rules-empty');
const count = document.querySelector<HTMLElement>('#rule-count');
const search = document.querySelector<HTMLInputElement>('#rule-search');
const severityFilter = document.querySelector<HTMLSelectElement>('#rule-severity-filter');
const statusFilter = document.querySelector<HTMLSelectElement>('#rule-status-filter');
const dialog = document.querySelector<HTMLDialogElement>('#rule-dialog');
const form = document.querySelector<HTMLFormElement>('#rule-form');
const formError = document.querySelector<HTMLElement>('#rule-form-error');
const baselineSelect = document.querySelector<HTMLSelectElement>('#rule-baseline');
const typeSelect = document.querySelector<HTMLSelectElement>('#rule-type');
const codeInput = document.querySelector<HTMLInputElement>('#rule-code');
const expectedInput = document.querySelector<HTMLInputElement>('#rule-expected');

const baselineFor = (id: number) => state.baselines.find((baseline) => baseline.id === id);
const organizationFor = (id: number) => state.organizations.find((org) => org.id === id);

const fieldValue = (selector: string) =>
  (document.querySelector<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(selector)?.value ?? '').trim();

const showMessage = (text: string, kind: 'success' | 'error' = 'success') => notify('#rule-message', text, kind, 5200);

function severityClass(value: BaselineRule['severidad']): string {
  if (value === 'CRITICA') return 'severity-critical';
  if (value === 'ALTA') return 'severity-high';
  if (value === 'MEDIA') return 'severity-medium';
  return 'severity-low';
}

const conditionLabel = (value: RuleType) => etiqueta('condicion', value);
const ruleStatus = (value: string) => etiqueta('regla', value);

const conditionText = (rule: BaselineRule) =>
  rule.tipo === 'VALOR_ESPERADO' ? `${rule.patron} = ${rule.valorEsperado ?? ''}` : rule.patron;

const isFrozen = (rule: BaselineRule) => (baselineFor(rule.baselineId)?.auditorias ?? 0) > 0;

function baselineLabel(baseline: ConfigurationBaseline): string {
  const org = organizationFor(baseline.organizacionId);
  return `${baseline.nombre} v${baseline.version} · ${org?.razonSocial ?? ''}`;
}

function filteredRules(): BaselineRule[] {
  const query = search?.value.trim().toLowerCase() ?? '';
  const severity = severityFilter?.value ?? '';
  const status = statusFilter?.value ?? '';
  return state.rules.filter((rule) => {
    const baseline = baselineFor(rule.baselineId);
    const haystack = [rule.codigo, rule.nombre, rule.descripcion, conditionText(rule), rule.impacto, baseline?.nombre ?? '']
      .join(' ').toLowerCase();
    return (!query || haystack.includes(query))
      && (!severity || rule.severidad === severity)
      && (!status || rule.estado === status);
  });
}

function buildRow(rule: BaselineRule): HTMLTableRowElement {
  const row = document.createElement('tr');
  row.className = 'policy-row';
  row.classList.toggle('is-selected', state.current?.id === rule.id);
  row.tabIndex = 0;
  row.dataset.ruleId = String(rule.id);
  const baseline = baselineFor(rule.baselineId);

  const code = document.createElement('td');
  const codeNode = document.createElement('code');
  codeNode.textContent = rule.codigo;
  code.appendChild(codeNode);

  const name = document.createElement('td');
  const strong = document.createElement('strong');
  strong.textContent = rule.nombre;
  const small = document.createElement('small');
  small.textContent = conditionText(rule);
  name.append(strong, small);

  const baselineCell = document.createElement('td');
  baselineCell.textContent = baseline ? `${baseline.nombre} v${baseline.version}` : `#${rule.baselineId}`;

  const type = document.createElement('td');
  const typeCode = document.createElement('code');
  typeCode.textContent = conditionLabel(rule.tipo);
  type.appendChild(typeCode);

  const severity = document.createElement('td');
  const severityBadge = document.createElement('span');
  severityBadge.className = `severity-badge ${severityClass(rule.severidad)}`;
  severityBadge.textContent = severityCode(rule.severidad);
  severity.appendChild(severityBadge);

  const status = document.createElement('td');
  const statusBadge = document.createElement('span');
  statusBadge.className = `status-badge ${rule.estado === 'ACTIVA' ? 'status-ready' : 'status-next'}`;
  statusBadge.textContent = ruleStatus(rule.estado);
  status.appendChild(statusBadge);

  row.append(code, name, baselineCell, type, severity, status);
  const inspect = () => void inspectRule(rule.id);
  row.addEventListener('click', inspect);
  row.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') inspect();
  });
  return row;
}

function render(): void {
  const rules = filteredRules();
  if (count) count.textContent = plural(state.rules.length, 'reglas.cantidad');
  if (!body || !empty) return;
  body.replaceChildren(...rules.map(buildRow));
  empty.classList.toggle('is-hidden', rules.length > 0);
  document.querySelector('.policy-table')?.classList.toggle('is-hidden', rules.length === 0);
}

async function inspectRule(id: number): Promise<void> {
  const response = await auditFetch(`/api/rules/${id}`);
  if (!response.ok) return showMessage(await readApiError(response), 'error');
  showRule(await response.json() as BaselineRule);
}

function showRule(rule: BaselineRule): void {
  state.current = rule;
  document.querySelectorAll<HTMLElement>('.policy-row').forEach((row) => {
    row.classList.toggle('is-selected', row.dataset.ruleId === String(rule.id));
  });

  const baseline = baselineFor(rule.baselineId);
  const expected = rule.tipo === 'VALOR_ESPERADO';
  const frozen = isFrozen(rule);
  setText('#rule-inspector-code', rule.codigo);
  setText('#rule-inspector-name', rule.nombre);
  setText('#rule-inspector-status', ruleStatus(rule.estado));
  setText('#rule-inspector-description', rule.descripcion);
  setText('#rule-inspector-baseline', baseline ? baselineLabel(baseline) : `#${rule.baselineId}`);
  setText('#rule-inspector-type', conditionLabel(rule.tipo));
  setText('#rule-inspector-severity', severityCode(rule.severidad));
  setText('#rule-inspector-id', String(rule.id));
  setText('#rule-inspector-pattern-label', t(expected ? 'reglas.parametro' : 'reglas.patron'));
  setText('#rule-inspector-pattern', rule.patron);
  setText('#rule-inspector-expected', rule.valorEsperado ?? '—');
  setText('#rule-inspector-impact', rule.impacto);
  setText('#rule-inspector-recommendation', rule.recomendacion);
  setText('#rule-toggle', t(rule.estado === 'ACTIVA' ? 'reglas.desactivar' : 'baselines.activar'));
  document.querySelector('#rule-inspector-expected-block')?.classList.toggle('is-hidden', !expected);
  document.querySelector('#rule-inspector-frozen')?.classList.toggle('is-hidden', !frozen);

  const editButton = document.querySelector<HTMLButtonElement>('#rule-edit');
  if (editButton) {
    editButton.disabled = frozen || baseline?.estado !== 'ACTIVO';
    editButton.title = frozen
      ? t('reglas.baselineUsada')
      : baseline?.estado !== 'ACTIVO' ? t('reglas.baselineInactiva') : '';
  }
  const toggleButton = document.querySelector<HTMLButtonElement>('#rule-toggle');
  if (toggleButton) {
    // La baja lógica siempre se permite; reactivar agrega un criterio y requiere una baseline editable.
    const blocked = rule.estado === 'INACTIVA' && (frozen || baseline?.estado !== 'ACTIVO');
    toggleButton.disabled = blocked;
    toggleButton.title = blocked ? t('reglas.reactivarAyuda') : '';
  }
  const status = document.querySelector('#rule-inspector-status');
  if (status) status.className = `status-badge ${rule.estado === 'ACTIVA' ? 'status-ready' : 'status-next'}`;
  document.querySelector('#rule-inspector-empty')?.classList.add('is-hidden');
  document.querySelector('#rule-inspector-content')?.classList.remove('is-hidden');
}

function fillBaselineSelect(selected?: number): void {
  if (!baselineSelect) return;
  baselineSelect.replaceChildren(new Option(t('reglas.seleccionarBaseline'), ''));
  state.baselines
    .filter((baseline) => baseline.estado === 'ACTIVO' || baseline.id === selected)
    .forEach((baseline) => {
      const frozen = baseline.auditorias > 0;
      const option = new Option(
        `${baselineLabel(baseline)}${frozen ? ` · ${t('reglas.usadaEnAuditorias')}` : ''}`,
        String(baseline.id),
      );
      option.disabled = frozen && baseline.id !== selected;
      baselineSelect.add(option);
    });
  if (selected) {
    baselineSelect.value = String(selected);
    return;
  }
  const editable = state.baselines.filter((baseline) => baseline.estado === 'ACTIVO' && baseline.auditorias === 0);
  if (editable.length === 1) baselineSelect.value = String(editable[0].id);
}

async function load(): Promise<void> {
  const [rulesResponse, baselinesResponse, organizationsResponse] = await Promise.all([
    auditFetch('/api/rules'),
    auditFetch('/api/baselines'),
    authFetch('/api/client-organizations'),
  ]);
  for (const response of [rulesResponse, baselinesResponse, organizationsResponse]) {
    if (!response.ok) throw new Error(await readApiError(response));
  }
  state.rules = await rulesResponse.json() as BaselineRule[];
  state.baselines = await baselinesResponse.json() as ConfigurationBaseline[];
  state.organizations = await organizationsResponse.json() as Organization[];
  render();
}

function syncTypeFields(): void {
  const expected = typeSelect?.value === 'VALOR_ESPERADO';
  document.querySelector('#rule-expected-field')?.classList.toggle('is-hidden', !expected);
  if (expectedInput) expectedInput.required = expected;
  setText('#rule-pattern-label', t(expected ? 'reglas.parametroObligatorio' : 'reglas.patronObligatorio'));
  const pattern = document.querySelector<HTMLInputElement>('#rule-pattern');
  if (pattern) pattern.placeholder = expected ? 'logging buffered' : 'ip ssh version 2';
}

function openRuleDialog(rule: BaselineRule | null): void {
  form?.reset();
  formError?.classList.add('is-hidden');
  state.editing = rule;
  fillBaselineSelect(rule?.baselineId);
  if (baselineSelect) baselineSelect.disabled = rule !== null;
  if (codeInput) codeInput.disabled = rule !== null;

  setText('#rule-dialog-eyebrow', t(rule ? 'reglas.editarRegla' : 'reglas.nuevaReglaTitulo'));
  setText('#rule-dialog-title', rule ? t('dispositivos.editarIdentificador', { identificador: rule.codigo }) : t('reglas.crearRegla'));
  setText('#rule-dialog-subtitle', rule
    ? t('reglas.edicionAyuda')
    : t('reglas.convertiCondicionConfiguracionControl'));
  setText('#rule-submit', t(rule ? 'general.guardarCambios' : 'reglas.crearRegla'));

  if (rule) {
    const set = (selector: string, value: string) => {
      const node = document.querySelector<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(selector);
      if (node) node.value = value;
    };
    set('#rule-code', rule.codigo);
    set('#rule-name', rule.nombre);
    set('#rule-type', rule.tipo);
    set('#rule-severity', rule.severidad);
    set('#rule-pattern', rule.patron);
    set('#rule-expected', rule.valorEsperado ?? '');
    set('#rule-description', rule.descripcion);
    set('#rule-impact', rule.impacto);
    set('#rule-recommendation', rule.recomendacion);
  }
  syncTypeFields();
  dialog?.showModal();
}

document.querySelector('#open-rule-dialog')?.addEventListener('click', () => openRuleDialog(null));
document.querySelector('#rule-edit')?.addEventListener('click', () => {
  if (state.current) openRuleDialog(state.current);
});
document.querySelector('#close-rule-dialog')?.addEventListener('click', () => dialog?.close());
document.querySelector('#cancel-rule-dialog')?.addEventListener('click', () => dialog?.close());
typeSelect?.addEventListener('change', syncTypeFields);
search?.addEventListener('input', render);
severityFilter?.addEventListener('change', render);
statusFilter?.addEventListener('change', render);

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  formError?.classList.add('is-hidden');
  const tipo = fieldValue('#rule-type') as RuleType;
  const datos = {
    nombre: fieldValue('#rule-name'),
    descripcion: fieldValue('#rule-description'),
    tipo,
    patron: fieldValue('#rule-pattern'),
    valorEsperado: tipo === 'VALOR_ESPERADO' ? fieldValue('#rule-expected') : null,
    severidad: fieldValue('#rule-severity'),
    impacto: fieldValue('#rule-impact'),
    recomendacion: fieldValue('#rule-recommendation'),
  };

  const editing = state.editing;
  const response = editing
    ? await auditFetch(`/api/rules/${editing.id}`, jsonRequest('PUT', datos))
    : await auditFetch(`/api/baselines/${Number(baselineSelect?.value)}/rules`,
      jsonRequest('POST', { codigo: fieldValue('#rule-code'), ...datos }));
  if (!response.ok) {
    if (formError) {
      formError.textContent = await readApiError(response);
      formError.classList.remove('is-hidden');
    }
    return;
  }
  const saved = await response.json() as BaselineRule;
  dialog?.close();
  showMessage(t(editing ? 'reglas.actualizada' : 'reglas.creada', { codigo: saved.codigo }));
  await load();
  showRule(saved);
});

document.querySelector('#rule-toggle')?.addEventListener('click', async () => {
  const rule = state.current;
  if (!rule) return;
  const deactivate = rule.estado === 'ACTIVA';
  const question = deactivate
    ? t('reglas.confirmarDesactivar', { codigo: rule.codigo })
    : t('reglas.confirmarActivar', { codigo: rule.codigo });
  if (!await confirmAction(t('general.confirmasOperacion'), question, t(deactivate ? 'reglas.desactivar' : 'baselines.activar'))) return;

  const response = await auditFetch(`/api/rules/${rule.id}/status`,
    jsonRequest('PATCH', { estado: deactivate ? 'INACTIVA' : 'ACTIVA' }));
  if (!response.ok) {
    showMessage(await readApiError(response), 'error');
    return;
  }
  const saved = await response.json() as BaselineRule;
  showMessage(t('reglas.ahoraEsta', { codigo: saved.codigo, estado: ruleStatus(saved.estado) }));
  await load();
  showRule(saved);
});

load().catch((error) => {
  showMessage(error instanceof Error ? error.message : t('reglas.errorCargar'), 'error');
});
