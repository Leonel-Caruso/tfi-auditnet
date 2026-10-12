// Paginador de tablas. En el navegador, recortar() pagina una lista ya cargada; en el servidor, actualizar()
// recibe el total que la API informa en X-Total-Count.

export const OPCIONES_POR_PAGINA = [10, 25, 50, 100];

export type Paginador = {
  readonly pagina: number;
  readonly porPagina: number;
  /** Informa el total de elementos y redibuja; si la página quedó fuera de rango vuelve a la última. */
  actualizar: (total: number) => void;
  /** Para listas cargadas completas: informa el total y devuelve solo los elementos de la página actual. */
  recortar: <T>(items: T[]) => T[];
  /** Vuelve a la primera página (al cambiar filtros o búsqueda). */
  reiniciar: () => void;
  /** Lleva a la página que contiene la posición indicada (desde 0). */
  irAPosicion: (indice: number) => void;
};

type Opciones = {
  porPagina?: number;
  /** Se llama cuando el usuario cambia de página o de tamaño de página. */
  alCambiar: () => void;
};

export function crearPaginador(contenedor: HTMLElement | null, opciones: Opciones): Paginador {
  let pagina = 1;
  let porPagina = opciones.porPagina ?? 25;
  let total = 0;

  const ultimaPagina = () => Math.max(1, Math.ceil(total / porPagina));

  // Los controles se redibujan en "cuerpo"; "anuncio" queda fijo para que el lector de pantalla lea el cambio de página.
  const cuerpo = document.createElement('div');
  cuerpo.className = 'pager-body';
  const anuncio = document.createElement('span');
  anuncio.className = 'pager-announce';
  anuncio.setAttribute('aria-live', 'polite');
  contenedor?.replaceChildren(cuerpo, anuncio);

  const dibujar = () => {
    if (!contenedor) return;
    // Al redibujar se recupera el foco del teclado en el mismo control (o en el otro botón si quedó deshabilitado).
    const activo = document.activeElement instanceof HTMLElement && contenedor.contains(document.activeElement)
      ? document.activeElement.dataset.pager ?? null
      : null;
    const oculto = total <= OPCIONES_POR_PAGINA[0];
    contenedor.classList.toggle('is-hidden', oculto);
    if (oculto) {
      cuerpo.innerHTML = '';
      anuncio.textContent = '';
      return;
    }
    const desde = total === 0 ? 0 : (pagina - 1) * porPagina + 1;
    const hasta = Math.min(total, pagina * porPagina);
    const opcionesHtml = OPCIONES_POR_PAGINA
      .map((valor) => `<option value="${valor}" ${valor === porPagina ? 'selected' : ''}>${valor}</option>`)
      .join('');
    const textoPagina = `Página ${pagina} de ${ultimaPagina()}`;
    if (activo) anuncio.textContent = `${textoPagina}, ${desde} a ${hasta} de ${total}`;
    cuerpo.innerHTML = `
      <span class="pager-range">${desde}–${hasta} de ${total}</span>
      <div class="pager-controls">
        <button type="button" class="button button-ghost button-small" data-pager="anterior" ${pagina <= 1 ? 'disabled' : ''}
          aria-label="Página anterior">‹<span class="pager-word"> Anterior</span></button>
        <span class="pager-page">${textoPagina}</span>
        <button type="button" class="button button-ghost button-small" data-pager="siguiente" ${pagina >= ultimaPagina() ? 'disabled' : ''}
          aria-label="Página siguiente"><span class="pager-word">Siguiente </span>›</button>
      </div>
      <label class="pager-size"><span>por página</span>
        <select class="technical-select" data-pager="tamanio" aria-label="Filas por página">${opcionesHtml}</select>
      </label>`;
    if (activo) {
      const control = cuerpo.querySelector<HTMLButtonElement | HTMLSelectElement>(`[data-pager="${activo}"]`);
      const alternativo = cuerpo.querySelector<HTMLButtonElement>('button[data-pager]:not(:disabled)');
      (control && !control.disabled ? control : alternativo)?.focus();
    }
  };

  contenedor?.addEventListener('click', (event) => {
    const boton = (event.target as HTMLElement).closest<HTMLButtonElement>('button[data-pager]');
    if (!boton || boton.disabled) return;
    pagina = Math.min(ultimaPagina(), Math.max(1, pagina + (boton.dataset.pager === 'siguiente' ? 1 : -1)));
    opciones.alCambiar();
  });
  contenedor?.addEventListener('change', (event) => {
    const select = event.target as HTMLSelectElement;
    if (select.dataset.pager !== 'tamanio') return;
    // Se mantiene a la vista el primer elemento que se estaba mirando.
    const primero = (pagina - 1) * porPagina;
    porPagina = Number(select.value) || porPagina;
    pagina = Math.floor(primero / porPagina) + 1;
    opciones.alCambiar();
  });

  return {
    get pagina() {
      return pagina;
    },
    get porPagina() {
      return porPagina;
    },
    actualizar(nuevoTotal: number) {
      total = Math.max(0, nuevoTotal);
      pagina = Math.min(pagina, ultimaPagina());
      dibujar();
    },
    recortar<T>(items: T[]): T[] {
      this.actualizar(items.length);
      return items.slice((pagina - 1) * porPagina, pagina * porPagina);
    },
    reiniciar() {
      pagina = 1;
    },
    irAPosicion(indice: number) {
      if (indice >= 0) pagina = Math.floor(indice / porPagina) + 1;
    },
  };
}

/** Total informado por la API en X-Total-Count; si falta (versión anterior), el largo de la página recibida. */
export function totalDeRespuesta(response: Response, recibidos: number): number {
  const total = Number(response.headers.get('X-Total-Count'));
  return Number.isFinite(total) && response.headers.has('X-Total-Count') ? total : recibidos;
}
