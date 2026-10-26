// Recuperación de contraseña: pedido del enlace por correo.
import { MANAGEMENT_API_URL, readApiError } from '../lib/api';
import { idiomaActual, t } from '../lib/i18n';

const form = document.querySelector<HTMLFormElement>('#recover-form');
const errorBox = document.querySelector<HTMLElement>('#recover-error');
const done = document.querySelector<HTMLElement>('#recover-done');
const submit = document.querySelector<HTMLButtonElement>('#recover-submit');

const fail = (message: string) => {
  if (!errorBox) return;
  errorBox.textContent = message;
  errorBox.classList.remove('is-hidden');
};

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox?.classList.add('is-hidden');
  const identidad = document.querySelector<HTMLInputElement>('#recover-identity')?.value.trim();
  if (!identidad) return fail(t('recuperar.falta'));
  if (submit) submit.disabled = true;
  try {
    const response = await fetch(`${MANAGEMENT_API_URL}/api/auth/password-recovery`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept-Language': idiomaActual() },
      body: JSON.stringify({ identidad }),
    });
    if (!response.ok) throw new Error(await readApiError(response));
    // La respuesta es la misma exista o no la cuenta: el texto sale del diccionario.
    if (done) {
      done.textContent = t('recuperar.enviado');
      done.classList.remove('is-hidden');
    }
    if (submit) submit.textContent = t('recuperar.reenviar');
  } catch (error) {
    fail(error instanceof Error ? error.message : t('recuperar.error'));
  } finally {
    if (submit) submit.disabled = false;
  }
});
