// Piezas de presentación compartidas por el tablero (/dashboard) y el estado del cliente (/client-status).
import type { AuditSummary, DashboardSummary, DeviceRisk, ReviewStatus, Severity, Site, WeeklyEvolution } from './api';
import { code, escapeHtml, formatDate, plural, riskBadge, riskHelp, severityClass, severityCode } from './format';
import { t } from './i18n';

const SEVERITIES: Severity[] = ['CRITICA', 'ALTA', 'MEDIA', 'BAJA'];

const shortDate = (isoDate: string) => {
  const [, month, day] = isoDate.split('-');
  return `${day}/${month}`;
};

/** Banner con el nivel general de riesgo y qué significa. */
export const riskBannerHtml = (summary: DashboardSummary, scope: string) => `
  <div class="risk-banner risk-${summary.nivelRiesgo.toLowerCase()}">
    <div>
      <span class="metric-label">${escapeHtml(t('tablero.nivelRiesgo', { alcance: scope }))}</span>
      <div class="risk-banner-level">${riskBadge(summary.nivelRiesgo)}</div>
      <p>${escapeHtml(riskHelp(summary.nivelRiesgo))}</p>
    </div>
    <small>${escapeHtml(t('tablero.calculado', { fecha: formatDate(summary.generado) }))}</small>
  </div>`;

/** Indicadores principales; cada uno lleva al módulo de detalle. Sin estado de revisión, esos dos van con "—". */
export const kpiHtml = (summary: DashboardSummary, review: ReviewStatus | null = null) => {
  const { dispositivos, hallazgos, alertas, auditorias } = summary;
  const criticalHigh = (hallazgos.pendientesPorSeveridad.CRITICA ?? 0) + (hallazgos.pendientesPorSeveridad.ALTA ?? 0);
  const tiles = [
    {
      href: '/devices', label: t('tablero.dispositivosAuditados'),
      value: `${dispositivos.auditados}/${dispositivos.registrados}`,
      note: dispositivos.activosSinAuditar > 0
        ? plural(dispositivos.activosSinAuditar, 'tablero.sinAuditar')
        : t('tablero.todosAuditados'),
    },
    {
      href: '/history', label: t('tablero.cumplimiento'),
      value: auditorias.cumplimientoActual == null ? '—' : `${auditorias.cumplimientoActual}%`,
      note: t('tablero.cumplimientoNota'),
    },
    {
      href: '/results?estado=PENDIENTES', label: t('tablero.pendientes'), value: String(hallazgos.pendientes),
      note: t('tablero.pendientesNota', { revision: hallazgos.enRevision, cerrados: hallazgos.cerrados }),
    },
    {
      href: '/results?estado=PENDIENTES', label: t('tablero.criticosAltos'), value: String(criticalHigh),
      note: t('tablero.criticosAltosNota'), alert: criticalHigh > 0,
    },
    {
      href: '/alerts', label: t('tablero.alertasVigentes'), value: String(alertas.vigentes),
      note: t('tablero.alertasNota', { activas: alertas.activas, atencion: alertas.enAtencion }),
      alert: alertas.criticasVigentes > 0,
    },
    {
      href: '/history', label: t('tablero.auditorias30'), value: String(auditorias.ultimos30Dias),
      note: t('tablero.enTotal', { total: auditorias.total }),
    },
    {
      href: '/results?estado=VENCIDOS', label: t('tablero.vencidos'),
      value: review ? String(review.hallazgosVencidos) : '—',
      note: t('tablero.vencidosNota'), alert: (review?.hallazgosVencidos ?? 0) > 0,
    },
    {
      href: '/devices', label: t('tablero.desactualizadas'),
      value: review ? String(review.dispositivosDesactualizados) : '—',
      note: review?.politica
        ? t('tablero.desactualizadasNota', { dias: review.politica.diasConfiguracion })
        : t('tablero.desactualizadasNotaSinPolitica'),
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
    return `<p class="log-muted">${escapeHtml(t('tablero.sinHallazgos'))}</p>`;
  }
  return `
    <div class="viz-bars" role="list">
      ${SEVERITIES.map((severity) => {
        const value = pending[severity] ?? 0;
        const tip = `${severityCode(severity)}: ${plural(value, 'tablero.pendiente')} · ${plural(closed[severity] ?? 0, 'tablero.cerrado')}`;
        return `
          <a class="viz-bar-row" role="listitem" href="/results?estado=PENDIENTES&severidad=${severity}" data-tip="${escapeHtml(tip)}">
            <span class="severity-badge ${severityClass(severity)}">${escapeHtml(severityCode(severity))}</span>
            <span class="viz-bar-track"><span class="viz-bar" style="width:${(value / max) * 100}%"></span></span>
            <span class="viz-bar-value"><strong>${value}</strong><small>${escapeHtml(t('tablero.cerradosCorto', { n: closed[severity] ?? 0 }))}</small></span>
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
      <span><i class="viz-swatch viz-series-1"></i>${escapeHtml(t('tablero.detectados'))}</span>
      <span><i class="viz-swatch viz-series-2"></i>${escapeHtml(t('tablero.cerrados'))}</span>
    </div>
    <div class="viz-columns" role="img" aria-label="${escapeHtml(t('tablero.evolucionAyuda', { semanas: points.length }))}">
      <div class="viz-grid">${ticks.map((tick) => `<span style="bottom:${(tick / max) * 100}%"><em>${tick}</em></span>`).join('')}</div>
      ${points.map((point) => `
        <div class="viz-column-group" tabindex="0"
          data-tip="${escapeHtml(`${t('tablero.semanaDel', { fecha: shortDate(point.semana) })}: ${plural(point.hallazgosDetectados, 'tablero.detectado')} · ${plural(point.hallazgosCerrados, 'tablero.cerrado')} · ${plural(point.auditorias, 'tablero.auditoria')}`)}">
          <div class="viz-column-pair">
            <span class="viz-column viz-series-1" style="height:${height(point.hallazgosDetectados)}"></span>
            <span class="viz-column viz-series-2" style="height:${height(point.hallazgosCerrados)}"></span>
          </div>
          <small>${shortDate(point.semana)}</small>
        </div>`).join('')}
    </div>
    <details class="viz-table">
      <summary>${escapeHtml(t('tablero.verTabla'))}</summary>
      <table class="data-table">
        <thead><tr><th>${escapeHtml(t('tablero.colSemana'))}</th><th>${escapeHtml(t('tablero.colAuditorias'))}</th><th>${escapeHtml(t('tablero.colDetectados'))}</th><th>${escapeHtml(t('tablero.colCerrados'))}</th></tr></thead>
        <tbody>${points.map((point) => `
          <tr><td>${shortDate(point.semana)}</td><td>${point.auditorias}</td><td>${point.hallazgosDetectados}</td><td>${point.hallazgosCerrados}</td></tr>`).join('')}
        </tbody>
      </table>
    </details>`;
};

export const recentAuditsHtml = (audits: AuditSummary[], deviceName: (id: number) => string) => audits.length === 0
  ? `<p class="log-muted">${escapeHtml(t('tablero.sinAuditorias'))}</p>`
  : `<ul class="compare-list">${audits.map((audit) => `
      <li class="compare-item">
        <a class="inline-link" href="/history?auditoria=${audit.id}"><code>${code('AUD', audit.id)}</code></a>
        <span>${escapeHtml(t('tablero.auditoriaReciente', { dispositivo: deviceName(audit.dispositivoId), cumplidas: audit.reglasCumplidas, total: audit.totalReglas }))}</span>
        <small>${escapeHtml(formatDate(audit.fechaEjecucion))}</small>
      </li>`).join('')}</ul>`;

export const deviceRiskRowsHtml = (devices: DeviceRisk[], sites: Site[]) => devices.map((device) => `
  <tr>
    <td><a class="inline-link" href="/results?estado=PENDIENTES&dispositivo=${device.dispositivoId}"><strong>${escapeHtml(device.identificador)}</strong></a><small>${escapeHtml(device.nombre)}</small></td>
    <td>${escapeHtml(device.sedeId == null ? t('tablero.sinSede') : sites.find((site) => site.id === device.sedeId)?.nombre ?? t('tablero.sedeNumero', { id: device.sedeId }))}</td>
    <td><span class="severity-badge ${severityClass(device.criticidad)}">${escapeHtml(severityCode(device.criticidad))}</span></td>
    <td>${riskBadge(device.nivel)}</td>
    <td><strong>${device.pendientes}</strong><small>${escapeHtml(t('tablero.criticosAltosCorto', { criticos: device.criticosPendientes, altos: device.altosPendientes }))}</small></td>
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
