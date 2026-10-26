import {
  AUDIT_CORE_API_URL,
  jsonRequest,
  MANAGEMENT_API_URL,
  readApiError,
  type CurrentUser,
  type LoginResponse,
} from './api';
import { esIdioma, idiomaActual, recordarIdioma, t, type Idioma } from './i18n';
import { esTema, recordarTema, temaDelNavegador, type Tema } from './tema';

const TOKEN_KEY = 'tfi.auth.token';
const USER_KEY = 'tfi.auth.user';
const EXPIRES_KEY = 'tfi.auth.expiresAt';

export type StoredUser = Pick<
  LoginResponse,
  'userId' | 'organizationId' | 'nombre' | 'username' | 'email' | 'roles' | 'idioma'
>;

export async function login(identidad: string, password: string): Promise<LoginResponse> {
  let response: Response;
  try {
    response = await fetch(`${MANAGEMENT_API_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept-Language': idiomaActual() },
      body: JSON.stringify({ identidad, password }),
    });
  } catch {
    throw new Error(t('error.conexionAcceso'));
  }

  if (!response.ok) {
    throw new Error(await readApiError(response));
  }

  const result = (await response.json()) as LoginResponse;
  saveSession(result);
  // Si la cuenta todavía no tiene tema, se le guarda el que se eligió en la pantalla de acceso.
  const elegido = temaDelNavegador();
  if (!esTema(result.tema) && elegido) {
    await saveTheme(elegido).catch(() => undefined);
  }
  return result;
}

export function saveSession(result: LoginResponse): void {
  const idioma: Idioma = esIdioma(result.idioma) ? result.idioma : 'es';
  const user: StoredUser = {
    userId: result.userId,
    organizationId: result.organizationId,
    nombre: result.nombre,
    username: result.username,
    email: result.email,
    roles: result.roles,
    idioma,
  };

  sessionStorage.setItem(TOKEN_KEY, result.accessToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + result.expiresIn * 1000));
  // El idioma y el tema guardados del usuario mandan; también quedan para la pantalla de acceso de este navegador.
  recordarIdioma(idioma);
  if (esTema(result.tema)) recordarTema(result.tema);
}

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): StoredUser | null {
  const raw = sessionStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredUser;
  } catch {
    return null;
  }
}

export function isSessionExpired(): boolean {
  const raw = sessionStorage.getItem(EXPIRES_KEY);
  if (!raw) return true;
  return Number(raw) <= Date.now();
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
  sessionStorage.removeItem(EXPIRES_KEY);
}

export function logout(): void {
  clearSession();
  window.location.assign('/login');
}

function redirectToLogin(reason?: 'expired'): void {
  clearSession();
  const target = reason ? `/login?reason=${reason}` : '/login';
  window.location.assign(target);
}

async function authorizedFetch(
  baseUrl: string,
  path: string,
  init: RequestInit = {}
): Promise<Response> {
  const token = getToken();
  if (!token) {
    redirectToLogin();
    throw new Error(t('error.sinSesion'));
  }

  if (isSessionExpired()) {
    redirectToLogin('expired');
    throw new Error(t('error.sesionVencida'));
  }

  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);
  // Los servicios devuelven los mensajes en este idioma.
  headers.set('Accept-Language', idiomaActual());

  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers,
    });
  } catch {
    throw new Error(t('error.conexionApi'));
  }

  if (response.status === 401) {
    redirectToLogin('expired');
  }

  return response;
}

export function authFetch(path: string, init: RequestInit = {}): Promise<Response> {
  return authorizedFetch(MANAGEMENT_API_URL, path, init);
}

export function auditFetch(path: string, init: RequestInit = {}): Promise<Response> {
  return authorizedFetch(AUDIT_CORE_API_URL, path, init);
}

export async function verifySession(): Promise<CurrentUser> {
  const response = await authFetch('/api/auth/me');
  if (!response.ok) {
    throw new Error(await readApiError(response));
  }
  return (await response.json()) as CurrentUser;
}

/** Guarda el idioma en la cuenta: lo usan la consola en cualquier navegador y los correos. */
export async function saveLanguage(idioma: Idioma): Promise<void> {
  const response = await authFetch('/api/auth/me/language', jsonRequest('PUT', { idioma }));
  if (!response.ok) throw new Error(await readApiError(response));
}

/** Guarda el tema en la cuenta: lo usa la consola en cualquier navegador. */
export async function saveTheme(tema: Tema): Promise<void> {
  const response = await authFetch('/api/auth/me/theme', jsonRequest('PUT', { tema }));
  if (!response.ok) throw new Error(await readApiError(response));
}

export function hasRole(role: string): boolean {
  return getStoredUser()?.roles?.includes(role) ?? false;
}
