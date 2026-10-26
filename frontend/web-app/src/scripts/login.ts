// Inicio de sesión.
import { getToken, isSessionExpired, login } from '../lib/auth';
import { t } from '../lib/i18n';
import { animarLogo } from '../lib/logo-animado';

animarLogo(document.querySelector<HTMLElement>('#hero-mark'));

if (getToken() && !isSessionExpired()) {
  window.location.replace('/dashboard');
}

const form = document.querySelector<HTMLFormElement>('#login-form');
const errorBox = document.querySelector<HTMLDivElement>('#login-error');
const button = document.querySelector<HTMLButtonElement>('#login-button');

const reason = new URLSearchParams(window.location.search).get('reason');
if (reason === 'expired' && errorBox) {
  errorBox.textContent = t('login.sesionTerminada');
  errorBox.classList.remove('is-hidden');
  window.history.replaceState({}, '', '/login');
}

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox?.classList.add('is-hidden');

  const identity = document.querySelector<HTMLInputElement>('#identity')?.value.trim();
  const password = document.querySelector<HTMLInputElement>('#password')?.value;

  if (!identity || !password) return;

  if (button) {
    button.disabled = true;
    button.textContent = t('login.validando');
  }

  try {
    await login(identity, password);
    window.location.assign('/dashboard');
  } catch (error) {
    if (errorBox) {
      errorBox.textContent = error instanceof Error ? error.message : t('login.error');
      errorBox.classList.remove('is-hidden');
    }
  } finally {
    if (button) {
      button.disabled = false;
      button.textContent = t('login.ingresar');
    }
  }
});
