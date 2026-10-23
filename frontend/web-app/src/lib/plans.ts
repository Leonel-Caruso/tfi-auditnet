// Presentación de planes, uso y solicitudes de cambio (Mi plan, Organizaciones y Planes).
import type { OrganizationPlan, Plan, PlanRequest, PlanRequestStatus } from './api';
import { escapeHtml, formatDate } from './format';
import { etiqueta, localeActual, t } from './i18n';

export const formatDay = (value: string | null | undefined) =>
  value ? new Intl.DateTimeFormat(localeActual(), { dateStyle: 'short' }).format(new Date(value)) : '—';

/** Plan a medida: los límites se acuerdan con cada cliente. */
export const isCustomPlan = (plan: Plan) => plan.maxDispositivos === null || plan.maxUsuarios === null;

export const isTrialPlan = (plan: Plan) => plan.diasVigencia !== null;

export const formatPrice = (plan: Plan) => {
  if (plan.precioMensualUsd !== null) {
    const precio = new Intl.NumberFormat(localeActual(), { maximumFractionDigits: 2 }).format(plan.precioMensualUsd);
    return t('planes.precioMensual', { precio });
  }
  return isCustomPlan(plan) ? t('planes.aCotizar') : t('planes.bonificado');
};

/** Límites en una frase: "hasta 50 dispositivos y 15 usuarios activos" o "dispositivos y usuarios a medida". */
export const limitsText = (plan: Plan) => (isCustomPlan(plan)
  ? t('planes.limitesAMedida')
  : t('planes.limites', { dispositivos: plan.maxDispositivos ?? 0, usuarios: plan.maxUsuarios ?? 0 }));

/** Tarjeta del catálogo; con current marca el plan de la organización. */
export const planCard = (plan: Plan, current: boolean, action = '') => `
  <article class="plan-card ${current ? 'is-current' : ''}">
    <h3>${escapeHtml(plan.nombre)}${current ? `<span class="status-badge status-strong">${escapeHtml(t('planes.actual'))}</span>` : ''}</h3>
    <strong>${escapeHtml(formatPrice(plan))}</strong>
    <ul>
      <li>${escapeHtml(limitsText(plan))}</li>
      ${plan.diasVigencia ? `<li>${escapeHtml(t('planes.prueba', { dias: plan.diasVigencia }))}</li>` : ''}
    </ul>
    ${plan.condiciones ? `<p>${escapeHtml(plan.condiciones)}</p>` : ''}
    ${action}
  </article>`;

export const modalityLabel = (modality: PlanRequest['modalidad']) => etiqueta('modalidad', modality);

/** "mensual · pedida por ana el 01/10/26 · resuelta por admin el 02/10/26" */
export const requestDetail = (request: PlanRequest) => [
  modalityLabel(request.modalidad),
  t('planes.pedidaPor', { usuario: request.solicitadoPor, fecha: formatDate(request.fechaSolicitud) }),
  ...(request.resueltoPor
    ? [t('planes.resueltaPor', { usuario: request.resueltoPor, fecha: formatDate(request.fechaResolucion) })]
    : []),
].join(' · ');

export const requestStatusLabel = (status: PlanRequestStatus) => etiqueta('solicitud', status);

export const requestStatusBadge = (status: PlanRequestStatus) => {
  const kind = { PENDIENTE: 'status-next', APROBADA: 'status-ready', RECHAZADA: 'status-locked', CANCELADA: 'status-off' }[status];
  return `<span class="status-badge ${kind}">${escapeHtml(requestStatusLabel(status))}</span>`;
};

/** Barra de uso: naranja desde el 80 % y roja al llegar al límite. */
export const usageRow = (label: string, used: number, max: number | null) => {
  const percent = max ? Math.min(100, Math.round((used / max) * 100)) : 0;
  const state = max !== null && used >= max ? 'is-full' : max !== null && percent >= 80 ? 'is-warning' : '';
  const limit = max === null ? t('planes.sinLimite') : t('planes.usoDe', { usado: used, maximo: max });
  return `
    <div class="usage-row ${state}">
      <div class="usage-copy"><strong>${escapeHtml(label)}</strong><code>${escapeHtml(limit)}</code></div>
      <div class="usage-track" role="progressbar" aria-label="${escapeHtml(label)}" aria-valuemin="0"
        aria-valuemax="${max ?? used}" aria-valuenow="${used}"><span style="width: ${max === null ? 0 : percent}%"></span></div>
    </div>`;
};

/** Vigencia del plan en una línea: vencido, días que faltan o sin vencimiento. */
export const validityText = (state: OrganizationPlan) => {
  if (!state.vence) return t('planes.sinVencimiento');
  if (state.vencido) return t('planes.pruebaVencida', { fecha: formatDay(state.vence) });
  const days = state.diasRestantes ?? 0;
  return t(days === 1 ? 'planes.vence.uno' : 'planes.vence.otros', { fecha: formatDay(state.vence), n: days });
};
