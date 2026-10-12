// Utilidades de presentación compartidas entre pantallas.
import type { AlertStatus, FindingStatus, RiskLevel } from './api';

/** Escapa texto antes de insertarlo como HTML (los datos vienen de usuarios y de configuraciones). */
export const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');

export const formatDate = (value: string | null | undefined) => {
  if (!value) return '—';
  try {
    return new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
  } catch {
    return value;
  }
};

/**
 * Convierte el valor de un input datetime-local (hora local) a ISO-8601 en UTC, o null si está vacío.
 * Con endOfMinute (filtro "hasta") incluye todo ese minuto: 10:30 → 10:30:59.999.
 */
export const localToIso = (value: string, endOfMinute = false) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error('La fecha ingresada no es válida.');
  if (endOfMinute) date.setSeconds(59, 999);
  return date.toISOString();
};

export const code = (prefix: string, id: number | null | undefined) =>
  id == null ? '—' : `${prefix}-${String(id).padStart(3, '0')}`;

export const severityClass = (value: string | null | undefined) => ({
  CRITICA: 'severity-critical',
  ALTA: 'severity-high',
  MEDIA: 'severity-medium',
  BAJA: 'severity-low',
}[value ?? ''] ?? 'severity-low');

export const FINDING_STATUS_LABEL: Record<FindingStatus, string> = {
  ABIERTO: 'abierto',
  EN_REVISION: 'en revisión',
  RESUELTO: 'resuelto',
  ACEPTADO: 'riesgo aceptado',
};

export const findingStatusBadge = (status: FindingStatus) => {
  const kind = status === 'ABIERTO' ? 'status-strong'
    : status === 'EN_REVISION' ? 'status-next'
      : 'status-ready';
  return `<span class="status-badge ${kind}">${escapeHtml(status)}</span>`;
};

// Nivel general de riesgo

export const RISK_LABEL: Record<RiskLevel, string> = {
  ALTO: 'riesgo alto',
  MEDIO: 'riesgo medio',
  BAJO: 'riesgo bajo',
  SIN_DATOS: 'sin auditorías',
};

/** Qué significa cada nivel, para mostrarlo junto al indicador. */
export const RISK_HELP: Record<RiskLevel, string> = {
  ALTO: 'Hay hallazgos CRITICA pendientes o alertas vigentes en equipos de criticidad ALTA o CRITICA.',
  MEDIO: 'Hay hallazgos ALTA pendientes en equipos de criticidad media o baja.',
  BAJO: 'Solo quedan hallazgos MEDIA o BAJA pendientes, o ninguno.',
  SIN_DATOS: 'Todavía no se ejecutaron auditorías.',
};

export const riskBadge = (level: RiskLevel) => {
  const kind = level === 'ALTO' ? 'severity-critical'
    : level === 'MEDIO' ? 'severity-high'
      : level === 'BAJO' ? 'severity-low'
        : 'status-next';
  return `<span class="severity-badge risk-badge ${kind}">${escapeHtml(RISK_LABEL[level] ?? level)}</span>`;
};

export const ALERT_STATUS_LABEL: Record<AlertStatus, string> = {
  ACTIVA: 'activa',
  EN_ATENCION: 'en atención',
  CERRADA: 'cerrada',
};

export const alertStatusBadge = (status: AlertStatus) => {
  const kind = status === 'ACTIVA' ? 'status-strong' : status === 'EN_ATENCION' ? 'status-next' : 'status-ready';
  return `<span class="status-badge ${kind}">${escapeHtml(status)}</span>`;
};
