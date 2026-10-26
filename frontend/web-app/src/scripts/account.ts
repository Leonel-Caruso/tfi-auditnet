// Mi cuenta: datos de acceso, contraseña, idioma y avisos por correo.
import { authFetch, getStoredUser, saveLanguage, saveSession } from '../lib/auth';
import { jsonRequest, readApiError, ROLE_LABEL, type LoginResponse, type Organization } from '../lib/api';
import { formatDate } from '../lib/format';
import { conectarSelectorIdioma, t, type Idioma } from '../lib/i18n';
import { passwordRulesHtml, updatePasswordRules } from '../lib/password';
import { setText, show } from '../lib/ui';

const user = getStoredUser();
const field = (selector: string) => document.querySelector<HTMLInputElement>(selector)!;

const renderIdentity = () => {
  const current = getStoredUser();
  if (!current) return;
  setText('#account-name', current.nombre);
  setText('#account-username', `@${current.username}`);
  setText('#account-email', current.email);
  setText('#account-roles', current.roles.map((role) => ROLE_LABEL[role] ?? role).join(', '));
  const expires = Number(sessionStorage.getItem('tfi.auth.expiresAt'));
  setText('#account-expires', expires ? formatDate(new Date(expires).toISOString()) : '—');
};

const rules = document.querySelector<HTMLElement>('#account-rules');
if (rules) rules.innerHTML = passwordRulesHtml();
const syncRules = () => updatePasswordRules(rules, field('#account-new').value, user?.username ?? '');
field('#account-new').addEventListener('input', syncRules);

document.querySelector<HTMLFormElement>('#account-password-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  show('#account-error', null);
  const actual = field('#account-current').value;
  const nueva = field('#account-new').value;
  if (!actual) return show('#account-error', t('cuenta.faltaActual'));
  if (!syncRules()) return show('#account-error', t('cuenta.politica'));
  if (nueva !== field('#account-confirm').value) return show('#account-error', t('cuenta.distintas'));

  const submit = document.querySelector<HTMLButtonElement>('#account-submit');
  if (submit) submit.disabled = true;
  try {
    const response = await authFetch('/api/auth/password', jsonRequest('POST', { actual, nueva }));
    if (!response.ok) return show('#account-error', await readApiError(response));
    // El servidor devuelve un token nuevo: el anterior (y las otras sesiones) dejan de valer.
    saveSession(await response.json() as LoginResponse);
    (event.target as HTMLFormElement).reset();
    syncRules();
    renderIdentity();
    show('#account-message', t('cuenta.contrasenaCambiada'));
  } finally {
    if (submit) submit.disabled = false;
  }
});

// Al elegir otro idioma se guarda en la cuenta y la página se recarga traducida.
conectarSelectorIdioma(document.querySelector('#account-language') ?? document, async (idioma: Idioma) => {
  show('#language-error', null);
  try {
    await saveLanguage(idioma);
  } catch (error) {
    show('#language-error', (error as Error).message);
    throw error;
  }
});

// Qué avisos corresponden a cada perfil (el servidor aplica la misma regla al elegir destinatarios).
const NOTICE_ROLES: Record<string, string[]> = {
  alertas: ['ANALISTA_RED', 'RESPONSABLE_GESTION_IT', 'ADMINISTRADOR_ORGANIZACION'],
  vencimientos: ['ANALISTA_RED', 'RESPONSABLE_GESTION_IT', 'ADMINISTRADOR_ORGANIZACION'],
  resumen: ['RESPONSABLE_GESTION_IT', 'ADMINISTRADOR_ORGANIZACION'],
};
type Notices = { alertas: boolean; vencimientos: boolean; resumen: boolean };
const noticeForm = document.querySelector<HTMLFormElement>('#notifications-form');
const noticeInput = (name: keyof Notices) =>
  noticeForm?.querySelector<HTMLInputElement>(`input[name=${name}]`) ?? null;
const applicable = (Object.keys(NOTICE_ROLES) as (keyof Notices)[])
  .filter((notice) => user?.roles.some((role) => NOTICE_ROLES[notice].includes(role)));

document.querySelectorAll<HTMLElement>('[data-notice]').forEach((option) => {
  option.classList.toggle('is-hidden', !applicable.includes(option.dataset.notice as keyof Notices));
});
document.querySelector('#notifications-none')?.classList.toggle('is-hidden', applicable.length > 0);
document.querySelector('#notifications-submit')?.classList.toggle('is-hidden', applicable.length === 0);

const renderNotices = (notices: Notices) => {
  (Object.keys(NOTICE_ROLES) as (keyof Notices)[]).forEach((name) => {
    const input = noticeInput(name);
    if (input) input.checked = notices[name];
  });
};

if (applicable.length) {
  authFetch('/api/auth/me/notifications')
    .then(async (response) => {
      if (response.ok) renderNotices(await response.json() as Notices);
    })
    .catch(() => {});
}

noticeForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  show('#notifications-error', null);
  const payload: Notices = {
    alertas: noticeInput('alertas')?.checked ?? true,
    vencimientos: noticeInput('vencimientos')?.checked ?? true,
    resumen: noticeInput('resumen')?.checked ?? true,
  };
  const submit = document.querySelector<HTMLButtonElement>('#notifications-submit');
  if (submit) submit.disabled = true;
  try {
    const response = await authFetch('/api/auth/me/notifications', jsonRequest('PUT', payload));
    if (!response.ok) return show('#notifications-error', await readApiError(response));
    renderNotices(await response.json() as Notices);
    show('#account-message', t('cuenta.avisosGuardados'));
  } finally {
    if (submit) submit.disabled = false;
  }
});

renderIdentity();
syncRules();
if (user?.organizationId) {
  authFetch(`/api/client-organizations/${user.organizationId}`)
    .then(async (response) => {
      if (response.ok) setText('#account-organization', (await response.json() as Organization).razonSocial);
    })
    .catch(() => {});
}
