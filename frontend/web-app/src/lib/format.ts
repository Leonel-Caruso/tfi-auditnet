// Utilidades de presentación compartidas entre pantallas.
import type { AlertStatus, FindingStatus, RiskLevel } from './api';
import { etiqueta, localeActual, t, type Clave } from './i18n';

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
    return new Intl.DateTimeFormat(localeActual(), { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
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
  if (Number.isNaN(date.getTime())) throw new Error(t('general.fechaInvalida'));
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

/** Nombre de un estado en minúscula ("en revisión"); el badge muestra el código (EN_REVISION). */
export const findingStatusLabel = (status: FindingStatus) => etiqueta('hallazgoTexto', status);

export const findingStatusBadge = (status: FindingStatus) => {
  const kind = status === 'ABIERTO' ? 'status-strong'
    : status === 'EN_REVISION' ? 'status-next'
      : 'status-ready';
  return `<span class="status-badge ${kind}">${escapeHtml(etiqueta('hallazgo', status))}</span>`;
};

// Nivel general de riesgo

export const riskLabel = (level: RiskLevel) => etiqueta('riesgo', level);

/** Qué significa cada nivel, para mostrarlo junto al indicador. */
export const riskHelp = (level: RiskLevel) => etiqueta('riesgoAyuda', level);

export const riskBadge = (level: RiskLevel) => {
  const kind = level === 'ALTO' ? 'severity-critical'
    : level === 'MEDIO' ? 'severity-high'
      : level === 'BAJO' ? 'severity-low'
        : 'status-next';
  return `<span class="severity-badge risk-badge ${kind}">${escapeHtml(riskLabel(level))}</span>`;
};

export const alertStatusLabel = (status: AlertStatus) => etiqueta('alertaTexto', status);

export const alertStatusBadge = (status: AlertStatus) => {
  const kind = status === 'ACTIVA' ? 'status-strong' : status === 'EN_ATENCION' ? 'status-next' : 'status-ready';
  return `<span class="status-badge ${kind}">${escapeHtml(etiqueta('alerta', status))}</span>`;
};

/** Código de severidad o criticidad (CRITICA, ALTA...) en el idioma actual. */
export const severityCode = (value: string | null | undefined) => etiqueta('severidad', value);

/** ACTIVO / INACTIVO en el idioma actual. */
export const recordStatusCode = (value: string | null | undefined) => etiqueta('registro', value);

/** Singular o plural: plural(n, 'tablero.pendiente') usa tablero.pendiente.uno o tablero.pendiente.otros. */
export const plural = (cantidad: number, clave: string, datos: Record<string, string | number> = {}) =>
  t(`${clave}.${cantidad === 1 ? 'uno' : 'otros'}` as Clave, { n: cantidad, ...datos });
