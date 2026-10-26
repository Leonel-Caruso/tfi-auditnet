// Documento formal del reporte de auditoría. Es el mismo HTML en pantalla y en el PDF (el navegador lo imprime
// con los estilos @media print de global.css). Sale en el idioma de la consola; los datos cargados (reglas,
// evidencia, recomendaciones) quedan como se registraron.
import type { AuditReport, FindingStatus } from './api';
import { code, escapeHtml, plural } from './format';
import { etiqueta, localeActual, t } from './i18n';
import { evidencia } from './evidencia';

export type ReportContext = {
  orgName: (id: number | null) => string;
  siteName: (id: number | null) => string;
  deviceName: (id: number) => string;
};

const SEVERITIES = ['CRITICA', 'ALTA', 'MEDIA', 'BAJA'] as const;
const severityLabel = (severity: string) => etiqueta('informeSeveridad', severity);
const statusLabel = (status: string) => etiqueta('informeEstado', status);
const resultLabel = (result: string) => etiqueta('informeResultado', result);
const PENDING: FindingStatus[] = ['ABIERTO', 'EN_REVISION'];

// Fechas en el formato del idioma de la consola (23/10/2026 o 10/23/2026), con el año completo.
const dayOnly = (value: string) =>
  new Intl.DateTimeFormat(localeActual(), { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(value));
const dateTime = (value: string) => new Intl.DateTimeFormat(localeActual(), {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
}).format(new Date(value));
const joinList = (items: string[]) => (items.length <= 1
  ? items.join('')
  : `${items.slice(0, -1).join(', ')} ${t('informe.y')} ${items[items.length - 1]}`);
const sevClass = (severity: string) => `rpt-sev-${severity.toLowerCase()}`;

export function reportPeriod(report: AuditReport): string {
  const { desde, hasta, auditoriaIds } = report.criterios;
  if (desde && hasta) return t('informe.periodoEntre', { desde: dayOnly(desde), hasta: dayOnly(hasta) });
  if (desde) return t('informe.periodoDesde', { fecha: dayOnly(desde) });
  if (hasta) return t('informe.periodoHasta', { fecha: dayOnly(hasta) });
  return auditoriaIds.length ? t('informe.auditoriasSeleccionadas') : t('informe.sinLimite');
}

/** Párrafos del resumen ejecutivo, redactados a partir de los números del reporte. */
export function executiveSummary(report: AuditReport, context: ReportContext): string[] {
  const { resumen, hallazgos } = report.contenido;
  const { desde, hasta, auditoriaIds } = report.criterios;
  const scope = report.criterios.organizacionId == null
    ? t('informe.organizacionesCliente')
    : context.orgName(report.criterios.organizacionId);
  const period = desde && hasta
    ? t('informe.ejecutadasEntre', { desde: dayOnly(desde), hasta: dayOnly(hasta) })
    : auditoriaIds.length ? t('informe.seleccionadas', { auditorias: auditoriaIds.map((id) => code('AUD', id)).join(', ') }) : '';

  const paragraphs: string[] = [];
  let first = t('informe.comprende', {
    auditorias: plural(resumen.auditorias, 'informe.auditoria'),
    dispositivos: plural(resumen.dispositivos, 'informe.dispositivo'),
    alcance: scope,
    periodo: period,
  });
  if (resumen.reglasEvaluadas > 0) {
    first += ` ${t('informe.evaluadas', {
      reglas: plural(resumen.reglasEvaluadas, 'informe.regla'),
      cumplidas: resumen.reglasCumplidas,
      cumplimiento: resumen.cumplimiento ?? 0,
    })}`;
  }
  paragraphs.push(first);

  if (resumen.hallazgos === 0) {
    paragraphs.push(t('informe.sinHallazgosResumen'));
    return paragraphs;
  }

  const bySeverity = SEVERITIES
    .map((severity) => ({ severity, total: resumen.hallazgosPorSeveridad[severity] ?? 0 }))
    .filter((item) => item.total > 0)
    .map((item) => t('informe.deSeveridad', { n: item.total, severidad: severityLabel(item.severity).toLowerCase() }));
  const pendingCritical = hallazgos.filter((finding) => PENDING.includes(finding.estado)
    && (finding.severidad === 'CRITICA' || finding.severidad === 'ALTA')).length;

  let second = t('informe.identificados', { hallazgos: plural(resumen.hallazgos, 'informe.hallazgo'), detalle: joinList(bySeverity) });
  second += ` ${resumen.hallazgosPendientes === 0
    ? t('informe.todosResueltos')
    : plural(resumen.hallazgosPendientes, 'informe.permanecen')}`;
  paragraphs.push(second);

  if (pendingCritical > 0) {
    paragraphs.push(plural(pendingCritical, 'informe.priorizar'));
  } else if (resumen.hallazgosPendientes > 0) {
    paragraphs.push(t('informe.pendientesMenores'));
  }
  return paragraphs;
}

export function reportDocumentHtml(report: AuditReport, context: ReportContext): string {
  const { resumen, auditorias, hallazgos, inconsistencias } = report.contenido;
  const criteria = report.criterios;
  const rep = code('REP', report.id);
  const organization = criteria.organizacionId == null
    ? t('informe.todasOrganizaciones')
    : context.orgName(criteria.organizacionId);
  const devices = criteria.dispositivoIds.length
    ? criteria.dispositivoIds.map(context.deviceName).join(', ')
    : t('informe.todosDispositivos', { dispositivos: plural(resumen.dispositivos, 'informe.dispositivoAuditado') });
  const withDeviations = resumen.resultadoGeneral === 'CON_DESVIOS';
  const maxCount = Math.max(1, ...SEVERITIES.map((severity) => resumen.hallazgosPorSeveridad[severity] ?? 0));
  const percent = (value: number) => (resumen.hallazgos === 0 ? '0 %' : `${Math.round((value * 100) / resumen.hallazgos)} %`);

  const severityRows = SEVERITIES.map((severity) => {
    const total = resumen.hallazgosPorSeveridad[severity] ?? 0;
    const pending = hallazgos.filter((finding) => finding.severidad === severity && PENDING.includes(finding.estado)).length;
    return `
      <tr>
        <td><span class="rpt-sev ${sevClass(severity)}">${escapeHtml(severityLabel(severity))}</span></td>
        <td class="rpt-num">${total}</td>
        <td class="rpt-num">${pending}</td>
        <td class="rpt-num">${percent(total)}</td>
        <td class="rpt-bar-cell"><span class="rpt-bar ${sevClass(severity)}" style="width:${Math.round((total * 100) / maxCount)}%"></span></td>
      </tr>`;
  }).join('');

  const statusRows = Object.entries(resumen.hallazgosPorEstado).map(([status, total]) => `
      <tr><td>${escapeHtml(statusLabel(status))}</td><td class="rpt-num">${total}</td><td class="rpt-num">${percent(total)}</td></tr>`).join('');

  const auditRows = auditorias.map((audit) => `
      <tr>
        <td><a href="/history?auditoria=${audit.id}">${code('AUD', audit.id)}</a></td>
        <td class="rpt-nowrap">${escapeHtml(dateTime(audit.fechaEjecucion))}</td>
        <td><strong class="rpt-nowrap">${escapeHtml(audit.dispositivoIdentificador ?? `#${audit.dispositivoId}`)}</strong><small>${escapeHtml([audit.criticidadDispositivo ? t('informe.criticidad', { criticidad: severityLabel(audit.criticidadDispositivo).toLowerCase() }) : null, context.siteName(audit.sedeId)].filter(Boolean).join(' · '))}</small></td>
        <td>${escapeHtml(audit.baselineNombre ?? `#${audit.baselineId}`)}${audit.baselineVersion ? `<small>${escapeHtml(t('informe.version', { version: audit.baselineVersion }))}</small>` : ''}</td>
        <td class="rpt-nowrap">${code('CFG', audit.configuracionId)}${audit.configuracionVersion ? `<small>${escapeHtml(t('informe.version', { version: audit.configuracionVersion }))}</small>` : ''}</td>
        <td class="rpt-num">${audit.reglasCumplidas}/${audit.totalReglas}</td>
        <td class="rpt-num">${audit.totalHallazgos}</td>
        <td>${escapeHtml(resultLabel(audit.resultado))}</td>
      </tr>`).join('');

  const findingBlocks = hallazgos.map((finding, index) => `
      <article class="rpt-finding">
        <header>
          <span class="rpt-finding-index">5.${index + 1}</span>
          <div>
            <strong>${escapeHtml(finding.nombreRegla)}</strong>
            <small>${escapeHtml(finding.codigoRegla)} · ${code('FND', finding.id)} · ${escapeHtml(finding.dispositivoIdentificador ?? `#${finding.dispositivoId}`)} · ${code('AUD', finding.auditoriaId)} · ${escapeHtml(t('informe.detectado', { fecha: dateTime(finding.fechaDeteccion) }))}</small>
          </div>
          <span class="rpt-sev ${sevClass(finding.severidad)}">${escapeHtml(severityLabel(finding.severidad))}</span>
        </header>
        <dl>
          <dt>${t('informe.estado')}</dt><dd>${escapeHtml(statusLabel(finding.estado))}</dd>
          ${finding.impacto ? `<dt>${t('informe.impacto')}</dt><dd>${escapeHtml(finding.impacto)}</dd>` : ''}
          <dt>${t('informe.evidencia')}</dt><dd><pre>${escapeHtml(evidencia(finding.evidencia))}</pre></dd>
          <dt>${t('informe.recomendacion')}</dt><dd>${escapeHtml(finding.recomendacion)}</dd>
        </dl>
      </article>`).join('');

  const observations = [
    t('informe.fotoFija', { fecha: dateTime(report.fechaGeneracion) }),
    t('informe.calculoCumplimiento'),
    ...inconsistencias,
  ];

  return `
    <div class="rpt">
      <header class="rpt-masthead">
        <div class="rpt-brand">
          <span class="rpt-logo">A_</span>
          <div><strong>AuditNet</strong><small>${t('informe.marca')}</small></div>
        </div>
        <div class="rpt-docid">
          <span>${t('informe.reporteAuditoria')}</span>
          <strong>${rep}</strong>
        </div>
      </header>

      <h1 class="rpt-title">${escapeHtml(report.titulo)}</h1>

      <table class="rpt-meta">
        <tr><th>${t('informe.organizacion')}</th><td>${escapeHtml(organization)}</td><th>${t('informe.periodo')}</th><td>${escapeHtml(reportPeriod(report))}</td></tr>
        <tr><th>${t('informe.emitidoPor')}</th><td>${escapeHtml(report.generadoPor)}</td><th>${t('informe.fechaEmision')}</th><td>${escapeHtml(dateTime(report.fechaGeneracion))}</td></tr>
        <tr><th>${t('informe.resultadoGeneral')}</th><td><span class="rpt-result ${withDeviations ? 'is-deviation' : 'is-ok'}">${t(withDeviations ? 'informe.conDesvios' : 'informe.sinDesvios')}</span></td><th>${t('informe.severidadMaxima')}</th><td>${escapeHtml(severityLabel(resumen.severidadMaxima))}</td></tr>
      </table>
      ${inconsistencias.length ? `<p class="rpt-warning"><strong>${t('informe.datosIncompletos')}</strong> ${inconsistencias.map(escapeHtml).join(' ')} ${t('informe.verApartado6')}</p>` : ''}

      <section class="rpt-section">
        <h2><span>1</span>${t('informe.resumenEjecutivo')}</h2>
        ${executiveSummary(report, context).map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join('')}
        <div class="rpt-figures">
          <div><strong>${resumen.cumplimiento == null ? '—' : `${resumen.cumplimiento} %`}</strong><span>${t('informe.cumplimiento')}</span><small>${escapeHtml(t('informe.reglasDe', { cumplidas: resumen.reglasCumplidas, evaluadas: resumen.reglasEvaluadas }))}</small></div>
          <div><strong>${resumen.auditorias}</strong><span>${t('informe.auditorias')}</span><small>${escapeHtml(plural(resumen.dispositivos, 'informe.dispositivo'))}</small></div>
          <div><strong>${resumen.hallazgos}</strong><span>${t('informe.hallazgos')}</span><small>${escapeHtml(plural(resumen.hallazgosPendientes, 'informe.pendiente'))}</small></div>
          <div><strong class="${resumen.severidadMaxima === 'CRITICA' || resumen.severidadMaxima === 'ALTA' ? 'is-alert' : ''}">${escapeHtml(severityLabel(resumen.severidadMaxima))}</strong><span>${t('informe.severidadMaxima')}</span><small>${t('informe.entreIncluidos')}</small></div>
        </div>
      </section>

      <section class="rpt-section">
        <h2><span>2</span>${t('informe.alcance')}</h2>
        <table class="rpt-kv">
          <tr><th>${t('informe.organizacion')}</th><td>${escapeHtml(organization)}</td></tr>
          <tr><th>${t('informe.periodo')}</th><td>${escapeHtml(reportPeriod(report))}</td></tr>
          <tr><th>${t('informe.auditorias')}</th><td>${criteria.auditoriaIds.length ? criteria.auditoriaIds.map((id) => code('AUD', id)).join(', ') : t('informe.todasDelPeriodo')}</td></tr>
          <tr><th>${t('informe.dispositivos')}</th><td>${escapeHtml(devices)}</td></tr>
          <tr><th>${t('informe.severidades')}</th><td>${escapeHtml(criteria.severidades.length ? criteria.severidades.map(severityLabel).join(', ') : t('informe.todas'))}</td></tr>
          <tr><th>${t('informe.estadosHallazgo')}</th><td>${escapeHtml(criteria.estados.length ? criteria.estados.map(statusLabel).join(', ') : t('informe.todos'))}</td></tr>
        </table>
      </section>

      <section class="rpt-section">
        <h2><span>3</span>${t('informe.distribucion')}</h2>
        <div class="rpt-two">
          <table class="rpt-table">
            <thead><tr><th>${t('informe.severidad')}</th><th class="rpt-num">${t('informe.total')}</th><th class="rpt-num">${t('informe.pendientes')}</th><th class="rpt-num">%</th><th></th></tr></thead>
            <tbody>${severityRows}</tbody>
          </table>
          <table class="rpt-table">
            <thead><tr><th>${t('informe.estado')}</th><th class="rpt-num">${t('informe.total')}</th><th class="rpt-num">%</th></tr></thead>
            <tbody>${statusRows}</tbody>
          </table>
        </div>
      </section>

      <section class="rpt-section">
        <h2><span>4</span>${t('informe.auditoriasIncluidas')}</h2>
        <table class="rpt-table rpt-audits">
          <thead><tr><th>${t('informe.auditoria')}</th><th>${t('informe.fecha')}</th><th>${t('informe.dispositivoColumna')}</th><th>${t('informe.baseline')}</th><th>${t('informe.configuracion')}</th><th class="rpt-num">${t('informe.reglas')}</th><th class="rpt-num">${t('informe.hallazgos')}</th><th>${t('informe.resultado')}</th></tr></thead>
          <tbody>${auditRows}</tbody>
        </table>
      </section>

      <section class="rpt-section">
        <h2><span>5</span>${t('informe.detalleHallazgos')}</h2>
        ${hallazgos.length === 0 ? `<p>${t('informe.sinHallazgos')}</p>` : findingBlocks}
      </section>

      <section class="rpt-section">
        <h2><span>6</span>${t('informe.observaciones')}</h2>
        <ul class="rpt-notes">${observations.map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul>
      </section>

      <footer class="rpt-footer">
        <span>AuditNet · ${rep}</span>
        <span>${t('informe.pie')}</span>
      </footer>
    </div>`;
}

/** Pie de página del PDF (Chrome/Edge): "AuditNet · REP-001" y "Página X de Y". */
export function printPageStyle(report: AuditReport): string {
  const rep = code('REP', report.id);
  return `@media print { @page {
    @bottom-left { content: "AuditNet · ${rep}"; font: 8pt Inter, system-ui, sans-serif; color: #697078; }
    @bottom-right { content: "${t('informe.pagina')} " counter(page) " ${t('informe.de')} " counter(pages); font: 8pt Inter, system-ui, sans-serif; color: #697078; }
  } }`;
}
