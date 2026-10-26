// Organizaciones y sedes: alta, edición, plan y plazos de revisión.
import { auditFetch, authFetch, hasRole } from '../lib/auth';
import { jsonRequest, readApiError, type Organization, type OrganizationPlan, type Plan, type ReviewPolicy, type Site } from '../lib/api';
import { escapeHtml, formatDate, recordStatusCode } from '../lib/format';
import { t } from '../lib/i18n';
import { formatDay, formatPrice, isCustomPlan, limitsText, isTrialPlan, requestStatusBadge, usageRow, validityText } from '../lib/plans';
import { confirmAction, notify, show } from '../lib/ui';

const orgList = document.querySelector<HTMLDivElement>('#organization-list');
const orgCount = document.querySelector<HTMLSpanElement>('#organization-count');
const siteList = document.querySelector<HTMLDivElement>('#site-list');
const siteTitle = document.querySelector<HTMLHeadingElement>('#site-title');
const accessError = document.querySelector<HTMLElement>('#organization-access-error');
const openSiteButton = document.querySelector<HTMLButtonElement>('#open-site-dialog');
const organizationDialog = document.querySelector<HTMLDialogElement>('#organization-dialog');
const siteDialog = document.querySelector<HTMLDialogElement>('#site-dialog');

let selectedOrganization: Organization | null = null;
let editingOrganization: Organization | null = null;
let editingSite: Site | null = null;
let currentSites: Site[] = [];
const isAdmin = hasRole('ADMINISTRADOR_SISTEMA');
const canManageSites = isAdmin || hasRole('ADMINISTRADOR_ORGANIZACION');
let catalog: Plan[] = [];

const statusClass = (estado: string) => (estado === 'ACTIVO' ? 'status-ready' : 'status-off');

function showError(text: string): void {
  if (!accessError) return;
  accessError.textContent = text;
  accessError.classList.remove('is-hidden');
}

function showMessage(text: string): void {
  accessError?.classList.add('is-hidden');
  notify('#organization-message', text, 'success', 4200);
}

function inputValue(selector: string): string {
  return (document.querySelector<HTMLInputElement>(selector)?.value ?? '').trim();
}

function buildSiteRow(site: Site): HTMLDivElement {
  const row = document.createElement('div');
  row.className = 'data-row';

  const copy = document.createElement('div');
  const name = document.createElement('strong');
  name.textContent = site.nombre;
  const location = document.createElement('span');
  location.textContent = site.ubicacion;
  copy.append(name, location);

  const status = document.createElement('span');
  status.className = `status-badge ${statusClass(site.estado)}`;
  status.textContent = recordStatusCode(site.estado).toLowerCase();

  const side = document.createElement('div');
  side.className = 'inline-actions';
  side.append(status);
  if (canManageSites) {
    const edit = document.createElement('button');
    edit.type = 'button';
    edit.className = 'button button-ghost button-small';
    edit.textContent = t('general.editar');
    edit.addEventListener('click', () => openSiteDialog(site));
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'button button-ghost button-small';
    toggle.textContent = t(site.estado === 'ACTIVO' ? 'organizaciones.deshabilitar' : 'organizaciones.habilitar');
    toggle.addEventListener('click', () => void toggleSite(site));
    side.append(edit, toggle);
  }

  row.append(copy, side);
  return row;
}

function syncOrganizationActions(organization: Organization): void {
  const active = organization.estado === 'ACTIVO';
  if (openSiteButton) openSiteButton.disabled = !active;
  const edit = document.querySelector<HTMLButtonElement>('#edit-organization');
  const toggle = document.querySelector<HTMLButtonElement>('#toggle-organization');
  if (edit) edit.disabled = false;
  if (toggle) {
    toggle.disabled = false;
    toggle.textContent = t(active ? 'organizaciones.deshabilitar' : 'organizaciones.habilitar');
  }
  document.querySelector('#organization-disabled-note')?.classList.toggle('is-hidden', active);
}

// Plan de la organización seleccionada
function fillPlanSelect(select: HTMLSelectElement | null, current?: string): void {
  if (!select) return;
  select.replaceChildren(...catalog.map((plan) => new Option(
    `${plan.nombre} · ${limitsText(plan)} · ${formatPrice(plan)}`,
    plan.codigo)));
  if (current) select.value = current;
}

/** Muestra días de prueba o límites acordados según el plan elegido en el diálogo. */
function syncPlanFields(form: HTMLElement): void {
  const code = form.querySelector<HTMLSelectElement>('[data-plan-select]')?.value;
  const plan = catalog.find((item) => item.codigo === code);
  const editingExisting = form.id === 'organization-form' && editingOrganization !== null;
  form.querySelectorAll<HTMLElement>('[data-plan-choice]').forEach((node) => node.classList.toggle('is-hidden', editingExisting));
  form.querySelectorAll<HTMLElement>('[data-plan-trial]').forEach((node) =>
    node.classList.toggle('is-hidden', editingExisting || !plan || !isTrialPlan(plan)));
  form.querySelectorAll<HTMLElement>('[data-plan-custom]').forEach((node) =>
    node.classList.toggle('is-hidden', editingExisting || !plan || !isCustomPlan(plan)));
}

function planPayload(form: HTMLElement) {
  const number = (selector: string) => {
    const input = form.querySelector<HTMLInputElement>(selector);
    return input && !input.closest('.is-hidden') && input.value !== '' ? Number(input.value) : null;
  };
  return {
    plan: form.querySelector<HTMLSelectElement>('[data-plan-select]')?.value ?? '',
    diasPrueba: number('[data-plan-days]'),
    limiteDispositivos: number('[data-plan-devices]'),
    limiteUsuarios: number('[data-plan-users]'),
  };
}

let currentPlan: OrganizationPlan | null = null;

async function loadPlan(organization: Organization): Promise<void> {
  const summary = document.querySelector<HTMLElement>('#plan-summary');
  const changeButton = document.querySelector<HTMLButtonElement>('#change-plan');
  if (changeButton) changeButton.disabled = true;
  const response = await authFetch(`/api/client-organizations/${organization.id}/plan`);
  if (!summary) return;
  if (!response.ok) {
    summary.innerHTML = `<div class="alert alert-error">${escapeHtml(await readApiError(response))}</div>`;
    return;
  }
  currentPlan = await response.json() as OrganizationPlan;
  const pending = currentPlan.solicitudPendiente;
  summary.innerHTML = `
    <div class="plan-summary-head">
      <h3>${escapeHtml(currentPlan.plan.nombre)}</h3>
      <code>${escapeHtml(formatPrice(currentPlan.plan))} · ${escapeHtml(t('organizaciones.planDesde', { fecha: formatDay(currentPlan.desde) }))}</code>
    </div>
    <p class="plan-validity ${currentPlan.vencido ? 'is-expired' : ''}">${escapeHtml(validityText(currentPlan))}</p>
    ${usageRow(t('planes.dispositivosActivos'), currentPlan.dispositivosActivos, currentPlan.maxDispositivos)}
    ${usageRow(t('planes.usuariosActivos'), currentPlan.usuariosActivos, currentPlan.maxUsuarios)}
    ${pending ? `<p class="log-muted">${t('organizaciones.solicitudPendiente', {
      estado: requestStatusBadge(pending.estado),
      plan: `<strong>${escapeHtml(catalog.find((plan) => plan.codigo === pending.planSolicitado)?.nombre ?? pending.planSolicitado)}</strong>`,
      usuario: escapeHtml(pending.solicitadoPor),
    })}${isAdmin ? ` <a class="inline-link" href="/plans">${escapeHtml(t('organizaciones.resolver'))}</a>` : ''}</p>` : ''}`;
  if (changeButton) changeButton.disabled = false;
}

// Plazos de revisión (audit-core-service)
const canEditReview = canManageSites;
const reviewInputs: Record<keyof Omit<ReviewPolicy, 'reauditoriaAutomatica' | 'modificadaEn' | 'modificadaPor'>, string> = {
  diasCritica: '#review-critica',
  diasAlta: '#review-alta',
  diasMedia: '#review-media',
  diasBaja: '#review-baja',
  diasReauditoria: '#review-reaudit',
  diasConfiguracion: '#review-config',
};
const reviewFields = document.querySelector<HTMLFieldSetElement>('#review-fields');
const reviewSubmit = document.querySelector<HTMLButtonElement>('#review-submit');

function renderReview(policy: ReviewPolicy): void {
  (Object.entries(reviewInputs) as [keyof typeof reviewInputs, string][]).forEach(([key, selector]) => {
    document.querySelector<HTMLInputElement>(selector)!.value = String(policy[key]);
  });
  document.querySelector<HTMLInputElement>('#review-auto')!.checked = policy.reauditoriaAutomatica;
  const updated = document.querySelector('#review-updated');
  if (updated) {
    updated.textContent = policy.modificadaEn
      ? `${formatDate(policy.modificadaEn)} · ${policy.modificadaPor ?? ''}`
      : t('organizaciones.valoresDefecto');
  }
}

async function loadReview(organization: Organization): Promise<void> {
  show('#review-error', null);
  if (reviewFields) reviewFields.disabled = true;
  if (reviewSubmit) reviewSubmit.disabled = true;
  const response = await auditFetch(`/api/client-organizations/${organization.id}/review-policy`);
  if (!response.ok) return show('#review-error', await readApiError(response));
  renderReview(await response.json() as ReviewPolicy);
  const editable = canEditReview && organization.estado === 'ACTIVO';
  if (reviewFields) reviewFields.disabled = !editable;
  if (reviewSubmit) reviewSubmit.disabled = !editable;
}

document.querySelector<HTMLFormElement>('#review-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const organization = selectedOrganization;
  if (!organization) return;
  show('#review-error', null);
  const payload: Record<string, number | boolean> = {
    reauditoriaAutomatica: document.querySelector<HTMLInputElement>('#review-auto')!.checked,
  };
  for (const [key, selector] of Object.entries(reviewInputs)) {
    const value = Number(document.querySelector<HTMLInputElement>(selector)!.value);
    if (!Number.isInteger(value) || value < 1 || value > 365) {
      return show('#review-error', t('organizaciones.plazoInvalido'));
    }
    payload[key] = value;
  }
  const response = await auditFetch(`/api/client-organizations/${organization.id}/review-policy`,
    jsonRequest('PUT', payload));
  if (!response.ok) return show('#review-error', await readApiError(response));
  renderReview(await response.json() as ReviewPolicy);
  showMessage(t('organizaciones.plazosGuardados', { organizacion: organization.razonSocial }));
});

async function loadSites(organization: Organization): Promise<void> {
  selectedOrganization = organization;
  syncOrganizationActions(organization);
  void loadPlan(organization);
  void loadReview(organization);
  if (siteTitle) siteTitle.textContent = organization.razonSocial;
  if (siteList) {
    siteList.className = 'data-list';
    const skeleton = document.createElement('div');
    skeleton.className = 'skeleton-row';
    siteList.replaceChildren(skeleton);
  }

  const response = await authFetch(`/api/client-organizations/${organization.id}/sites`);
  if (!response.ok) {
    if (siteList) {
      const error = document.createElement('div');
      error.className = 'alert alert-error';
      error.textContent = await readApiError(response);
      siteList.replaceChildren(error);
    }
    return;
  }

  const sites = await response.json() as Site[];
  currentSites = sites;
  if (!siteList) return;

  siteList.replaceChildren();
  if (sites.length === 0) {
    siteList.className = 'data-list empty-state';
    const empty = document.createElement('p');
    empty.textContent = t('organizaciones.sinSedes');
    siteList.appendChild(empty);
    return;
  }

  siteList.className = 'data-list';
  sites.forEach((site) => siteList.appendChild(buildSiteRow(site)));
}

function buildOrganizationButton(organization: Organization): HTMLButtonElement {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'select-row';
  button.dataset.organizationId = String(organization.id);

  const copy = document.createElement('span');
  copy.className = 'select-row-main';
  const name = document.createElement('strong');
  name.textContent = organization.razonSocial;
  const identifier = document.createElement('small');
  identifier.textContent = organization.identificador;
  copy.append(name, identifier);

  const status = document.createElement('span');
  status.className = `status-badge ${statusClass(organization.estado)}`;
  status.textContent = recordStatusCode(organization.estado).toLowerCase();
  button.append(copy, status);

  button.addEventListener('click', () => {
    document.querySelectorAll('.select-row').forEach((node) => node.classList.remove('is-selected'));
    button.classList.add('is-selected');
    void loadSites(organization);
  });

  return button;
}

async function loadOrganizations(selectId?: number): Promise<void> {
  const response = await authFetch('/api/client-organizations');
  if (!response.ok) {
    showError(await readApiError(response));
    orgList?.replaceChildren();
    return;
  }

  const organizations = await response.json() as Organization[];
  if (orgCount) orgCount.textContent = String(organizations.length);
  if (!orgList) return;

  const buttons = organizations.map(buildOrganizationButton);
  orgList.replaceChildren(...buttons);
  const toSelect = buttons.find((button) => Number(button.dataset.organizationId) === selectId) ?? buttons[0];
  toSelect?.click();
}

// Diálogos
document.querySelectorAll<HTMLButtonElement>('[data-close-dialog]').forEach((button) => {
  button.addEventListener('click', () => button.closest('dialog')?.close());
});

function openOrganizationDialog(organization: Organization | null): void {
  editingOrganization = organization;
  document.querySelector<HTMLFormElement>('#organization-form')?.reset();
  show('#organization-form-error', null);
  const text = (selector: string, value: string) => {
    const node = document.querySelector(selector);
    if (node) node.textContent = value;
  };
  text('#organization-dialog-eyebrow', t(organization ? 'organizaciones.modificarOrganizacion' : 'organizaciones.altaOrganizacion'));
  text('#organization-dialog-title', organization ? organization.razonSocial : t('organizaciones.registrarOrganizacion'));
  text('#organization-dialog-help', t(organization ? 'organizaciones.siguenUnicos' : 'organizaciones.incorporaClientePlataforma'));
  text('#organization-dialog-state', t('organizaciones.estadoCodigo', { estado: recordStatusCode(organization?.estado ?? 'ACTIVO') }));
  text('#organization-submit', t(organization ? 'general.guardarCambios' : 'organizaciones.registrarOrganizacion'));
  if (organization) {
    document.querySelector<HTMLInputElement>('#organization-identifier')!.value = organization.identificador;
    document.querySelector<HTMLInputElement>('#organization-name')!.value = organization.razonSocial;
  }
  const form = document.querySelector<HTMLFormElement>('#organization-form');
  fillPlanSelect(document.querySelector('#organization-plan'), 'PILOTO');
  if (form) syncPlanFields(form);
  organizationDialog?.showModal();
}

function openSiteDialog(site: Site | null): void {
  if (!selectedOrganization) return;
  editingSite = site;
  document.querySelector<HTMLFormElement>('#site-form')?.reset();
  show('#site-form-error', null);
  const text = (selector: string, value: string) => {
    const node = document.querySelector(selector);
    if (node) node.textContent = value;
  };
  text('#site-dialog-eyebrow', t(site ? 'organizaciones.modificarSede' : 'organizaciones.altaSede'));
  text('#site-dialog-title', site ? site.nombre : t('organizaciones.registrarSede'));
  text('#site-dialog-context', t(site ? 'organizaciones.sedeDe' : 'organizaciones.nuevaSedeDe',
    { organizacion: selectedOrganization.razonSocial }));
  text('#site-submit', t(site ? 'general.guardarCambios' : 'organizaciones.registrarSede'));
  if (site) {
    document.querySelector<HTMLInputElement>('#site-name')!.value = site.nombre;
    document.querySelector<HTMLInputElement>('#site-location')!.value = site.ubicacion;
  }
  siteDialog?.showModal();
}

async function toggleSite(site: Site): Promise<void> {
  if (!selectedOrganization) return;
  const disable = site.estado === 'ACTIVO';
  const accepted = await confirmAction(
    t(disable ? 'organizaciones.deshabilitarSede' : 'organizaciones.habilitarSede', { sede: site.nombre }),
    t(disable ? 'organizaciones.deshabilitarSedeAyuda' : 'organizaciones.habilitarSedeAyuda'),
    t(disable ? 'organizaciones.deshabilitar' : 'organizaciones.habilitar'));
  if (!accepted) return;
  const response = await authFetch(`/api/sites/${site.id}/status`, jsonRequest('PATCH', { estado: disable ? 'DESHABILITADO' : 'ACTIVO' }));
  if (!response.ok) return showError(await readApiError(response));
  showMessage(t(disable ? 'organizaciones.sedeDeshabilitada' : 'organizaciones.sedeHabilitada', { sede: site.nombre }));
  await loadSites(selectedOrganization);
}

document.querySelector('#open-organization-dialog')?.addEventListener('click', () => openOrganizationDialog(null));
document.querySelector('#edit-organization')?.addEventListener('click', () => {
  if (selectedOrganization) openOrganizationDialog(selectedOrganization);
});
openSiteButton?.addEventListener('click', () => openSiteDialog(null));

document.querySelector('#toggle-organization')?.addEventListener('click', async () => {
  const organization = selectedOrganization;
  if (!organization) return;
  const disable = organization.estado === 'ACTIVO';
  const accepted = await confirmAction(
    t(disable ? 'organizaciones.deshabilitarOrganizacion' : 'organizaciones.habilitarOrganizacion',
      { organizacion: organization.razonSocial }),
    disable
      ? t('organizaciones.deshabilitarOrganizacionAyuda', { sedes: currentSites.length })
      : t('organizaciones.habilitarOrganizacionAyuda'),
    t(disable ? 'organizaciones.deshabilitar' : 'organizaciones.habilitar'));
  if (!accepted) return;
  const response = await authFetch(`/api/client-organizations/${organization.id}/status`,
    jsonRequest('PATCH', { estado: disable ? 'DESHABILITADO' : 'ACTIVO' }));
  if (!response.ok) return showError(await readApiError(response));
  showMessage(t(disable ? 'organizaciones.organizacionDeshabilitada' : 'organizaciones.organizacionHabilitada',
    { organizacion: organization.razonSocial }));
  await loadOrganizations(organization.id);
});

document.querySelector<HTMLFormElement>('#organization-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  show('#organization-form-error', null);
  const payload = {
    identificador: inputValue('#organization-identifier'),
    razonSocial: inputValue('#organization-name'),
  };
  const form = document.querySelector<HTMLFormElement>('#organization-form')!;
  const response = editingOrganization
    ? await authFetch(`/api/client-organizations/${editingOrganization.id}`, jsonRequest('PUT', payload))
    : await authFetch('/api/client-organizations', jsonRequest('POST', { ...payload, ...planPayload(form) }));
  if (!response.ok) {
    show('#organization-form-error', await readApiError(response));
    return;
  }
  const saved = await response.json() as Organization;
  organizationDialog?.close();
  showMessage(editingOrganization
    ? t('organizaciones.organizacionActualizada', { organizacion: saved.razonSocial })
    : t('organizaciones.organizacionRegistrada', { organizacion: saved.razonSocial }));
  await loadOrganizations(saved.id);
});

document.querySelector<HTMLFormElement>('#site-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!selectedOrganization) return;
  show('#site-form-error', null);
  const organization = selectedOrganization;
  const payload = {
    nombre: inputValue('#site-name'),
    ubicacion: inputValue('#site-location'),
  };
  const response = editingSite
    ? await authFetch(`/api/sites/${editingSite.id}`, jsonRequest('PUT', payload))
    : await authFetch(`/api/client-organizations/${organization.id}/sites`, jsonRequest('POST', payload));
  if (!response.ok) {
    show('#site-form-error', await readApiError(response));
    return;
  }
  const saved = await response.json() as Site;
  siteDialog?.close();
  showMessage(editingSite
    ? t('organizaciones.sedeActualizada', { sede: saved.nombre })
    : t('organizaciones.sedeRegistrada', { sede: saved.nombre, organizacion: organization.razonSocial }));
  await loadSites(organization);
});

// Cambio de plan (administrador del sistema)
const planDialog = document.querySelector<HTMLDialogElement>('#plan-dialog');
document.querySelectorAll<HTMLSelectElement>('[data-plan-select]').forEach((select) => {
  select.addEventListener('change', () => syncPlanFields(select.closest('form')!));
});

document.querySelector('#change-plan')?.addEventListener('click', () => {
  if (!selectedOrganization || !currentPlan) return;
  const form = document.querySelector<HTMLFormElement>('#plan-form')!;
  form.reset();
  show('#plan-form-error', null);
  document.querySelector('#plan-dialog-title')!.textContent = t('organizaciones.planDe', { organizacion: selectedOrganization.razonSocial });
  fillPlanSelect(document.querySelector('#plan-code'), currentPlan.plan.codigo);
  if (isCustomPlan(currentPlan.plan)) {
    document.querySelector<HTMLInputElement>('#plan-limit-devices')!.value = String(currentPlan.maxDispositivos ?? '');
    document.querySelector<HTMLInputElement>('#plan-limit-users')!.value = String(currentPlan.maxUsuarios ?? '');
  }
  syncPlanFields(form);
  planDialog?.showModal();
});

document.querySelector<HTMLFormElement>('#plan-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const organization = selectedOrganization;
  if (!organization) return;
  show('#plan-form-error', null);
  const response = await authFetch(`/api/client-organizations/${organization.id}/plan`,
    jsonRequest('PUT', planPayload(event.currentTarget as HTMLFormElement)));
  if (!response.ok) return show('#plan-form-error', await readApiError(response));
  planDialog?.close();
  const updated = await response.json() as OrganizationPlan;
  showMessage(t('organizaciones.planAplicado', { organizacion: organization.razonSocial, plan: updated.plan.nombre }));
  await loadPlan(organization);
});

async function loadCatalog(): Promise<void> {
  const response = await authFetch('/api/plans');
  if (response.ok) catalog = await response.json() as Plan[];
}

loadCatalog()
  .then(() => loadOrganizations())
  .catch(() => showError(t('organizaciones.errorCargar')));
