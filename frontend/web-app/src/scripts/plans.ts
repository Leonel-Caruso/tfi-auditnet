// Planes y solicitudes: resolución de pedidos y precios del catálogo.
import { authFetch } from '../lib/auth';
import { jsonRequest, readApiError, type Plan, type PlanRequest } from '../lib/api';
import { escapeHtml } from '../lib/format';
import { t } from '../lib/i18n';
import { isCustomPlan, planCard, requestDetail, requestStatusBadge } from '../lib/plans';
import { notify, show } from '../lib/ui';

const $ = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector);
const resolveDialog = $<HTMLDialogElement>('#resolve-dialog');
const priceDialog = $<HTMLDialogElement>('#price-dialog');

let catalog: Plan[] = [];
let requests: PlanRequest[] = [];
let resolving: { request: PlanRequest; approve: boolean } | null = null;
let editingPlan: Plan | null = null;

const planName = (code: string) => catalog.find((plan) => plan.codigo === code)?.nombre ?? code;

const requestItem = (request: PlanRequest, actions: boolean) => `
  <li data-request-id="${request.id}">
    <div>
      <strong>${escapeHtml(request.organizacion ?? t('tablero.organizacionNumero', { id: request.organizationId }))}: ${escapeHtml(planName(request.planActual))} → ${escapeHtml(planName(request.planSolicitado))}</strong>
      ${requestStatusBadge(request.estado)}
    </div>
    <small>${escapeHtml(requestDetail(request))}</small>
    ${request.comentario ? `<p>${escapeHtml(t('planes.comentarioCliente', { texto: request.comentario }))}</p>` : ''}
    ${request.respuesta ? `<p>${escapeHtml(t('planes.respuesta', { texto: request.respuesta }))}</p>` : ''}
    ${actions ? `<div class="inline-actions">
      <button class="button button-primary button-small" type="button" data-action="approve">${t('planes.aprobar')}</button>
      <button class="button button-ghost button-small" type="button" data-action="reject">${t('planes.rechazar')}</button>
    </div>` : ''}
  </li>`;

const renderRequests = () => {
  const pending = requests.filter((request) => request.estado === 'PENDIENTE');
  const resolved = requests.filter((request) => request.estado !== 'PENDIENTE').reverse();
  $('#pending-list')!.innerHTML = pending.map((request) => requestItem(request, true)).join('');
  $('#pending-count')!.textContent = String(pending.length);
  $('#pending-empty')?.classList.toggle('is-hidden', pending.length > 0);
  $('#history-list')!.innerHTML = resolved.map((request) => requestItem(request, false)).join('');
  $('#history-empty')?.classList.toggle('is-hidden', resolved.length > 0);
};

const renderCatalog = () => {
  $('#catalog')!.innerHTML = catalog.map((plan) => planCard(plan, false,
    `<button class="button button-ghost button-small" type="button" data-plan="${escapeHtml(plan.codigo)}">${t('planes.editarPrecio')}</button>`)).join('');
};

const load = async () => {
  const [catalogResponse, requestResponse] = await Promise.all([
    authFetch('/api/plans'),
    authFetch('/api/admin/plan-requests'),
  ]);
  if (!catalogResponse.ok) return show('#plans-error', await readApiError(catalogResponse));
  if (!requestResponse.ok) return show('#plans-error', await readApiError(requestResponse));
  catalog = await catalogResponse.json() as Plan[];
  requests = await requestResponse.json() as PlanRequest[];
  renderCatalog();
  renderRequests();
};

// Aprobar o rechazar
$('#pending-list')?.addEventListener('click', (event) => {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-action]');
  const id = Number(button?.closest<HTMLElement>('[data-request-id]')?.dataset.requestId);
  const request = requests.find((item) => item.id === id);
  if (!button || !request) return;
  const approve = button.dataset.action === 'approve';
  const target = catalog.find((plan) => plan.codigo === request.planSolicitado);
  const custom = approve && target !== undefined && isCustomPlan(target);
  resolving = { request, approve };
  $<HTMLFormElement>('#resolve-form')?.reset();
  show('#resolve-form-error', null);
  $('#resolve-eyebrow')!.textContent = t(approve ? 'planes.aprobarSolicitud' : 'planes.rechazarSolicitud');
  $('#resolve-title')!.textContent = `${request.organizacion ?? ''}: ${planName(request.planSolicitado)}`;
  $('#resolve-help')!.textContent = t(approve ? 'planes.aprobarAyuda' : 'planes.rechazarAyuda');
  $('#resolve-answer-label')!.textContent = t(approve ? 'planes.respuestaCliente' : 'planes.motivoRechazo');
  $('#resolve-submit')!.textContent = t(approve ? 'planes.aprobar' : 'planes.rechazar');
  document.querySelectorAll<HTMLElement>('[data-custom-limit]').forEach((node) => node.classList.toggle('is-hidden', !custom));
  resolveDialog?.showModal();
});

$<HTMLFormElement>('#resolve-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!resolving) return;
  show('#resolve-form-error', null);
  const { request, approve } = resolving;
  const number = (selector: string) => {
    const input = $<HTMLInputElement>(selector);
    return input && !input.closest('.is-hidden') && input.value !== '' ? Number(input.value) : null;
  };
  const payload = {
    respuesta: ($<HTMLTextAreaElement>('#resolve-answer')?.value ?? '').trim(),
    limiteDispositivos: number('#resolve-devices'),
    limiteUsuarios: number('#resolve-users'),
  };
  const response = await authFetch(`/api/admin/plan-requests/${request.id}/${approve ? 'approve' : 'reject'}`,
    jsonRequest('POST', payload));
  if (!response.ok) return show('#resolve-form-error', await readApiError(response));
  resolveDialog?.close();
  notify('#plans-message', approve
    ? t('organizaciones.planAplicado', { organizacion: request.organizacion ?? t('planes.laOrganizacion'), plan: planName(request.planSolicitado) })
    : t('planes.solicitudRechazada'));
  await load();
});

// Precio y condiciones del catálogo
$('#catalog')?.addEventListener('click', (event) => {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-plan]');
  editingPlan = catalog.find((plan) => plan.codigo === button?.dataset.plan) ?? null;
  if (!editingPlan) return;
  show('#price-form-error', null);
  $('#price-title')!.textContent = t('planes.planNombre', { plan: editingPlan.nombre });
  $<HTMLInputElement>('#price-value')!.value = editingPlan.precioMensualUsd === null ? '' : String(editingPlan.precioMensualUsd);
  $<HTMLTextAreaElement>('#price-conditions')!.value = editingPlan.condiciones ?? '';
  priceDialog?.showModal();
});

$<HTMLFormElement>('#price-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!editingPlan) return;
  const price = $<HTMLInputElement>('#price-value')?.value ?? '';
  const response = await authFetch(`/api/plans/${editingPlan.codigo}`, jsonRequest('PUT', {
    precioMensualUsd: price === '' ? null : Number(price),
    condiciones: ($<HTMLTextAreaElement>('#price-conditions')?.value ?? '').trim(),
  }));
  if (!response.ok) return show('#price-form-error', await readApiError(response));
  priceDialog?.close();
  notify('#plans-message', t('planes.planActualizado', { plan: editingPlan.nombre }));
  await load();
});

document.querySelectorAll<HTMLButtonElement>('[data-close-dialog]').forEach((button) => {
  button.addEventListener('click', () => button.closest('dialog')?.close());
});

load().catch((error) => show('#plans-error', error instanceof Error ? error.message : t('planes.errorCargar')));
