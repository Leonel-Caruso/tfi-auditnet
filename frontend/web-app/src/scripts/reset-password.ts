// Nueva contraseña desde el enlace del correo.
import { MANAGEMENT_API_URL, readApiError } from '../lib/api';
import { idiomaActual, t } from '../lib/i18n';
import { PASSWORD_RULES, updatePasswordRules } from '../lib/password';

const token = new URLSearchParams(window.location.search).get('token') ?? '';
// El token no queda en la barra de direcciones ni en el historial.
window.history.replaceState({}, '', '/reset-password');

const form = document.querySelector<HTMLFormElement>('#reset-form');
const fields = document.querySelector<HTMLFieldSetElement>('#reset-fields');
const submit = document.querySelector<HTMLButtonElement>('#reset-submit');
const errorBox = document.querySelector<HTMLElement>('#reset-error');
const password = document.querySelector<HTMLInputElement>('#reset-password')!;
const confirm = document.querySelector<HTMLInputElement>('#reset-confirm')!;
const rules = document.querySelector<HTMLElement>('#reset-rules');

// Esta pantalla no conoce el nombre de usuario: esa regla solo la controla el servidor, que avisa si no se cumple.
if (rules) {
  rules.innerHTML = PASSWORD_RULES.filter((rule) => rule.id !== 'username')
    .map((rule) => `<li data-rule="${rule.id}">${rule.text}</li>`).join('');
}
const syncRules = () => updatePasswordRules(rules, password.value, '');
password.addEventListener('input', syncRules);

const fail = (message: string) => {
  if (!errorBox) return;
  errorBox.textContent = message;
  errorBox.classList.remove('is-hidden');
};

if (!token) {
  document.querySelector('#reset-missing')?.classList.remove('is-hidden');
  if (fields) fields.disabled = true;
  submit?.classList.add('is-hidden');
}

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox?.classList.add('is-hidden');
  if (!syncRules()) return fail(t('restablecer.politica'));
  if (password.value !== confirm.value) return fail(t('restablecer.distintas'));
  if (submit) submit.disabled = true;
  try {
    const response = await fetch(`${MANAGEMENT_API_URL}/api/auth/password-reset`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept-Language': idiomaActual() },
      body: JSON.stringify({ token, password: password.value }),
    });
    if (!response.ok) throw new Error(await readApiError(response));
    form.reset();
    if (fields) fields.disabled = true;
    submit?.classList.add('is-hidden');
    document.querySelector('#reset-done')?.classList.remove('is-hidden');
  } catch (error) {
    fail(error instanceof Error ? error.message : t('restablecer.error'));
  } finally {
    if (submit) submit.disabled = false;
  }
});
