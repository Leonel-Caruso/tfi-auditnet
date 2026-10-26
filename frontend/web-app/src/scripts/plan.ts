// Mi plan: plan contratado, uso y solicitudes de cambio.
import { authFetch, getStoredUser, hasRole } from '../lib/auth';
import { jsonRequest, readApiError, type OrganizationPlan, type Plan, type PlanRequest } from '../lib/api';
import { escapeHtml } from '../lib/format';
import { t } from '../lib/i18n';
import {
  formatDay, formatPrice, isCustomPlan, isTrialPlan, limitsText, planCard, requestDetail, requestStatusBadge,
  usageRow, validityText,
} from '../lib/plans';
import { show } from '../lib/ui';

// El administrador del sistema gestiona los planes de todas las organizaciones desde otra pantalla.
if (hasRole('ADMINISTRADOR_SISTEMA')) window.location.replace('/plans');

const organizationId = getStoredUser()?.organizationId;
const canRequest = hasRole('ADMINISTRADOR_ORGANIZACION');
const $ = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector);
const requestDialog = $<HTMLDialogElement>('#request-dialog');

let catalog: Plan[] = [];
let current: OrganizationPlan | null = null;

const planName = (code: string) => catalog.find((plan) => plan.codigo === code)?.nombre ?? code;

/** Planes que se pueden pedir: no los de prueba ni el actual (salvo uno a medida, para renegociar límites). */
const requestable = () => catalog.filter((plan) => !isTrialPlan(plan)
  && (plan.codigo !== current?.plan.codigo || isCustomPlan(plan)));

const renderCurrent = () => {
  if (!current) return;
  $('#plan-name')!.textContent = current.plan.nombre;
  $('#plan-price')!.textContent = formatPrice(current.plan);
  $('#plan-current')!.innerHTML = `
    <p class="plan-validity ${current.vencido ? 'is-expired' : ''}">${escapeHtml(validityText(current))}</p>
    ${usageRow(t('planes.dispositivosActivos'), current.dispositivosActivos, current.maxDispositivos)}
    ${usageRow(t('planes.usuariosActivos'), current.usuariosActivos, current.maxUsuarios)}
    <p class="log-muted">${escapeHtml(t('plan.desdeEl', { fecha: formatDay(current.desde) }))}
      ${current.plan.condiciones ? escapeHtml(current.plan.condiciones) : ''}</p>`;

  const expiry = $('#plan-expiry');
  if (expiry) {
    const soon = current.vence && !current.vencido && (current.diasRestantes ?? 0) <= 15;
    expiry.className = `alert ${current.vencido ? 'alert-error' : 'alert-warning'}`;
    expiry.textContent = current.vencido
      ? t('plan.pruebaVencida')
      : t('plan.pruebaPorVencer', { dias: current.diasRestantes ?? 0 });
    expiry.classList.toggle('is-hidden', !current.vencido && !soon);
  }

  const pending = current.solicitudPendiente;
  $('#plan-pending')!.innerHTML = pending
    ? `<ul class="request-list"><li>
        <div><strong>${escapeHtml(planName(pending.planActual))} → ${escapeHtml(planName(pending.planSolicitado))}</strong>${requestStatusBadge(pending.estado)}</div>
        <small>${escapeHtml(requestDetail(pending))}</small>
        ${pending.comentario ? `<p>${escapeHtml(pending.comentario)}</p>` : ''}
        ${canRequest ? `<div><button class="button button-ghost button-small" type="button" data-cancel="${pending.id}">${escapeHtml(t('plan.cancelarSolicitud'))}</button></div>` : ''}
      </li></ul>`
    : `<p class="log-muted">${escapeHtml(t('plan.sinPendientes'))}${canRequest ? ` ${escapeHtml(t('plan.pediOtroPlan'))}` : ''}</p>`;
  const open = $<HTMLButtonElement>('#request-open');
  if (open) open.disabled = Boolean(pending) || requestable().length === 0;
};

const renderCatalog = () => {
  $('#plan-catalog')!.innerHTML = catalog.map((plan) => planCard(plan, plan.codigo === current?.plan.codigo)).join('');
};

const renderHistory = (requests: PlanRequest[]) => {
  const done = requests.filter((request) => request.estado !== 'PENDIENTE');
  $('#plan-history')!.innerHTML = done.map((request) => `
    <li>
      <div><strong>${escapeHtml(planName(request.planActual))} → ${escapeHtml(planName(request.planSolicitado))}</strong>${requestStatusBadge(request.estado)}</div>
      <small>${escapeHtml(requestDetail(request))}</small>
      ${request.respuesta ? `<p>${escapeHtml(request.respuesta)}</p>` : ''}
    </li>`).join('');
  $('#plan-history-empty')?.classList.toggle('is-hidden', done.length > 0);
};

const load = async () => {
  if (!organizationId) return show('#plan-error', t('tablero.sinOrganizacion'));
  const [catalogResponse, planResponse] = await Promise.all([
    authFetch('/api/plans'),
    authFetch(`/api/client-organizations/${organizationId}/plan`),
  ]);
  if (!planResponse.ok) return show('#plan-error', await readApiError(planResponse));
  if (catalogResponse.ok) catalog = await catalogResponse.json() as Plan[];
  current = await planResponse.json() as OrganizationPlan;
  renderCurrent();
  renderCatalog();
  const historyResponse = await authFetch(`/api/client-organizations/${organizationId}/plan-requests`);
  renderHistory(historyResponse.ok ? await historyResponse.json() as PlanRequest[] : []);
};

const syncConditions = () => {
  const plan = catalog.find((item) => item.codigo === $<HTMLSelectElement>('#request-plan')?.value);
  $('#request-conditions')!.textContent = plan
    ? `${formatPrice(plan)} · ${limitsText(plan)}. ${plan.condiciones ?? ''}`
    : '';
};

$('#request-open')?.addEventListener('click', () => {
  $<HTMLFormElement>('#request-form')?.reset();
  show('#request-form-error', null);
  const select = $<HTMLSelectElement>('#request-plan');
  select?.replaceChildren(...requestable().map((plan) => new Option(plan.nombre, plan.codigo)));
  syncConditions();
  requestDialog?.showModal();
});
$('#request-plan')?.addEventListener('change', syncConditions);

$<HTMLFormElement>('#request-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  show('#request-form-error', null);
  const payload = {
    plan: $<HTMLSelectElement>('#request-plan')?.value ?? '',
    modalidad: document.querySelector<HTMLInputElement>('input[name=modalidad]:checked')?.value ?? 'MENSUAL',
    comentario: ($<HTMLTextAreaElement>('#request-comment')?.value ?? '').trim(),
  };
  const response = await authFetch(`/api/client-organizations/${organizationId}/plan-requests`, jsonRequest('POST', payload));
  if (!response.ok) return show('#request-form-error', await readApiError(response));
  requestDialog?.close();
  show('#plan-message', t('plan.solicitudEnviada'));
  await load();
});

$('#plan-pending')?.addEventListener('click', async (event) => {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-cancel]');
  if (!button) return;
  const response = await authFetch(`/api/client-organizations/${organizationId}/plan-requests/${button.dataset.cancel}/cancel`,
    { method: 'POST' });
  if (!response.ok) return show('#plan-error', await readApiError(response));
  show('#plan-message', t('plan.solicitudCancelada'));
  await load();
});

document.querySelectorAll<HTMLButtonElement>('[data-close-dialog]').forEach((button) => {
  button.addEventListener('click', () => button.closest('dialog')?.close());
});

load().catch((error) => show('#plan-error', error instanceof Error ? error.message : t('plan.errorCargar')));
