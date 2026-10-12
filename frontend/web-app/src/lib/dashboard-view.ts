// Piezas de presentación compartidas por el tablero (/dashboard) y el estado del cliente (/client-status).
import type { AuditSummary, DashboardSummary, DeviceRisk, Severity, Site, WeeklyEvolution } from './api';
import { RISK_HELP, code, escapeHtml, formatDate, riskBadge, severityClass } from './format';

const SEVERITIES: Severity[] = ['CRITICA', 'ALTA', 'MEDIA', 'BAJA'];

const plural = (value: number, singular: string, pluralForm: string) => `${value} ${value === 1 ? singular : pluralForm}`;

const shortDate = (isoDate: string) => {
  const [, month, day] = isoDate.split('-');
  return `${day}/${month}`;
};

/** Banner con el nivel general de riesgo y qué significa. */
export const riskBannerHtml = (summary: DashboardSummary, scope: string) => `
  <div class="risk-banner risk-${summary.nivelRiesgo.toLowerCase()}">
    <div>
      <span class="metric-label">nivel general de riesgo · ${escapeHtml(scope)}</span>
      <div class="risk-banner-level">${riskBadge(summary.nivelRiesgo)}</div>
      <p>${escapeHtml(RISK_HELP[summary.nivelRiesgo])}</p>
    </div>
    <small>calculado ${escapeHtml(formatDate(summary.generado))}</small>
  </div>`;

/** Indicadores principales; cada uno lleva al módulo de detalle. */
export const kpiHtml = (summary: DashboardSummary) => {
  const { dispositivos, hallazgos, alertas, auditorias } = summary;
  const criticalHigh = (hallazgos.pendientesPorSeveridad.CRITICA ?? 0) + (hallazgos.pendientesPorSeveridad.ALTA ?? 0);
  const tiles = [
    {
      href: '/devices', label: 'dispositivos auditados', value: `${dispositivos.auditados}/${dispositivos.registrados}`,
      note: dispositivos.activosSinAuditar > 0
        ? `${plural(dispositivos.activosSinAuditar, 'activo sin auditar', 'activos sin auditar')}`
        : 'Todos los activos tienen auditoría',
    },
    {
      href: '/history', label: 'cumplimiento actual',
      value: auditorias.cumplimientoActual == null ? '—' : `${auditorias.cumplimientoActual}%`,
      note: 'Reglas cumplidas en la última auditoría de cada equipo',
    },
    {
      href: '/results?estado=PENDIENTES', label: 'hallazgos pendientes', value: String(hallazgos.pendientes),
      note: `${hallazgos.enRevision} en revisión · ${hallazgos.cerrados} cerrados`,
    },
    {
      href: '/results?estado=PENDIENTES', label: 'críticos / altos pendientes', value: String(criticalHigh),
      note: 'Primeros en la lista de pendientes', alert: criticalHigh > 0,
    },
    {
      href: '/alerts', label: 'alertas vigentes', value: String(alertas.vigentes),
      note: `${alertas.activas} sin atender · ${alertas.enAtencion} en atención`, alert: alertas.criticasVigentes > 0,
    },
    {
      href: '/history', label: 'auditorías (30 días)', value: String(auditorias.ultimos30Dias),
      note: `${auditorias.total} en total`,
    },
  ];
  return tiles.map((tile) => `
    <a class="metric-card metric-link${tile.alert ? ' is-alert' : ''}" href="${tile.href}">
      <span class="metric-label">${escapeHtml(tile.label)}</span>
      <strong>${escapeHtml(tile.value)}</strong>
      <small>${escapeHtml(tile.note)}</small>
    </a>`).join('');
};

/** Barras horizontales: hallazgos pendientes por severidad (una sola serie; los cerrados van como texto). */
export const severityChartHtml = (summary: DashboardSummary) => {
  const pending = summary.hallazgos.pendientesPorSeveridad;
  const closed = summary.hallazgos.cerradosPorSeveridad;
  const max = Math.max(1, ...SEVERITIES.map((severity) => pending[severity] ?? 0));
  if (summary.hallazgos.pendientes + summary.hallazgos.cerrados === 0) {
    return '<p class="log-muted">Todavía no hay hallazgos registrados.</p>';
  }
  return `
    <div class="viz-bars" role="list">
      ${SEVERITIES.map((severity) => {
        const value = pending[severity] ?? 0;
        const tip = `${severity}: ${plural(value, 'pendiente', 'pendientes')} · ${plural(closed[severity] ?? 0, 'cerrado', 'cerrados')}`;
        return `
          <a class="viz-bar-row" role="listitem" href="/results?estado=PENDIENTES&severidad=${severity}" data-tip="${escapeHtml(tip)}">
            <span class="severity-badge ${severityClass(severity)}">${severity}</span>
            <span class="viz-bar-track"><span class="viz-bar" style="width:${(value / max) * 100}%"></span></span>
            <span class="viz-bar-value"><strong>${value}</strong><small>${closed[severity] ?? 0} cerr.</small></span>
          </a>`;
      }).join('')}
    </div>`;
};

/** Columnas agrupadas por semana: hallazgos detectados y cerrados (mismo eje: cantidad de hallazgos). */
export const evolutionChartHtml = (points: WeeklyEvolution[]) => {
  const max = Math.max(1, ...points.flatMap((point) => [point.hallazgosDetectados, point.hallazgosCerrados]));
  const ticks = [max, Math.round(max / 2), 0].filter((value, index, all) => all.indexOf(value) === index);
  const height = (value: number) => `${(value / max) * 100}%`;
  return `
    <div class="viz-legend" aria-hidden="true">
      <span><i class="viz-swatch viz-series-1"></i>detectados</span>
      <span><i class="viz-swatch viz-series-2"></i>cerrados</span>
    </div>
    <div class="viz-columns" role="img" aria-label="Hallazgos detectados y cerrados por semana, últimas ${points.length} semanas">
      <div class="viz-grid">${ticks.map((tick) => `<span style="bottom:${(tick / max) * 100}%"><em>${tick}</em></span>`).join('')}</div>
      ${points.map((point) => `
        <div class="viz-column-group" tabindex="0"
          data-tip="${escapeHtml(`Semana del ${shortDate(point.semana)}: ${plural(point.hallazgosDetectados, 'detectado', 'detectados')} · ${plural(point.hallazgosCerrados, 'cerrado', 'cerrados')} · ${plural(point.auditorias, 'auditoría', 'auditorías')}`)}">
          <div class="viz-column-pair">
            <span class="viz-column viz-series-1" style="height:${height(point.hallazgosDetectados)}"></span>
            <span class="viz-column viz-series-2" style="height:${height(point.hallazgosCerrados)}"></span>
          </div>
          <small>${shortDate(point.semana)}</small>
        </div>`).join('')}
    </div>
    <details class="viz-table">
      <summary>ver tabla</summary>
      <table class="data-table">
        <thead><tr><th>SEMANA</th><th>AUDITORÍAS</th><th>DETECTADOS</th><th>CERRADOS</th></tr></thead>
        <tbody>${points.map((point) => `
          <tr><td>${shortDate(point.semana)}</td><td>${point.auditorias}</td><td>${point.hallazgosDetectados}</td><td>${point.hallazgosCerrados}</td></tr>`).join('')}
        </tbody>
      </table>
    </details>`;
};

export const recentAuditsHtml = (audits: AuditSummary[], deviceName: (id: number) => string) => audits.length === 0
  ? '<p class="log-muted">Todavía no se ejecutaron auditorías.</p>'
  : `<ul class="compare-list">${audits.map((audit) => `
      <li class="compare-item">
        <a class="inline-link" href="/history?auditoria=${audit.id}"><code>${code('AUD', audit.id)}</code></a>
        <span>${escapeHtml(deviceName(audit.dispositivoId))} · ${audit.reglasCumplidas}/${audit.totalReglas} reglas</span>
        <small>${escapeHtml(formatDate(audit.fechaEjecucion))}</small>
      </li>`).join('')}</ul>`;

export const deviceRiskRowsHtml = (devices: DeviceRisk[], sites: Site[]) => devices.map((device) => `
  <tr>
    <td><a class="inline-link" href="/results?estado=PENDIENTES&dispositivo=${device.dispositivoId}"><strong>${escapeHtml(device.identificador)}</strong></a><small>${escapeHtml(device.nombre)}</small></td>
    <td>${escapeHtml(device.sedeId == null ? 'sin sede' : sites.find((site) => site.id === device.sedeId)?.nombre ?? `sede #${device.sedeId}`)}</td>
    <td><span class="severity-badge ${severityClass(device.criticidad)}">${escapeHtml(device.criticidad)}</span></td>
    <td>${riskBadge(device.nivel)}</td>
    <td><strong>${device.pendientes}</strong><small>${device.criticosPendientes} crít. · ${device.altosPendientes} altos</small></td>
    <td>${device.alertasVigentes > 0 ? `<a class="inline-link" href="/alerts?dispositivo=${device.dispositivoId}">${device.alertasVigentes} →</a>` : '0'}</td>
    <td><span class="log-nowrap">${escapeHtml(formatDate(device.ultimaAuditoria))}</span></td>
  </tr>`).join('');

/** Tooltip compartido para las marcas con data-tip (barras, columnas). */
export const wireTooltips = (root: ParentNode) => {
  let tooltip = document.querySelector<HTMLElement>('#viz-tooltip');
  if (!tooltip) {
    tooltip = document.createElement('div');
    tooltip.id = 'viz-tooltip';
    tooltip.className = 'viz-tooltip is-hidden';
    tooltip.setAttribute('role', 'tooltip');
    document.body.append(tooltip);
  }
  const tip = tooltip;
  const place = (target: HTMLElement, x: number, y: number) => {
    tip.textContent = target.dataset.tip ?? '';
    tip.classList.remove('is-hidden');
    const width = tip.offsetWidth;
    tip.style.left = `${Math.min(Math.max(8, x - width / 2), window.innerWidth - width - 8)}px`;
    tip.style.top = `${Math.max(8, y - tip.offsetHeight - 12)}px`;
  };
  root.querySelectorAll<HTMLElement>('[data-tip]').forEach((target) => {
    target.addEventListener('mousemove', (event) => place(target, event.clientX, event.clientY));
    target.addEventListener('focus', () => {
      const box = target.getBoundingClientRect();
      place(target, box.left + box.width / 2, box.top);
    });
    target.addEventListener('mouseleave', () => tip.classList.add('is-hidden'));
    target.addEventListener('blur', () => tip.classList.add('is-hidden'));
  });
};
