// Panel de administración: salud de los servicios, correo, revisión periódica, archivos, respaldos y seguridad.
import { auditFetch, authFetch } from '../lib/auth';
import { readApiError, type MailDispatchResult, type MailOutbox, type PlanRequest, type ReviewRunResult, type SecuritySummary, type ServiceHealth, type StorageStatus, type StorageSyncResult, type StoredFile } from '../lib/api';
import { formatBytes, saveResponse } from '../lib/download';
import { escapeHtml, formatDate, plural } from '../lib/format';
import { etiqueta, localeActual, t } from '../lib/i18n';
import { show } from '../lib/ui';

const LABELS: Record<string, string> = {
  management: 'management-service',
  core: 'audit-core-service',
};
const EVENT_ORDER = [
  'LOGIN_EXITOSO', 'LOGIN_RECHAZADO', 'CUENTA_BLOQUEADA', 'CUENTA_DESBLOQUEADA', 'CONTRASENA_CAMBIADA',
  'CONTRASENA_RESTABLECIDA', 'USUARIO_CREADO', 'USUARIO_MODIFICADO', 'USUARIO_DESHABILITADO', 'USUARIO_HABILITADO',
  'ROLES_ASIGNADOS', 'ROL_MODIFICADO', 'RECUPERACION_SOLICITADA', 'CONTRASENA_RECUPERADA',
];
const ALERT_EVENTS = new Set(['LOGIN_RECHAZADO', 'CUENTA_BLOQUEADA']);

const showError = (message: string | null) => show('#admin-error', message);

const uptime = (seconds: number) => {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) return `${days} d ${hours} h`;
  if (hours > 0) return `${hours} h ${minutes} min`;
  return `${minutes} min`;
};

const healthHtml = (key: string, health: ServiceHealth | null, error: string | null) => {
  const name = LABELS[key];
  if (!health) {
    return `
      <div class="panel-heading">
        <div><span class="eyebrow">${t('admin.servicio')}</span><h2>${name}</h2></div>
        <span class="status-badge status-locked">${t('admin.noResponde')}</span>
      </div>
      <p class="log-muted">${escapeHtml(error ?? t('admin.sinRespuestaServicio'))}</p>`;
  }
  const ok = health.estado === 'UP';
  return `
    <div class="panel-heading">
      <div><span class="eyebrow">${t('admin.servicio')}</span><h2>${escapeHtml(health.servicio)}</h2></div>
      <span class="status-badge ${ok ? 'status-ready' : 'status-next'}">${t(ok ? 'admin.operativo' : 'admin.degradado')}</span>
    </div>
    <dl class="health-meta">
      <div><dt>${t('admin.version')}</dt><dd>${escapeHtml(health.version)}</dd></div>
      <div><dt>${t('consola.enLinea')}</dt><dd title="${escapeHtml(t('organizaciones.planDesde', { fecha: formatDate(health.inicio) }))}">${uptime(health.segundosEnLinea)}</dd></div>
      <div><dt>${t('admin.memoria')}</dt><dd>${health.memoriaUsadaMb} / ${health.memoriaMaximaMb} MB</dd></div>
    </dl>
    <div class="health-components">
      ${health.componentes.map((component) => `
        <div class="health-component">
          <span class="health-dot ${component.estado === 'UP' ? '' : 'is-down'}"></span>
          <div><strong>${escapeHtml(component.nombre)}</strong><small>${escapeHtml(component.detalle)}</small></div>
          <code>${component.estado === 'UP' ? `${component.latenciaMs} ms` : t('admin.caido')}</code>
        </div>`).join('')}
    </div>`;
};

const fetchHealth = async (key: 'management' | 'core') => {
  try {
    const response = key === 'management' ? await authFetch('/api/admin/health') : await auditFetch('/api/system/health');
    if (!response.ok) return { key, health: null, error: await readApiError(response) };
    return { key, health: await response.json() as ServiceHealth, error: null };
  } catch (error) {
    return { key, health: null, error: error instanceof Error ? error.message : t('admin.sinRespuesta') };
  }
};

const loadHealth = async () => {
  const results = await Promise.all([fetchHealth('management'), fetchHealth('core')]);
  results.forEach(({ key, health, error }) => {
    const node = document.querySelector<HTMLElement>(`[data-service="${key}"]`);
    if (node) node.innerHTML = healthHtml(key, health, error);
  });
  const down = results.filter((result) => !result.health).length;
  const degraded = results.filter((result) => result.health && result.health.estado !== 'UP').length;
  const overall = document.querySelector('#admin-overall');
  if (overall) {
    overall.textContent = down > 0
      ? plural(down, 'admin.serviciosSinRespuesta')
      : t(degraded > 0 ? 'admin.componentesDegradados' : 'admin.todosOperativos');
  }
  const checked = document.querySelector('#admin-checked');
  if (checked) {
    checked.textContent = t('admin.verificado', { hora: new Intl.DateTimeFormat(localeActual(), { timeStyle: 'medium' }).format(new Date()) });
  }
};

const loadSecurity = async () => {
  const response = await authFetch('/api/admin/security');
  if (!response.ok) throw new Error(await readApiError(response));
  const summary = await response.json() as SecuritySummary;
  const events = summary.eventosUltimas24h;
  const kpis = document.querySelector('#admin-security-kpis');
  if (kpis) {
    kpis.innerHTML = `
      <a class="metric-card metric-link" href="/users"><span class="metric-label">${t('admin.usuariosActivos')}</span><strong>${summary.usuariosActivos}</strong><small>${t('admin.deshabilitados', { n: summary.usuariosDeshabilitados })}</small></a>
      <a class="metric-card metric-link ${summary.cuentasBloqueadas > 0 ? 'is-alert' : ''}" href="/users"><span class="metric-label">${t('admin.cuentasBloqueadas')}</span><strong>${summary.cuentasBloqueadas}</strong><small>${t('admin.porIntentos')}</small></a>
      <a class="metric-card metric-link" href="${logLink('evento=LOGIN_EXITOSO')}"><span class="metric-label">${t('admin.ingresos24')}</span><strong>${events.LOGIN_EXITOSO ?? 0}</strong><small>${t('admin.iniciosExitosos')}</small></a>
      <a class="metric-card metric-link ${(events.LOGIN_RECHAZADO ?? 0) > 0 ? 'is-alert' : ''}" href="${logLink('evento=LOGIN_RECHAZADO')}"><span class="metric-label">${t('admin.rechazos24')}</span><strong>${events.LOGIN_RECHAZADO ?? 0}</strong><small>${t('admin.bloqueosCuenta', { n: events.CUENTA_BLOQUEADA ?? 0 })}</small></a>`;
  }
  const eventsNode = document.querySelector('#admin-events');
  if (eventsNode) {
    eventsNode.innerHTML = EVENT_ORDER.map((event) => {
      const total = events[event] ?? 0;
      return `<a class="event-count ${ALERT_EVENTS.has(event) && total > 0 ? 'is-alert' : ''}" href="${logLink(`evento=${event}`)}"><span>${event}</span><strong>${total}</strong></a>`;
    }).join('');
  }
  const body = document.querySelector('#admin-rejections');
  if (body) {
    body.innerHTML = summary.ultimosRechazos.map((record) => `
      <tr>
        <td><span class="log-nowrap">${escapeHtml(formatDate(record.fecha))}</span></td>
        <td><strong>${escapeHtml(record.evento)}</strong></td>
        <td>${escapeHtml(record.actor ?? '—')}<small>${escapeHtml(record.origenIp ?? '')}</small></td>
        <td><small title="${escapeHtml(record.detalle ?? '')}">${escapeHtml(record.detalle ?? '')}</small></td>
      </tr>`).join('');
  }
  document.querySelector('#admin-rejections-empty')?.classList.toggle('is-hidden', summary.ultimosRechazos.length > 0);
};

/** Hace 24 h en el formato del filtro de Bitácoras, para que el listado coincida con el número. */
const last24h = () => {
  const date = new Date(Date.now() - 24 * 3600 * 1000);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};
const logLink = (params: string) => `/logs?${params}&desde=${encodeURIComponent(last24h())}#sistema`;

// Repositorio de archivos y respaldos

const loadStorage = async () => {
  const response = await auditFetch('/api/system/storage');
  if (!response.ok) throw new Error(await readApiError(response));
  const storage = await response.json() as StorageStatus;
  const state = document.querySelector<HTMLElement>('#storage-state');
  if (state) {
    state.textContent = t(storage.configurado ? 'admin.configurado' : 'admin.noConfigurado');
    state.className = `status-badge ${storage.configurado ? 'status-ready' : 'status-locked'}`;
  }
  const summary = document.querySelector('#storage-summary');
  if (summary) {
    summary.textContent = storage.configurado
      ? t('admin.contenedor', { contenedor: storage.contenedor })
      : t('admin.faltaAlmacenamiento');
  }
  const totals = document.querySelector('#storage-totals');
  if (totals) {
    totals.innerHTML = Object.entries(storage.archivos).map(([tipo, total]) => `
      <div><span>${escapeHtml(etiqueta('archivo', tipo))}</span><strong>${total.cantidad}</strong><small>${formatBytes(total.bytes)}</small></div>`).join('');
  }
  const pending = storage.configuracionesPendientes + storage.reportesPendientes;
  const pendingNode = document.querySelector('#storage-pending');
  if (pendingNode) {
    pendingNode.textContent = pending === 0
      ? t('admin.sinPendientesSubir')
      : t('admin.pendientesSubir', { configuraciones: storage.configuracionesPendientes, reportes: storage.reportesPendientes });
  }
  const sync = document.querySelector<HTMLButtonElement>('#storage-sync');
  if (sync) sync.disabled = pending === 0 || !storage.configurado;
  const create = document.querySelector<HTMLButtonElement>('#backup-create');
  if (create) create.disabled = !storage.configurado;
};

const loadBackups = async () => {
  const response = await auditFetch('/api/system/backups');
  if (!response.ok) throw new Error(await readApiError(response));
  const backups = await response.json() as StoredFile[];
  const body = document.querySelector('#backup-list');
  if (body) {
    body.innerHTML = backups.map((backup) => `
      <tr>
        <td class="backup-cell">
          <strong>${escapeHtml(formatDate(backup.fecha))} · ${escapeHtml(backup.usuario)}</strong>
          <small>${escapeHtml(backup.nombre)}</small>
          <small title="SHA-256 ${escapeHtml(backup.sha256)}">${escapeHtml(backup.detalle ?? '')}</small>
        </td>
        <td class="log-nowrap">${formatBytes(backup.tamanioBytes)}</td>
        <td><button class="button button-ghost button-small" type="button" data-backup-id="${backup.id}">${t('admin.descargar')}</button></td>
      </tr>`).join('');
  }
  document.querySelector('#backup-empty')?.classList.toggle('is-hidden', backups.length > 0);
};

const confirmDialog = document.querySelector<HTMLDialogElement>('#admin-confirm');
const confirmBackup = () => new Promise<boolean>((resolve) => {
  if (!confirmDialog) return resolve(false);
  confirmDialog.returnValue = 'cancel';
  confirmDialog.addEventListener('close', () => resolve(confirmDialog.returnValue === 'ok'), { once: true });
  confirmDialog.showModal();
});

const busy = async (button: HTMLButtonElement | null, label: string, action: () => Promise<void>) => {
  if (!button) return;
  const original = button.textContent;
  button.disabled = true;
  button.textContent = label;
  try {
    await action();
  } catch (error) {
    showError(error instanceof Error ? error.message : t('admin.operacionFallida'));
  } finally {
    button.textContent = original;
    button.disabled = false;
  }
};

document.querySelector('#backup-create')?.addEventListener('click', async (event) => {
  const button = event.currentTarget as HTMLButtonElement;
  if (!await confirmBackup()) return;
  await busy(button, t('admin.generando'), async () => {
    showError(null);
    const response = await auditFetch('/api/system/backups', { method: 'POST' });
    if (!response.ok) throw new Error(await readApiError(response));
    await Promise.all([loadBackups(), loadStorage()]);
  });
});

document.querySelector('#storage-sync')?.addEventListener('click', async (event) => {
  await busy(event.currentTarget as HTMLButtonElement, t('admin.subiendo'), async () => {
    showError(null);
    const response = await auditFetch('/api/system/storage/sync', { method: 'POST' });
    if (!response.ok) throw new Error(await readApiError(response));
    const result = await response.json() as StorageSyncResult;
    await loadStorage();
    const pendingNode = document.querySelector('#storage-pending');
    if (pendingNode) {
      pendingNode.textContent = t('admin.subidos', { configuraciones: result.configuraciones, reportes: result.reportes })
        + (result.pendientes > 0 ? ` ${t('admin.quedanSubir', { n: result.pendientes })}` : '');
    }
  });
});

document.querySelector('#backup-list')?.addEventListener('click', async (event) => {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-backup-id]');
  if (!button) return;
  await busy(button, t('admin.verificando'), async () => {
    const response = await auditFetch(`/api/system/backups/${button.dataset.backupId}/download`);
    if (!response.ok) throw new Error(await readApiError(response));
    await saveResponse(response, 'auditnet-respaldo.zip');
  });
});

// Correo y revisión periódica
const MAIL_STATUS: Record<string, string> = { PENDIENTE: 'status-next', ENVIADO: 'status-ready', FALLIDO: 'status-locked' };

const loadMail = async () => {
  const response = await authFetch('/api/admin/mail');
  if (!response.ok) throw new Error(await readApiError(response));
  const outbox = await response.json() as MailOutbox;
  const provider = document.querySelector<HTMLElement>('#mail-provider');
  if (provider) {
    provider.textContent = outbox.proveedor ?? t('admin.sinProveedor');
    provider.className = `status-badge ${outbox.proveedor ? 'status-ready' : 'status-locked'}`;
  }
  const totals = document.querySelector('#mail-totals');
  if (totals) {
    totals.innerHTML = `
      <div><span>${t('admin.pendientes')}</span><strong>${outbox.pendientes}</strong><small>${t('admin.esperandoEnvio')}</small></div>
      <div><span>${t('admin.enviados')}</span><strong>${outbox.enviados24h}</strong><small>${t('admin.ultimas24')}</small></div>
      <div><span>${t('admin.fallidos')}</span><strong>${outbox.fallidos24h}</strong><small>${t('admin.ultimas24')}</small></div>`;
  }
  if (!outbox.proveedor) {
    const note = document.querySelector('#mail-note');
    if (note) note.textContent = t('admin.envioNoConfigurado');
  }
  const dispatch = document.querySelector<HTMLButtonElement>('#mail-dispatch');
  if (dispatch) dispatch.disabled = !outbox.proveedor || outbox.pendientes === 0;
  const body = document.querySelector('#mail-list');
  if (body) {
    body.innerHTML = outbox.ultimos.map((mail) => `
      <tr>
        <td><span class="log-nowrap">${escapeHtml(formatDate(mail.creadoEn))}</span></td>
        <td><strong>${escapeHtml(etiqueta('correo', mail.tipo))}</strong><small>${escapeHtml(mail.origen)}</small></td>
        <td><small>${escapeHtml(mail.destinatario)}</small></td>
        <td>
          <span class="status-badge ${MAIL_STATUS[mail.estado] ?? 'status-next'}">${escapeHtml(etiqueta('estadoCorreo', mail.estado))}</span>
          ${mail.ultimoError ? `<small title="${escapeHtml(mail.ultimoError)}">${escapeHtml(plural(mail.intentos, 'admin.intentos'))} · ${escapeHtml(mail.ultimoError)}</small>` : ''}
        </td>
      </tr>`).join('');
  }
  document.querySelector('#mail-empty')?.classList.toggle('is-hidden', outbox.ultimos.length > 0);
};

document.querySelector('#mail-dispatch')?.addEventListener('click', async (event) => {
  await busy(event.currentTarget as HTMLButtonElement, t('admin.enviando'), async () => {
    showError(null);
    const response = await authFetch('/api/admin/mail/dispatch', { method: 'POST' });
    if (!response.ok) throw new Error(await readApiError(response));
    const result = await response.json() as MailDispatchResult;
    await loadMail();
    const note = document.querySelector('#mail-note');
    if (note) {
      note.textContent = t('admin.resultadoEnvio', { enviados: result.enviados, reintentos: result.reintentos, fallidos: result.fallidos })
        + (result.pendientes > 0 ? ` ${t('admin.quedanPendientes', { n: result.pendientes })}` : '');
    }
  });
});

document.querySelector('#review-run')?.addEventListener('click', async (event) => {
  await busy(event.currentTarget as HTMLButtonElement, t('admin.revisando'), async () => {
    showError(null);
    const response = await auditFetch('/api/review/run', { method: 'POST' });
    if (!response.ok) throw new Error(await readApiError(response));
    const result = await response.json() as ReviewRunResult;
    const node = document.querySelector<HTMLElement>('#review-result');
    if (node) {
      node.innerHTML = `
        <dl class="health-meta">
          <div><dt>${t('tablero.organizaciones')}</dt><dd>${result.organizaciones}</dd></div>
          <div><dt>${t('admin.reauditorias')}</dt><dd>${result.reauditorias}</dd></div>
          <div><dt>${t('tablero.vencidos')}</dt><dd>${result.hallazgosVencidos}</dd></div>
          <div><dt>${t('admin.desactualizadas')}</dt><dd>${result.dispositivosDesactualizados}</dd></div>
          <div><dt>${t('admin.correosEnviados')}</dt><dd>${result.correos}</dd></div>
          <div><dt>${t('admin.resumenes')}</dt><dd>${result.resumenes}</dd></div>
        </dl>
        ${result.omitidas.length ? `<p class="log-muted">${t('admin.noSePudo')}</p><ul class="review-skipped">${result.omitidas
          .map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul>` : ''}
        <p class="technical-hint">${escapeHtml(t('admin.ejecutada', { fecha: formatDate(result.ejecutada) }))}</p>`;
      node.classList.remove('is-hidden');
    }
    await loadMail();
  });
});

const loadPlanRequests = async () => {
  const response = await authFetch('/api/admin/plan-requests?estado=PENDIENTE');
  const title = document.querySelector('#admin-plan-requests');
  if (!title) return;
  if (!response.ok) {
    title.textContent = t('admin.errorSolicitudes');
    return;
  }
  const pending = (await response.json() as PlanRequest[]).length;
  title.textContent = pending === 0 ? t('admin.sinSolicitudes') : plural(pending, 'admin.solicitudesPorResolver');
};

const refresh = async () => {
  showError(null);
  const button = document.querySelector<HTMLButtonElement>('#admin-refresh');
  if (button) button.disabled = true;
  try {
    await Promise.all([loadHealth(), loadSecurity(), loadStorage(), loadBackups(), loadPlanRequests(), loadMail()]);
  } catch (error) {
    showError(error instanceof Error ? error.message : t('admin.errorEstado'));
  } finally {
    if (button) button.disabled = false;
  }
};

document.querySelector('#admin-refresh')?.addEventListener('click', () => void refresh());
// Se actualiza solo cada minuto mientras la pestaña está visible.
window.setInterval(() => { if (document.visibilityState === 'visible') void loadHealth(); }, 60_000);
void refresh();
