// Documento formal del reporte de auditoría. Es el mismo HTML en pantalla y en el PDF (el navegador lo imprime
// con los estilos @media print de global.css).
import type { AuditReport, FindingStatus } from './api';
import { code, escapeHtml } from './format';

export type ReportContext = {
  orgName: (id: number | null) => string;
  siteName: (id: number | null) => string;
  deviceName: (id: number) => string;
};

const SEVERITIES = ['CRITICA', 'ALTA', 'MEDIA', 'BAJA'] as const;
const SEVERITY_LABEL: Record<string, string> = { CRITICA: 'Crítica', ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja', NINGUNA: 'Ninguna' };
const STATUS_LABEL: Record<string, string> = {
  ABIERTO: 'Abierto',
  EN_REVISION: 'En revisión',
  RESUELTO: 'Resuelto',
  ACEPTADO: 'Riesgo aceptado',
};
const RESULT_LABEL: Record<string, string> = { CUMPLE: 'Cumple', CON_HALLAZGOS: 'Con hallazgos' };
const PENDING: FindingStatus[] = ['ABIERTO', 'EN_REVISION'];

// Fechas del documento siempre como dd/mm/aaaa y hh:mm (24 h), sin depender del formato regional del navegador.
const pad = (value: number) => String(value).padStart(2, '0');
const dayOnly = (value: string) => {
  const date = new Date(value);
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
};
const dateTime = (value: string) => {
  const date = new Date(value);
  return `${dayOnly(value)} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};
const plural = (count: number, singular: string, pluralForm: string) => `${count} ${count === 1 ? singular : pluralForm}`;
const joinList = (items: string[]) => (items.length <= 1 ? items.join('') : `${items.slice(0, -1).join(', ')} y ${items[items.length - 1]}`);
const sevClass = (severity: string) => `rpt-sev-${severity.toLowerCase()}`;

export function reportPeriod(report: AuditReport): string {
  const { desde, hasta, auditoriaIds } = report.criterios;
  if (desde && hasta) return `${dayOnly(desde)} al ${dayOnly(hasta)}`;
  if (desde) return `desde el ${dayOnly(desde)}`;
  if (hasta) return `hasta el ${dayOnly(hasta)}`;
  return auditoriaIds.length ? 'auditorías seleccionadas' : 'sin límite de fechas';
}

/** Párrafos del resumen ejecutivo, redactados a partir de los números del reporte. */
export function executiveSummary(report: AuditReport, context: ReportContext): string[] {
  const { resumen, hallazgos } = report.contenido;
  const { desde, hasta, auditoriaIds } = report.criterios;
  const scope = report.criterios.organizacionId == null ? 'las organizaciones cliente' : context.orgName(report.criterios.organizacionId);
  const period = desde && hasta
    ? `, ejecutadas entre el ${dayOnly(desde)} y el ${dayOnly(hasta)}`
    : auditoriaIds.length ? `, seleccionadas por número (${auditoriaIds.map((id) => code('AUD', id)).join(', ')})` : '';

  const paragraphs: string[] = [];
  let first = `El presente reporte comprende ${plural(resumen.auditorias, 'auditoría', 'auditorías')} sobre ${plural(resumen.dispositivos, 'dispositivo', 'dispositivos')} de ${scope}${period}.`;
  if (resumen.reglasEvaluadas > 0) {
    first += ` Se evaluaron ${plural(resumen.reglasEvaluadas, 'regla', 'reglas')} de las baselines aplicadas y se cumplieron ${resumen.reglasCumplidas}, lo que representa un cumplimiento del ${resumen.cumplimiento ?? 0} %.`;
  }
  paragraphs.push(first);

  if (resumen.hallazgos === 0) {
    paragraphs.push('No se registraron hallazgos para los criterios seleccionados: las configuraciones auditadas cumplen con las reglas evaluadas.');
    return paragraphs;
  }

  const bySeverity = SEVERITIES
    .map((severity) => ({ severity, total: resumen.hallazgosPorSeveridad[severity] ?? 0 }))
    .filter((item) => item.total > 0)
    .map((item) => `${item.total} de severidad ${SEVERITY_LABEL[item.severity].toLowerCase()}`);
  const pendingCritical = hallazgos.filter((finding) => PENDING.includes(finding.estado)
    && (finding.severidad === 'CRITICA' || finding.severidad === 'ALTA')).length;

  let second = `Se identificaron ${plural(resumen.hallazgos, 'hallazgo', 'hallazgos')} (${joinList(bySeverity)}).`;
  second += resumen.hallazgosPendientes === 0
    ? ' Al momento de la emisión, todos se encuentran resueltos o con riesgo aceptado.'
    : ` Al momento de la emisión, ${resumen.hallazgosPendientes === 1 ? 'uno permanece pendiente' : `${resumen.hallazgosPendientes} permanecen pendientes`} de resolución.`;
  paragraphs.push(second);

  if (pendingCritical > 0) {
    paragraphs.push(`Se recomienda priorizar la corrección ${pendingCritical === 1 ? 'del hallazgo pendiente' : `de los ${pendingCritical} hallazgos pendientes`} de severidad crítica o alta, siguiendo las recomendaciones detalladas en el apartado 5.`);
  } else if (resumen.hallazgosPendientes > 0) {
    paragraphs.push('Los hallazgos pendientes son de severidad media o baja y pueden planificarse dentro del ciclo habitual de cambios.');
  }
  return paragraphs;
}

export function reportDocumentHtml(report: AuditReport, context: ReportContext): string {
  const { resumen, auditorias, hallazgos, inconsistencias } = report.contenido;
  const criteria = report.criterios;
  const rep = code('REP', report.id);
  const organization = criteria.organizacionId == null ? 'Todas las organizaciones' : context.orgName(criteria.organizacionId);
  const devices = criteria.dispositivoIds.length
    ? criteria.dispositivoIds.map(context.deviceName).join(', ')
    : `Todos (${plural(resumen.dispositivos, 'dispositivo auditado', 'dispositivos auditados')})`;
  const withDeviations = resumen.resultadoGeneral === 'CON_DESVIOS';
  const maxCount = Math.max(1, ...SEVERITIES.map((severity) => resumen.hallazgosPorSeveridad[severity] ?? 0));
  const percent = (value: number) => (resumen.hallazgos === 0 ? '0 %' : `${Math.round((value * 100) / resumen.hallazgos)} %`);

  const severityRows = SEVERITIES.map((severity) => {
    const total = resumen.hallazgosPorSeveridad[severity] ?? 0;
    const pending = hallazgos.filter((finding) => finding.severidad === severity && PENDING.includes(finding.estado)).length;
    return `
      <tr>
        <td><span class="rpt-sev ${sevClass(severity)}">${SEVERITY_LABEL[severity]}</span></td>
        <td class="rpt-num">${total}</td>
        <td class="rpt-num">${pending}</td>
        <td class="rpt-num">${percent(total)}</td>
        <td class="rpt-bar-cell"><span class="rpt-bar ${sevClass(severity)}" style="width:${Math.round((total * 100) / maxCount)}%"></span></td>
      </tr>`;
  }).join('');

  const statusRows = Object.entries(resumen.hallazgosPorEstado).map(([status, total]) => `
      <tr><td>${escapeHtml(STATUS_LABEL[status] ?? status)}</td><td class="rpt-num">${total}</td><td class="rpt-num">${percent(total)}</td></tr>`).join('');

  const auditRows = auditorias.map((audit) => `
      <tr>
        <td><a href="/history?auditoria=${audit.id}">${code('AUD', audit.id)}</a></td>
        <td class="rpt-nowrap">${escapeHtml(dateTime(audit.fechaEjecucion))}</td>
        <td><strong class="rpt-nowrap">${escapeHtml(audit.dispositivoIdentificador ?? `#${audit.dispositivoId}`)}</strong><small>${escapeHtml([audit.criticidadDispositivo ? `criticidad ${audit.criticidadDispositivo.toLowerCase()}` : null, context.siteName(audit.sedeId)].filter(Boolean).join(' · '))}</small></td>
        <td>${escapeHtml(audit.baselineNombre ?? `#${audit.baselineId}`)}${audit.baselineVersion ? `<small>versión ${audit.baselineVersion}</small>` : ''}</td>
        <td class="rpt-nowrap">${code('CFG', audit.configuracionId)}${audit.configuracionVersion ? `<small>versión ${audit.configuracionVersion}</small>` : ''}</td>
        <td class="rpt-num">${audit.reglasCumplidas}/${audit.totalReglas}</td>
        <td class="rpt-num">${audit.totalHallazgos}</td>
        <td>${escapeHtml(RESULT_LABEL[audit.resultado] ?? audit.resultado)}</td>
      </tr>`).join('');

  const findingBlocks = hallazgos.map((finding, index) => `
      <article class="rpt-finding">
        <header>
          <span class="rpt-finding-index">5.${index + 1}</span>
          <div>
            <strong>${escapeHtml(finding.nombreRegla)}</strong>
            <small>${escapeHtml(finding.codigoRegla)} · ${code('FND', finding.id)} · ${escapeHtml(finding.dispositivoIdentificador ?? `#${finding.dispositivoId}`)} · ${code('AUD', finding.auditoriaId)} · detectado el ${escapeHtml(dateTime(finding.fechaDeteccion))}</small>
          </div>
          <span class="rpt-sev ${sevClass(finding.severidad)}">${SEVERITY_LABEL[finding.severidad] ?? escapeHtml(finding.severidad)}</span>
        </header>
        <dl>
          <dt>Estado</dt><dd>${escapeHtml(STATUS_LABEL[finding.estado] ?? finding.estado)}</dd>
          ${finding.impacto ? `<dt>Impacto</dt><dd>${escapeHtml(finding.impacto)}</dd>` : ''}
          <dt>Evidencia</dt><dd><pre>${escapeHtml(finding.evidencia)}</pre></dd>
          <dt>Recomendación</dt><dd>${escapeHtml(finding.recomendacion)}</dd>
        </dl>
      </article>`).join('');

  const observations = [
    `El reporte es una foto fija emitida el ${dateTime(report.fechaGeneracion)}: refleja el estado de los hallazgos en ese momento y no se modifica aunque cambien los datos de origen.`,
    'El cumplimiento se calcula como reglas cumplidas sobre reglas evaluadas en cada auditoría incluida.',
    ...inconsistencias,
  ];

  return `
    <div class="rpt">
      <header class="rpt-masthead">
        <div class="rpt-brand">
          <span class="rpt-logo">A_</span>
          <div><strong>AuditNet</strong><small>Auditoría de configuraciones de red</small></div>
        </div>
        <div class="rpt-docid">
          <span>Reporte de auditoría</span>
          <strong>${rep}</strong>
        </div>
      </header>

      <h1 class="rpt-title">${escapeHtml(report.titulo)}</h1>

      <table class="rpt-meta">
        <tr><th>Organización</th><td>${escapeHtml(organization)}</td><th>Período</th><td>${escapeHtml(reportPeriod(report))}</td></tr>
        <tr><th>Emitido por</th><td>${escapeHtml(report.generadoPor)}</td><th>Fecha de emisión</th><td>${escapeHtml(dateTime(report.fechaGeneracion))}</td></tr>
        <tr><th>Resultado general</th><td><span class="rpt-result ${withDeviations ? 'is-deviation' : 'is-ok'}">${withDeviations ? 'Con desvíos' : 'Sin desvíos'}</span></td><th>Severidad máxima</th><td>${escapeHtml(SEVERITY_LABEL[resumen.severidadMaxima] ?? resumen.severidadMaxima)}</td></tr>
      </table>
      ${inconsistencias.length ? `<p class="rpt-warning"><strong>Datos incompletos.</strong> ${inconsistencias.map(escapeHtml).join(' ')} El detalle figura en el apartado 6.</p>` : ''}

      <section class="rpt-section">
        <h2><span>1</span>Resumen ejecutivo</h2>
        ${executiveSummary(report, context).map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join('')}
        <div class="rpt-figures">
          <div><strong>${resumen.cumplimiento == null ? '—' : `${resumen.cumplimiento} %`}</strong><span>Cumplimiento</span><small>${resumen.reglasCumplidas} de ${resumen.reglasEvaluadas} reglas</small></div>
          <div><strong>${resumen.auditorias}</strong><span>Auditorías</span><small>${plural(resumen.dispositivos, 'dispositivo', 'dispositivos')}</small></div>
          <div><strong>${resumen.hallazgos}</strong><span>Hallazgos</span><small>${plural(resumen.hallazgosPendientes, 'pendiente', 'pendientes')}</small></div>
          <div><strong class="${resumen.severidadMaxima === 'CRITICA' || resumen.severidadMaxima === 'ALTA' ? 'is-alert' : ''}">${escapeHtml(SEVERITY_LABEL[resumen.severidadMaxima] ?? resumen.severidadMaxima)}</strong><span>Severidad máxima</span><small>entre los hallazgos incluidos</small></div>
        </div>
      </section>

      <section class="rpt-section">
        <h2><span>2</span>Alcance y criterios</h2>
        <table class="rpt-kv">
          <tr><th>Organización</th><td>${escapeHtml(organization)}</td></tr>
          <tr><th>Período</th><td>${escapeHtml(reportPeriod(report))}</td></tr>
          <tr><th>Auditorías</th><td>${criteria.auditoriaIds.length ? criteria.auditoriaIds.map((id) => code('AUD', id)).join(', ') : 'Todas las del período'}</td></tr>
          <tr><th>Dispositivos</th><td>${escapeHtml(devices)}</td></tr>
          <tr><th>Severidades</th><td>${criteria.severidades.length ? criteria.severidades.map((s) => SEVERITY_LABEL[s] ?? s).join(', ') : 'Todas'}</td></tr>
          <tr><th>Estados de hallazgo</th><td>${criteria.estados.length ? criteria.estados.map((s) => STATUS_LABEL[s] ?? s).join(', ') : 'Todos'}</td></tr>
        </table>
      </section>

      <section class="rpt-section">
        <h2><span>3</span>Distribución de hallazgos</h2>
        <div class="rpt-two">
          <table class="rpt-table">
            <thead><tr><th>Severidad</th><th class="rpt-num">Total</th><th class="rpt-num">Pendientes</th><th class="rpt-num">%</th><th></th></tr></thead>
            <tbody>${severityRows}</tbody>
          </table>
          <table class="rpt-table">
            <thead><tr><th>Estado</th><th class="rpt-num">Total</th><th class="rpt-num">%</th></tr></thead>
            <tbody>${statusRows}</tbody>
          </table>
        </div>
      </section>

      <section class="rpt-section">
        <h2><span>4</span>Auditorías incluidas</h2>
        <table class="rpt-table rpt-audits">
          <thead><tr><th>Auditoría</th><th>Fecha</th><th>Dispositivo</th><th>Baseline</th><th>Configuración</th><th class="rpt-num">Reglas</th><th class="rpt-num">Hallazgos</th><th>Resultado</th></tr></thead>
          <tbody>${auditRows}</tbody>
        </table>
      </section>

      <section class="rpt-section">
        <h2><span>5</span>Detalle de hallazgos</h2>
        ${hallazgos.length === 0 ? '<p>No hay hallazgos que cumplan los criterios del reporte.</p>' : findingBlocks}
      </section>

      <section class="rpt-section">
        <h2><span>6</span>Observaciones</h2>
        <ul class="rpt-notes">${observations.map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul>
      </section>

      <footer class="rpt-footer">
        <span>AuditNet · ${rep}</span>
        <span>Documento generado a partir de los registros de auditoría de la plataforma.</span>
      </footer>
    </div>`;
}

/** Pie de página del PDF (Chrome/Edge): "AuditNet · REP-001" y "Página X de Y". */
export function printPageStyle(report: AuditReport): string {
  const rep = code('REP', report.id);
  return `@media print { @page {
    @bottom-left { content: "AuditNet · ${rep}"; font: 8pt Inter, system-ui, sans-serif; color: #697078; }
    @bottom-right { content: "Página " counter(page) " de " counter(pages); font: 8pt Inter, system-ui, sans-serif; color: #697078; }
  } }`;
}
