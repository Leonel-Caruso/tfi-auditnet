import {
  AUDIT_CORE_API_URL,
  MANAGEMENT_API_URL,
  readApiError,
  type CurrentUser,
  type LoginResponse,
} from './api';

const TOKEN_KEY = 'tfi.auth.token';
const USER_KEY = 'tfi.auth.user';
const EXPIRES_KEY = 'tfi.auth.expiresAt';

export type StoredUser = Pick<
  LoginResponse,
  'userId' | 'organizationId' | 'nombre' | 'username' | 'email' | 'roles'
>;

export async function login(identidad: string, password: string): Promise<LoginResponse> {
  let response: Response;
  try {
    response = await fetch(`${MANAGEMENT_API_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identidad, password }),
    });
  } catch {
    throw new Error('No se pudo conectar con el servicio de acceso. Verificá management-service en el puerto 8081.');
  }

  if (!response.ok) {
    throw new Error(await readApiError(response));
  }

  const result = (await response.json()) as LoginResponse;
  saveSession(result);
  return result;
}

export function saveSession(result: LoginResponse): void {
  const user: StoredUser = {
    userId: result.userId,
    organizationId: result.organizationId,
    nombre: result.nombre,
    username: result.username,
    email: result.email,
    roles: result.roles,
  };

  sessionStorage.setItem(TOKEN_KEY, result.accessToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + result.expiresIn * 1000));
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
    throw new Error('Sesión inexistente.');
  }

  if (isSessionExpired()) {
    redirectToLogin('expired');
    throw new Error('La sesión venció.');
  }

  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);

  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers,
    });
  } catch {
    throw new Error('No se pudo conectar con la API. Verificá que los servicios backend estén iniciados.');
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

export function hasRole(role: string): boolean {
  return getStoredUser()?.roles?.includes(role) ?? false;
}
