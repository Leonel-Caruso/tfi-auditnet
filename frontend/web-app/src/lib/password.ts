// Política de contraseñas, igual que PoliticaContrasena en management-service. Acá solo se muestra el avance
// mientras se escribe; el backend la vuelve a validar.

export type PasswordRule = { id: string; text: string; ok: (password: string, username: string) => boolean };

export const PASSWORD_RULES: PasswordRule[] = [
  { id: 'length', text: 'entre 10 y 72 caracteres', ok: (p) => p.length >= 10 && p.length <= 72 },
  { id: 'mix', text: 'combina letras y números', ok: (p) => /\p{L}/u.test(p) && /\d/.test(p) },
  {
    id: 'username',
    text: 'no contiene el nombre de usuario',
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
