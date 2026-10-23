// Idioma de la consola. El texto en español queda en el HTML compilado; para inglés y portugués se reemplaza al cargar
// la página (elementos con data-i18n) y los scripts arman sus textos con t().
import es, { type Clave } from '../i18n/es';
import en from '../i18n/en';
import pt from '../i18n/pt';

export type Idioma = 'es' | 'en' | 'pt';
export type { Clave };

export const IDIOMAS: Idioma[] = ['es', 'en', 'pt'];
/** Elección hecha en el login o en la consola; se recuerda en el navegador. */
export const CLAVE_IDIOMA = 'auditnet.idioma';
const USUARIO = 'tfi.auth.user';

const DICCIONARIOS: Record<Idioma, Record<Clave, string>> = { es, en, pt };
const LOCALES: Record<Idioma, string> = { es: 'es-AR', en: 'en-US', pt: 'pt-BR' };

export const esIdioma = (valor: unknown): valor is Idioma => IDIOMAS.includes(valor as Idioma);

/** El del usuario con sesión, si no el elegido en este navegador, si no español. */
export function idiomaGuardado(): Idioma {
  try {
    const usuario = JSON.parse(sessionStorage.getItem(USUARIO) ?? 'null');
    if (esIdioma(usuario?.idioma)) return usuario.idioma;
    const elegido = localStorage.getItem(CLAVE_IDIOMA);
    if (esIdioma(elegido)) return elegido;
  } catch {
    // Sin almacenamiento disponible: español.
  }
  return 'es';
}

/** El que ya aplicó el script del encabezado (atributo lang). */
export function idiomaActual(): Idioma {
  if (typeof document === 'undefined') return 'es';
  const lang = document.documentElement.lang;
  return esIdioma(lang) ? lang : idiomaGuardado();
}

export const localeActual = () => LOCALES[idiomaActual()];

/** Texto en el idioma actual; {nombre} se reemplaza con datos[nombre]. */
export function t(clave: Clave, datos: Record<string, string | number> = {}): string {
  const texto = DICCIONARIOS[idiomaActual()][clave] ?? es[clave] ?? clave;
  return texto.replace(/\{(\w+)\}/g, (marca, nombre: string) =>
    nombre in datos ? String(datos[nombre]) : marca);
}

/** Recuerda el idioma en este navegador (y en la sesión abierta, si hay). No lo guarda en el servidor. */
export function recordarIdioma(idioma: Idioma): void {
  try {
    localStorage.setItem(CLAVE_IDIOMA, idioma);
    const usuario = JSON.parse(sessionStorage.getItem(USUARIO) ?? 'null');
    if (usuario) sessionStorage.setItem(USUARIO, JSON.stringify({ ...usuario, idioma }));
  } catch {
    // Sin almacenamiento: el cambio dura hasta recargar.
  }
}

const ATRIBUTOS: [string, string][] = [
  ['data-i18n-placeholder', 'placeholder'],
  ['data-i18n-title', 'title'],
  ['data-i18n-aria-label', 'aria-label'],
  ['data-i18n-label', 'data-label'],
];

/** Reemplaza los textos marcados con data-i18n (y sus atributos) por los del idioma actual. */
export function aplicarTraducciones(raiz: ParentNode = document): void {
  if (idiomaActual() === 'es') return;
  raiz.querySelectorAll<HTMLElement>('[data-i18n]').forEach((elemento) => {
    elemento.textContent = t(elemento.dataset.i18n as Clave);
  });
  for (const [marca, atributo] of ATRIBUTOS) {
    raiz.querySelectorAll<HTMLElement>(`[${marca}]`).forEach((elemento) => {
      elemento.setAttribute(atributo, t(elemento.getAttribute(marca) as Clave));
    });
  }
  const titulo = document.querySelector<HTMLMetaElement>('meta[name="i18n-titulo"]')?.content;
  if (titulo) document.title = `${t(titulo as Clave)} · AuditNet`;
}

/**
 * Conecta los botones ES/EN/PT: marca el idioma actual y, al elegir otro, llama a guardar (por ejemplo, en el
 * servidor), lo recuerda y recarga la página.
 */
export function conectarSelectorIdioma(raiz: ParentNode, guardar?: (idioma: Idioma) => Promise<void>): void {
  raiz.querySelectorAll<HTMLButtonElement>('[data-idioma]').forEach((boton) => {
    const idioma = boton.dataset.idioma;
    if (!esIdioma(idioma)) return;
    boton.setAttribute('aria-pressed', String(idioma === idiomaActual()));
    boton.addEventListener('click', async () => {
      if (idioma === idiomaActual()) return;
      boton.disabled = true;
      try {
        await guardar?.(idioma);
        recordarIdioma(idioma);
        window.location.reload();
      } catch {
        boton.disabled = false;
      }
    });
  });
}

/** Aplica las traducciones y muestra la página, que el encabezado ocultó mientras tanto. */
export function mostrarTraducida(): void {
  try {
    aplicarTraducciones();
  } finally {
    document.documentElement.classList.remove('i18n-pendiente');
  }
}
