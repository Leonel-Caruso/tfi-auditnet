// Presentación de planes, uso y solicitudes de cambio (Mi plan, Organizaciones y Planes).
import type { OrganizationPlan, Plan, PlanRequest, PlanRequestStatus } from './api';
import { escapeHtml } from './format';

export const formatDay = (value: string | null | undefined) =>
  value ? new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' }).format(new Date(value)) : '—';

/** Plan a medida: los límites se acuerdan con cada cliente. */
export const isCustomPlan = (plan: Plan) => plan.maxDispositivos === null || plan.maxUsuarios === null;

export const isTrialPlan = (plan: Plan) => plan.diasVigencia !== null;

export const formatPrice = (plan: Plan) => {
  if (plan.precioMensualUsd !== null) {
    return `USD ${new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 }).format(plan.precioMensualUsd)} / mes`;
  }
  return isCustomPlan(plan) ? 'a cotizar' : 'bonificado';
};

/** Límites en una frase: "hasta 50 dispositivos y 15 usuarios activos" o "dispositivos y usuarios a medida". */
export const limitsText = (plan: Plan) => (isCustomPlan(plan)
  ? 'dispositivos y usuarios a medida'
  : `hasta ${plan.maxDispositivos} dispositivos y ${plan.maxUsuarios} usuarios activos`);

/** Tarjeta del catálogo; con current marca el plan de la organización. */
export const planCard = (plan: Plan, current: boolean, action = '') => `
  <article class="plan-card ${current ? 'is-current' : ''}">
    <h3>${escapeHtml(plan.nombre)}${current ? '<span class="status-badge status-strong">actual</span>' : ''}</h3>
    <strong>${escapeHtml(formatPrice(plan))}</strong>
    <ul>
      <li>${escapeHtml(limitsText(plan))}</li>
      ${plan.diasVigencia ? `<li>prueba de hasta ${plan.diasVigencia} días</li>` : ''}
    </ul>
    ${plan.condiciones ? `<p>${escapeHtml(plan.condiciones)}</p>` : ''}
    ${action}
  </article>`;

export const MODALITY_LABEL: Record<PlanRequest['modalidad'], string> = { MENSUAL: 'mensual', ANUAL: 'anual' };

export const REQUEST_STATUS_LABEL: Record<PlanRequestStatus, string> = {
  PENDIENTE: 'pendiente',
  APROBADA: 'aprobada',
  RECHAZADA: 'rechazada',
  CANCELADA: 'cancelada',
};

export const requestStatusBadge = (status: PlanRequestStatus) => {
  const kind = { PENDIENTE: 'status-next', APROBADA: 'status-ready', RECHAZADA: 'status-locked', CANCELADA: 'status-off' }[status];
  return `<span class="status-badge ${kind}">${escapeHtml(REQUEST_STATUS_LABEL[status])}</span>`;
};

/** Barra de uso: naranja desde el 80 % y roja al llegar al límite. */
export const usageRow = (label: string, used: number, max: number | null) => {
  const percent = max ? Math.min(100, Math.round((used / max) * 100)) : 0;
  const state = max !== null && used >= max ? 'is-full' : max !== null && percent >= 80 ? 'is-warning' : '';
  const limit = max === null ? 'sin límite' : `${used} de ${max}`;
  return `
    <div class="usage-row ${state}">
      <div class="usage-copy"><strong>${escapeHtml(label)}</strong><code>${escapeHtml(limit)}</code></div>
      <div class="usage-track" role="progressbar" aria-label="${escapeHtml(label)}" aria-valuemin="0"
        aria-valuemax="${max ?? used}" aria-valuenow="${used}"><span style="width: ${max === null ? 0 : percent}%"></span></div>
    </div>`;
};

/** Vigencia del plan en una línea: vencido, días que faltan o sin vencimiento. */
export const validityText = (state: OrganizationPlan) => {
  if (!state.vence) return 'Sin vencimiento.';
  if (state.vencido) return `El período de prueba venció el ${formatDay(state.vence)}: la organización está en modo consulta.`;
  const days = state.diasRestantes ?? 0;
  return `Vence el ${formatDay(state.vence)} (${days === 1 ? 'falta 1 día' : `faltan ${days} días`}).`;
};
