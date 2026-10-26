// Usuarios y perfiles: altas, cambios, contraseñas, desbloqueo y descripción de los perfiles.
import { authFetch, getStoredUser, hasRole, logout } from '../lib/auth';
import {
  jsonRequest, OPERATION_ROLES, PROFILE_ORDER, readApiError, ROLE_LABEL,
  type Organization, type Permission, type Role, type User,
} from '../lib/api';
import { escapeHtml, formatDate } from '../lib/format';
import { etiqueta, localeActual, t } from '../lib/i18n';
import { passwordRulesHtml, updatePasswordRules } from '../lib/password';
import { confirmAction, notify, show } from '../lib/ui';

const me = getStoredUser();
// El administrador de una organización gestiona solo perfiles de operación de la suya.
const isSystemAdmin = hasRole('ADMINISTRADOR_SISTEMA');
const canManage = (user: User) => isSystemAdmin || user.roles.every((role) => OPERATION_ROLES.includes(role));

let users: User[] = [];
let organizations: Organization[] = [];
let roles: Role[] = [];
let editing: User | null = null;
let resetting: User | null = null;
let editingRole: Role | null = null;

const $ = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector);
const userDialog = $<HTMLDialogElement>('#user-dialog');
const passwordDialog = $<HTMLDialogElement>('#password-dialog');
const roleDialog = $<HTMLDialogElement>('#role-dialog');

const value = (selector: string) => ($<HTMLInputElement>(selector)?.value ?? '').trim();
const orgName = (id: number) => organizations.find((org) => org.id === id)?.razonSocial ?? t('tablero.organizacionNumero', { id });
const isLocked = (user: User) => Boolean(user.bloqueadoHasta && new Date(user.bloqueadoHasta).getTime() > Date.now());
const timeOnly = (iso: string) => new Intl.DateTimeFormat(localeActual(), { hour: '2-digit', minute: '2-digit' }).format(new Date(iso));

const statusBadge = (user: User) => {
  if (user.estado !== 'ACTIVO') return `<span class="status-badge status-off">${t('usuarios.deshabilitado')}</span>`;
  if (isLocked(user)) {
    return `<span class="status-badge status-locked" title="${t('usuarios.bloqueadaIntentos')}">${escapeHtml(t('usuarios.bloqueadoHasta', { hora: timeOnly(user.bloqueadoHasta!) }))}</span>`;
  }
  return `<span class="status-badge status-ready">${t('usuarios.activo')}</span>`;
};

const renderUsers = () => {
  const body = $<HTMLTableSectionElement>('#users-body');
  if (!body) return;
  const term = value('#user-search').toLowerCase();
  const status = $<HTMLSelectElement>('#user-status-filter')?.value ?? '';
  const visible = users.filter((user) => {
    if (term && ![user.nombre, user.nombreUsuario, user.email].some((field) => field.toLowerCase().includes(term))) return false;
    if (status === 'BLOQUEADO') return isLocked(user);
    return !status || user.estado === status;
  });

  body.innerHTML = visible.map((user) => {
    const self = user.id === me?.userId;
    const active = user.estado === 'ACTIVO';
    const ownPassword = `<a class="button button-ghost button-small" href="/account" title="${t('usuarios.contrasenaPropiaAyuda')}">${t('usuarios.contrasena')}</a>`;
    const actions = canManage(user) ? `
            <button class="button button-ghost button-small" type="button" data-action="edit">${t('general.editar')}</button>
            ${self
              ? ownPassword
              : `<button class="button button-ghost button-small" type="button" data-action="password">${t('usuarios.contrasena')}</button>`}
            ${isLocked(user) ? `<button class="button button-ghost button-small" type="button" data-action="unlock">${t('usuarios.desbloquear')}</button>` : ''}
            ${self ? '' : `<button class="button button-ghost button-small" type="button" data-action="${active ? 'disable' : 'enable'}">${t(active ? 'organizaciones.deshabilitar' : 'organizaciones.habilitar')}</button>`}`
      : self
        ? ownPassword
        : `<span class="log-muted">${t('usuarios.loGestionaAdministrador')}</span>`;
    return `
      <tr class="user-row ${active ? '' : 'is-disabled'}" data-user-id="${user.id}">
        <td><strong>${escapeHtml(user.nombre)}${self ? ` <span class="role-chip">${t('usuarios.vos')}</span>` : ''}</strong><small>@${escapeHtml(user.nombreUsuario)} · ${escapeHtml(user.email)}</small></td>
        <td>${escapeHtml(orgName(user.organizationId))}</td>
        <td>${user.roles.map((role) => `<span class="role-chip" title="${escapeHtml(role)}">${escapeHtml(ROLE_LABEL[role] ?? role)}</span>`).join('')}</td>
        <td>${statusBadge(user)}</td>
        <td><span class="log-nowrap">${user.ultimoAcceso ? escapeHtml(formatDate(user.ultimoAcceso)) : `<span class="log-muted">${t('usuarios.nuncaIngreso')}</span>`}</span></td>
        <td><div class="row-actions">${actions}</div></td>
      </tr>`;
  }).join('');

  const count = $('#users-count');
  if (count) count.textContent = String(users.length);
  $('#users-empty')?.classList.toggle('is-hidden', visible.length > 0);
};

const renderRoles = () => {
  const list = $('#role-list');
  if (!list) return;
  const ordered = [...roles].sort((a, b) => PROFILE_ORDER.indexOf(a.nombre) - PROFILE_ORDER.indexOf(b.nombre));
  list.innerHTML = ordered.map((role) => `
    <div class="role-item">
      <div>
        <strong>${escapeHtml(ROLE_LABEL[role.nombre] ?? role.nombre)}</strong>
        <code class="log-muted"> ${escapeHtml(role.nombre)}</code>
        <p>${escapeHtml(role.descripcion)}</p>
      </div>
      ${isSystemAdmin ? `<button class="button button-ghost button-small" type="button" data-role-id="${role.id}">${t('general.editar')}</button>` : ''}
    </div>`).join('');

  const checks = $('#user-roles');
  if (checks) {
    const assignable = ordered.filter((role) => isSystemAdmin || OPERATION_ROLES.includes(role.nombre));
    checks.innerHTML = `<legend>${t('usuarios.perfilObligatorio')}</legend>${assignable.map((role) => `
      <label class="role-check">
        <input type="checkbox" name="roles" value="${escapeHtml(role.nombre)}" />
        <strong>${escapeHtml(ROLE_LABEL[role.nombre] ?? role.nombre)}</strong>
        <small>${escapeHtml(role.descripcion)}</small>
      </label>`).join('')}`;
  }
};

const renderPermissions = (permissions: Permission[]) => {
  const body = $('#permission-body');
  if (!body) return;
  body.innerHTML = permissions.map((permission) => `
    <tr>
      <td><strong>${escapeHtml(etiqueta('permiso', permission.modulo))}</strong><small>${escapeHtml(etiqueta('permiso', permission.accion))}</small></td>
      ${PROFILE_ORDER.map((profile) => permission.perfiles.includes(profile)
        ? `<td><span class="perm-yes" aria-label="${t('usuarios.permitido')}">✓</span></td>`
        : `<td><span class="perm-no" aria-label="${t('usuarios.noPermitido')}">—</span></td>`).join('')}
    </tr>`).join('');
};

const loadUsers = async () => {
  const response = await authFetch('/api/users');
  if (!response.ok) {
    show('#users-error', await readApiError(response));
    $('#users-body')?.replaceChildren();
    return;
  }
  users = await response.json() as User[];
  renderUsers();
};

const loadAll = async () => {
  const [orgResponse, roleResponse, permissionResponse] = await Promise.all([
    authFetch('/api/client-organizations'),
    authFetch('/api/roles'),
    authFetch('/api/roles/permissions'),
  ]);
  if (orgResponse.ok) organizations = await orgResponse.json() as Organization[];
  if (roleResponse.ok) roles = await roleResponse.json() as Role[];
  if (permissionResponse.ok) renderPermissions(await permissionResponse.json() as Permission[]);
  renderRoles();
  const select = $<HTMLSelectElement>('#form-user-organization');
  if (select) {
    select.replaceChildren(...organizations.map((org) => {
      const option = new Option(`${org.razonSocial}${org.estado === 'ACTIVO' ? '' : ` ${t('dispositivos.deshabilitada')}`}`, String(org.id));
      return option;
    }));
    select.disabled = !isSystemAdmin;
  }
  $('#users-scope-note')?.classList.toggle('is-hidden', isSystemAdmin);
  await loadUsers();
};

// Alta y modificación
const syncPasswordRules = () => updatePasswordRules($('#form-user-password-rules'), $<HTMLInputElement>('#form-user-password')?.value ?? '', value('#form-user-username'));

const openUserDialog = (user: User | null) => {
  editing = user;
  const form = $<HTMLFormElement>('#user-form');
  form?.reset();
  show('#user-form-error', null);
  $('#user-dialog-eyebrow')!.textContent = t(user ? 'usuarios.modificarUsuario' : 'usuarios.altaUsuario');
  $('#user-dialog-title')!.textContent = user ? user.nombre : t('usuarios.registrarUsuario');
  $('#user-dialog-help')!.textContent = t(user ? 'usuarios.cambioCierraSesiones' : 'usuarios.cuentaQuedaActivaPerfil');
  $('#user-submit')!.textContent = t(user ? 'general.guardarCambios' : 'usuarios.registrarUsuario');
  const username = $<HTMLInputElement>('#form-user-username')!;
  username.disabled = Boolean(user);
  $('#form-user-password-field')?.classList.toggle('is-hidden', Boolean(user));
  $('#form-user-password-rules')?.classList.toggle('is-hidden', Boolean(user));
  $('#form-user-password-rules')!.innerHTML = passwordRulesHtml();
  if (user) {
    $<HTMLInputElement>('#form-user-name')!.value = user.nombre;
    $<HTMLInputElement>('#form-user-email')!.value = user.email;
    username.value = user.nombreUsuario;
    $<HTMLSelectElement>('#form-user-organization')!.value = String(user.organizationId);
    document.querySelectorAll<HTMLInputElement>('#user-roles input[name=roles]').forEach((check) => {
      check.checked = user.roles.includes(check.value);
    });
  }
  syncPasswordRules();
  userDialog?.showModal();
};

$('#user-create')?.addEventListener('click', () => openUserDialog(null));
$('#form-user-password')?.addEventListener('input', syncPasswordRules);
$('#form-user-username')?.addEventListener('input', syncPasswordRules);

$<HTMLFormElement>('#user-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  show('#user-form-error', null);
  const selectedRoles = Array.from(document.querySelectorAll<HTMLInputElement>('#user-roles input[name=roles]:checked')).map((check) => check.value);
  if (!value('#form-user-name') || !value('#form-user-email') || (!editing && !value('#form-user-username'))) {
    return show('#user-form-error', t('usuarios.completarDatos'));
  }
  if (selectedRoles.length === 0) return show('#user-form-error', t('usuarios.elegirPerfil'));
  if (!editing && !syncPasswordRules()) return show('#user-form-error', t('usuarios.contrasenaInicialPolitica'));

  const base = {
    organizationId: Number($<HTMLSelectElement>('#form-user-organization')?.value),
    nombre: value('#form-user-name'),
    email: value('#form-user-email'),
    roles: selectedRoles,
  };
  // Cambiar los perfiles o la organización propios cierra la sesión actual: se avisa antes.
  const selfSessionCloses = editing !== null && editing.id === me?.userId && (
    base.organizationId !== editing.organizationId
    || selectedRoles.length !== editing.roles.length
    || selectedRoles.some((role) => !editing!.roles.includes(role)));
  if (selfSessionCloses) {
    userDialog?.close();
    const accepted = await confirmAction(t('usuarios.cambiarPropia'), t('usuarios.cambiarPropiaAyuda'), t('usuarios.guardarSalir'));
    if (!accepted) return userDialog?.showModal();
  }
  const response = editing
    ? await authFetch(`/api/users/${editing.id}`, jsonRequest('PUT', base))
    : await authFetch('/api/users', jsonRequest('POST', {
      ...base,
      nombreUsuario: value('#form-user-username'),
      password: $<HTMLInputElement>('#form-user-password')?.value ?? '',
    }));
  if (!response.ok) return show('#user-form-error', await readApiError(response));
  const saved = await response.json() as User;
  userDialog?.close();
  if (selfSessionCloses) return logout();
  notify('#users-message', t(editing ? 'usuarios.actualizado' : 'usuarios.registrado', { usuario: saved.nombreUsuario }));
  await loadUsers();
});

// Contraseña
const syncResetRules = () => updatePasswordRules($('#reset-password-rules'), $<HTMLInputElement>('#reset-password')?.value ?? '', resetting?.nombreUsuario ?? '');
$('#reset-password')?.addEventListener('input', syncResetRules);

$<HTMLFormElement>('#password-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!resetting) return;
  show('#password-form-error', null);
  const password = $<HTMLInputElement>('#reset-password')?.value ?? '';
  if (!syncResetRules()) return show('#password-form-error', t('restablecer.politica'));
  if (password !== $<HTMLInputElement>('#reset-password-confirm')?.value) {
    return show('#password-form-error', t('restablecer.distintas'));
  }
  const response = await authFetch(`/api/users/${resetting.id}/password`, jsonRequest('POST', { password }));
  if (!response.ok) return show('#password-form-error', await readApiError(response));
  passwordDialog?.close();
  notify('#users-message', t('usuarios.contrasenaRestablecida', { usuario: resetting.nombreUsuario }));
  await loadUsers();
});

// Acciones por fila
$('#users-body')?.addEventListener('click', async (event) => {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-action]');
  const row = button?.closest<HTMLElement>('[data-user-id]');
  const user = users.find((item) => item.id === Number(row?.dataset.userId));
  if (!button || !user) return;
  show('#users-error', null);

  if (button.dataset.action === 'edit') return openUserDialog(user);
  if (button.dataset.action === 'password') {
    resetting = user;
    $<HTMLFormElement>('#password-form')?.reset();
    $('#reset-password-rules')!.innerHTML = passwordRulesHtml();
    show('#password-form-error', null);
    $('#password-dialog-title')!.textContent = t('usuarios.nuevaContrasenaPara', { usuario: user.nombreUsuario });
    syncResetRules();
    return passwordDialog?.showModal();
  }

  let response: Response | null = null;
  if (button.dataset.action === 'unlock') {
    response = await authFetch(`/api/users/${user.id}/unlock`, { method: 'POST' });
    if (response.ok) notify('#users-message', t('usuarios.desbloqueada', { usuario: user.nombreUsuario }));
  } else {
    const disable = button.dataset.action === 'disable';
    const accepted = await confirmAction(
      t(disable ? 'usuarios.deshabilitarA' : 'usuarios.habilitarA', { usuario: user.nombreUsuario }),
      t(disable ? 'usuarios.deshabilitarAyuda' : 'usuarios.habilitarAyuda'),
      t(disable ? 'organizaciones.deshabilitar' : 'organizaciones.habilitar'));
    if (!accepted) return;
    response = await authFetch(`/api/users/${user.id}/status`, jsonRequest('PATCH', { estado: disable ? 'DESHABILITADO' : 'ACTIVO' }));
    if (response.ok) notify('#users-message', t(disable ? 'usuarios.deshabilitadoAviso' : 'usuarios.habilitadoAviso', { usuario: user.nombreUsuario }));
  }
  if (!response.ok) return show('#users-error', await readApiError(response));
  await loadUsers();
});

// Perfiles
$('#role-list')?.addEventListener('click', (event) => {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-role-id]');
  editingRole = roles.find((role) => role.id === Number(button?.dataset.roleId)) ?? null;
  if (!editingRole) return;
  show('#role-form-error', null);
  $('#role-dialog-title')!.textContent = ROLE_LABEL[editingRole.nombre] ?? editingRole.nombre;
  $<HTMLTextAreaElement>('#role-description')!.value = editingRole.descripcion;
  roleDialog?.showModal();
});

$<HTMLFormElement>('#role-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!editingRole) return;
  const descripcion = ($<HTMLTextAreaElement>('#role-description')?.value ?? '').trim();
  if (!descripcion) return show('#role-form-error', t('usuarios.descripcionObligatoria'));
  const response = await authFetch(`/api/roles/${editingRole.id}`, jsonRequest('PUT', { descripcion }));
  if (!response.ok) return show('#role-form-error', await readApiError(response));
  const saved = await response.json() as Role;
  roles = roles.map((role) => (role.id === saved.id ? saved : role));
  renderRoles();
  roleDialog?.close();
  notify('#users-message', t('usuarios.descripcionActualizada', { perfil: ROLE_LABEL[saved.nombre] ?? saved.nombre }));
});

document.querySelectorAll<HTMLButtonElement>('[data-close-dialog]').forEach((button) => {
  button.addEventListener('click', () => button.closest('dialog')?.close());
});
$('#user-search')?.addEventListener('input', renderUsers);
$('#user-status-filter')?.addEventListener('change', renderUsers);

loadAll().catch((error) => show('#users-error', error instanceof Error ? error.message : t('usuarios.errorCargar')));
