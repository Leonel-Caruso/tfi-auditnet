// Mensajes y textos que las pantallas muestran en sus elementos.

const ocultar = new WeakMap<HTMLElement, number>();

/** Muestra el mensaje en el elemento; con null lo oculta. */
export function show(selector: string, message: string | null): void {
  const node = document.querySelector<HTMLElement>(selector);
  if (!node) return;
  node.textContent = message ?? '';
  node.classList.toggle('is-hidden', !message);
}

/** Aviso que se oculta solo a los pocos segundos; un aviso nuevo reemplaza al anterior. */
export function notify(selector: string, message: string, kind: 'success' | 'error' = 'success', ms = 4500): void {
  const node = document.querySelector<HTMLElement>(selector);
  if (!node) return;
  node.textContent = message;
  node.classList.toggle('alert-success', kind === 'success');
  node.classList.toggle('alert-error', kind === 'error');
  node.classList.remove('is-hidden');
  window.clearTimeout(ocultar.get(node));
  ocultar.set(node, window.setTimeout(() => node.classList.add('is-hidden'), ms));
}

export function setText(selector: string, text: string): void {
  const node = document.querySelector(selector);
  if (node) node.textContent = text;
}

/** El HTML tiene que venir con los datos ya escapados. */
export function setHtml(selector: string, html: string): void {
  const node = document.querySelector(selector);
  if (node) node.innerHTML = html;
}

/**
 * Pide confirmación con el diálogo de la consola (está en MasterLayout). A diferencia del confirm del navegador,
 * respeta el idioma y el estilo de la consola.
 */
export function confirmAction(title: string, text: string, accept: string): Promise<boolean> {
  const dialog = document.querySelector<HTMLDialogElement>('#confirm-dialog');
  if (!dialog) return Promise.resolve(false);
  setText('#confirm-title', title);
  setText('#confirm-text', text);
  setText('#confirm-accept', accept);
  dialog.returnValue = 'cancel';
  return new Promise((resolve) => {
    dialog.addEventListener('close', () => resolve(dialog.returnValue === 'ok'), { once: true });
    dialog.showModal();
  });
}

/** Lleva el elemento a la vista; sin animación si el sistema pide reducir el movimiento. */
export function scrollToView(node: Element | null): void {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  node?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' });
}
