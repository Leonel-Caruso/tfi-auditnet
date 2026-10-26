// Tema claro u oscuro. Se recuerda en este navegador y, con sesión, en la cuenta: al ingresar manda el de la cuenta.
// Hasta que se elige uno se usa el del sistema (public/inicio.js lo aplica antes de mostrar la página).
export type Tema = 'claro' | 'oscuro';

const CLAVE = 'auditnet.tema';

export function esTema(valor: unknown): valor is Tema {
  return valor === 'claro' || valor === 'oscuro';
}

export function temaActual(): Tema {
  return document.documentElement.dataset.tema === 'oscuro' ? 'oscuro' : 'claro';
}

/** El elegido en este navegador; null si nunca se eligió. */
export function temaDelNavegador(): Tema | null {
  try {
    const guardado = localStorage.getItem(CLAVE);
    return esTema(guardado) ? guardado : null;
  } catch {
    return null;
  }
}

/** Lo aplica y lo recuerda en este navegador. */
export function recordarTema(tema: Tema): void {
  try {
    localStorage.setItem(CLAVE, tema);
  } catch {
    // Se aplica igual en esta página aunque no se pueda recordar.
  }
  document.documentElement.dataset.tema = tema;
  marcar();
}

function marcar(): void {
  const actual = temaActual();
  document.querySelectorAll<HTMLButtonElement>('[data-opcion-tema]').forEach((boton) => {
    boton.setAttribute('aria-pressed', String(boton.dataset.opcionTema === actual));
  });
}

/** Conecta los botones Claro / Oscuro de la página. guardar: además lo guarda en la cuenta (con sesión). */
export function conectarSelectorTema(guardar?: (tema: Tema) => Promise<void>): void {
  document.querySelectorAll<HTMLButtonElement>('[data-opcion-tema]').forEach((boton) => {
    boton.addEventListener('click', () => {
      const tema = boton.dataset.opcionTema;
      if (!esTema(tema)) return;
      recordarTema(tema);
      guardar?.(tema).catch(() => {
        // Si no se pudo guardar en la cuenta, queda al menos en este navegador.
      });
    });
  });
  marcar();
  // Sin elección, el tema sigue al sistema también si cambia con la página abierta.
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => setTimeout(marcar));
}
