// Política de contraseñas, igual que PoliticaContrasena en management-service. Acá solo se muestra el avance
// mientras se escribe; el backend la vuelve a validar.
import { t } from './i18n';

export type PasswordRule = { id: string; text: string; ok: (password: string, username: string) => boolean };

export const PASSWORD_RULES: PasswordRule[] = [
  { id: 'length', text: t('contrasena.largo'), ok: (p) => p.length >= 10 && p.length <= 72 },
  { id: 'mix', text: t('contrasena.mezcla'), ok: (p) => /\p{L}/u.test(p) && /\d/.test(p) },
  {
    id: 'username',
    text: t('contrasena.usuario'),
    ok: (p, u) => p.length > 0 && (!u.trim() || !p.toLowerCase().includes(u.trim().toLowerCase())),
  },
];

export const passwordRulesHtml = () =>
  PASSWORD_RULES.map((rule) => `<li data-rule="${rule.id}">${rule.text}</li>`).join('');

/** Marca las reglas cumplidas en la lista y devuelve si la contraseña cumple todas. */
export function updatePasswordRules(list: HTMLElement | null, password: string, username: string): boolean {
  let valid = true;
  PASSWORD_RULES.forEach((rule) => {
    const ok = rule.ok(password, username);
    valid &&= ok;
    list?.querySelector(`[data-rule="${rule.id}"]`)?.classList.toggle('is-ok', ok);
  });
  return valid;
}
