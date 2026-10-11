// Sedes visibles para el usuario (management-service). Se usan para mostrar nombres y filtrar por sede.
import { authFetch, getStoredUser, hasRole } from './auth';
import type { Organization, Site } from './api';

/** Administrador: todas las organizaciones; resto: solo la propia. Devuelve [] si algo falla. */
export async function loadVisibleSites(): Promise<Site[]> {
  try {
    let organizationIds: number[];
    if (hasRole('ADMINISTRADOR_SISTEMA')) {
      const response = await authFetch('/api/client-organizations');
      if (!response.ok) return [];
      organizationIds = (await response.json() as Organization[]).map((organization) => organization.id);
    } else {
      const own = getStoredUser()?.organizationId;
      organizationIds = own ? [own] : [];
    }
    const responses = await Promise.all(
      organizationIds.map((id) => authFetch(`/api/client-organizations/${id}/sites`)),
    );
    const sites: Site[] = [];
    for (const response of responses) {
      if (response.ok) sites.push(...await response.json() as Site[]);
    }
    return sites;
  } catch {
    return [];
  }
}
