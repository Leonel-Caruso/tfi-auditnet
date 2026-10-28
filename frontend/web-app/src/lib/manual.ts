// Manuales de uso en PDF: uno por perfil e idioma (public/manuales), generados con docs/manuales/generar.py.
import { PROFILE_ORDER, ROLE_LABEL } from './api';
import { getStoredUser } from './auth';
import { escapeHtml } from './format';
import { idiomaActual, t } from './i18n';

const ARCHIVO: Record<string, string> = {
  ADMINISTRADOR_SISTEMA: 'administrador-sistema',
  ADMINISTRADOR_ORGANIZACION: 'administrador-organizacion',
  ANALISTA_RED: 'analista-redes',
  AUDITOR_TECNICO: 'auditor-tecnico',
  RESPONSABLE_GESTION_IT: 'responsable-gestion-it',
};

export function urlManual(perfil: string): string {
  return `/manuales/manual-${ARCHIVO[perfil]}-${idiomaActual()}.pdf`;
}

/** Los perfiles del usuario que tienen manual, del de mayor alcance al de menor. */
export function perfilesConManual(): string[] {
  const roles = getStoredUser()?.roles ?? [];
  return PROFILE_ORDER.filter((perfil) => roles.includes(perfil) && ARCHIVO[perfil]);
}

/** Un enlace de descarga por perfil. */
export function enlacesManual(clase: string, clave: 'manual.descargar' | 'manual.enlace'): string {
  return perfilesConManual().map((perfil) => {
    const nombre = urlManual(perfil).split('/').pop();
    return `<a class="${clase}" href="${urlManual(perfil)}" download="${nombre}" data-perfil="${perfil}">`
      + `${escapeHtml(t(clave, { perfil: ROLE_LABEL[perfil] }))}</a>`;
  }).join('');
}
