// Calendario propio para los campos de fecha. El <input> original queda oculto en el formulario con el mismo
// nombre y formato (date: AAAA-MM-DD, datetime-local: AAAA-MM-DDTHH:mm).
// Con data-time="end" (campo "hasta"), un día sin hora toma 23:59 en lugar de 00:00.

const MONTHS = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const WEEKDAYS = ['lu', 'ma', 'mi', 'ju', 'vi', 'sá', 'do'];

type Mode = 'date' | 'datetime';
type Parts = { year: number; month: number; day: number; hour: number; minute: number };

const pad = (value: number) => String(value).padStart(2, '0');

function parse(value: string, mode: Mode): Parts | null {
  const match = mode === 'date'
    ? /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
    : /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(value);
  if (!match) return null;
  const [year, month, day, hour = 0, minute = 0] = match.slice(1).map(Number);
  const check = new Date(year, month - 1, day);
  if (check.getMonth() !== month - 1 || hour > 23 || minute > 59) return null;
  return { year, month: month - 1, day, hour, minute };
}

function serialize(parts: Parts, mode: Mode): string {
  const date = `${parts.year}-${pad(parts.month + 1)}-${pad(parts.day)}`;
  return mode === 'date' ? date : `${date}T${pad(parts.hour)}:${pad(parts.minute)}`;
}

function display(parts: Parts | null, mode: Mode): string {
  if (!parts) return '';
  const date = `${pad(parts.day)}/${pad(parts.month + 1)}/${parts.year}`;
  return mode === 'date' ? date : `${date} ${pad(parts.hour)}:${pad(parts.minute)}`;
}

const sameDay = (a: Parts | null, year: number, month: number, day: number) =>
  a !== null && a.year === year && a.month === month && a.day === day;

let openPicker: (() => void) | null = null;

function enhance(input: HTMLInputElement): void {
  if (input.dataset.datepicker === 'ready') return;
  const mode: Mode = input.type === 'datetime-local' ? 'datetime' : 'date';
  const endOfDay = input.dataset.time === 'end';
  const label = input.closest('label')?.querySelector('span')?.textContent?.trim() ?? 'fecha';
  const initial = input.value;

  // El input original queda en el formulario como texto oculto (así form.reset() lo vacía) y conserva el nombre.
  input.type = 'text';
  input.value = initial;
  input.dataset.datepicker = 'ready';
  input.tabIndex = -1;
  input.readOnly = true; // solo se cambia con el calendario (sigue enviándose en el formulario)
  input.setAttribute('aria-hidden', 'true');
  input.classList.add('dp-native');

  const field = document.createElement('div');
  field.className = 'dp-field';
  input.before(field);
  field.append(input);

  const trigger = document.createElement('button');
  trigger.type = 'button';
  trigger.className = 'dp-trigger technical-input';
  trigger.setAttribute('aria-haspopup', 'dialog');
  trigger.setAttribute('aria-expanded', 'false');
  trigger.innerHTML = `<span class="dp-value"></span><svg class="dp-icon" viewBox="0 0 16 16" aria-hidden="true"><rect x="2" y="3" width="12" height="11" rx="2"/><path d="M2 6.5h12M5.5 1.5v3M10.5 1.5v3"/></svg>`;
  field.append(trigger);
  const valueNode = trigger.querySelector<HTMLElement>('.dp-value')!;

  const popover = document.createElement('div');
  popover.className = 'dp-popover';
  popover.setAttribute('role', 'dialog');
  popover.setAttribute('aria-label', `Elegir ${label}`);
  popover.hidden = true;
  field.append(popover);

  // Las pantallas asignan valores por código (ej. el rango por defecto de reportes): se intercepta la propiedad
  // value para que el texto visible se actualice solo.
  const native = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!;
  const read = () => String(native.get!.call(input) ?? '');
  Object.defineProperty(input, 'value', {
    configurable: true,
    get: read,
    set: (next: string) => { native.set!.call(input, next); sync(); },
  });

  let view = { year: 0, month: 0 };
  let focusDay = 1;

  function sync(): void {
    const parts = parse(read(), mode);
    valueNode.textContent = display(parts, mode) || (mode === 'date' ? 'dd/mm/aaaa' : 'dd/mm/aaaa --:--');
    trigger.classList.toggle('is-empty', !parts);
    trigger.setAttribute('aria-label', `${label}: ${display(parts, mode) || 'sin fecha'}`);
  }

  function commit(next: string): void {
    native.set!.call(input, next);
    sync();
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function current(): Parts | null {
    return parse(read(), mode);
  }

  function render(): void {
    const selected = current();
    const today = new Date();
    const first = new Date(view.year, view.month, 1);
    const offset = (first.getDay() + 6) % 7; // semana de lunes a domingo
    const daysInMonth = new Date(view.year, view.month + 1, 0).getDate();
    focusDay = Math.min(Math.max(focusDay, 1), daysInMonth);

    const cells: string[] = [];
    for (let i = 0; i < offset; i += 1) cells.push('<span class="dp-blank"></span>');
    for (let day = 1; day <= daysInMonth; day += 1) {
      const classes = ['dp-day'];
      if (sameDay(selected, view.year, view.month, day)) classes.push('is-selected');
      if (today.getFullYear() === view.year && today.getMonth() === view.month && today.getDate() === day) classes.push('is-today');
      cells.push(`<button type="button" class="${classes.join(' ')}" data-day="${day}" tabindex="${day === focusDay ? 0 : -1}" aria-label="${day} de ${MONTHS[view.month]} de ${view.year}"${sameDay(selected, view.year, view.month, day) ? ' aria-pressed="true"' : ''}>${day}</button>`);
    }

    const hour = selected?.hour ?? (endOfDay ? 23 : 0);
    const minute = selected?.minute ?? (endOfDay ? 59 : 0);
    const options = (max: number, value: number) => Array.from({ length: max }, (_, n) =>
      `<option value="${n}"${n === value ? ' selected' : ''}>${pad(n)}</option>`).join('');

    popover.innerHTML = `
      <div class="dp-head">
        <button type="button" class="dp-nav" data-nav="-1" aria-label="Mes anterior">‹</button>
        <strong>${MONTHS[view.month]} <span>${view.year}</span></strong>
        <button type="button" class="dp-nav" data-nav="1" aria-label="Mes siguiente">›</button>
      </div>
      <div class="dp-grid" role="grid">
        ${WEEKDAYS.map((name) => `<span class="dp-weekday">${name}</span>`).join('')}
        ${cells.join('')}
      </div>
      ${mode === 'datetime' ? `
      <div class="dp-time">
        <span>hora</span>
        <select class="technical-select" data-part="hour" aria-label="Hora">${options(24, hour)}</select>
        <b>:</b>
        <select class="technical-select" data-part="minute" aria-label="Minutos">${options(60, minute)}</select>
      </div>` : ''}
      <div class="dp-foot">
        <button type="button" class="dp-link" data-action="today">hoy</button>
        <button type="button" class="dp-link" data-action="clear">limpiar</button>
      </div>`;
  }

  function choose(day: number): void {
    const previous = current();
    const hour = Number(popover.querySelector<HTMLSelectElement>('[data-part=hour]')?.value ?? previous?.hour ?? (endOfDay ? 23 : 0));
    const minute = Number(popover.querySelector<HTMLSelectElement>('[data-part=minute]')?.value ?? previous?.minute ?? (endOfDay ? 59 : 0));
    commit(serialize({ year: view.year, month: view.month, day, hour, minute }, mode));
    if (mode === 'date') close(true);
    else {
      focusDay = day;
      render();
      popover.querySelector<HTMLElement>(`[data-day="${day}"]`)?.focus();
    }
  }

  function moveMonth(delta: number): void {
    const next = new Date(view.year, view.month + delta, 1);
    view = { year: next.getFullYear(), month: next.getMonth() };
    render();
  }

  function open(): void {
    openPicker?.();
    const selected = current();
    const base = selected ? new Date(selected.year, selected.month, selected.day) : new Date();
    view = { year: base.getFullYear(), month: base.getMonth() };
    focusDay = base.getDate();
    render();
    popover.hidden = false;
    field.classList.add('is-open');
    trigger.setAttribute('aria-expanded', 'true');
    // Si no entra a la derecha (columna angosta o diálogo), se alinea al borde derecho del campo.
    popover.classList.remove('is-right');
    if (popover.getBoundingClientRect().right > window.innerWidth - 8) popover.classList.add('is-right');
    popover.querySelector<HTMLElement>('.dp-day[tabindex="0"]')?.focus();
    openPicker = () => close(false);
  }

  function close(returnFocus: boolean): void {
    if (popover.hidden) return;
    popover.hidden = true;
    field.classList.remove('is-open');
    trigger.setAttribute('aria-expanded', 'false');
    if (openPicker) openPicker = null;
    if (returnFocus) trigger.focus();
  }

  trigger.addEventListener('click', () => (popover.hidden ? open() : close(true)));
  // El <label> que envuelve el campo apunta al input oculto: un clic en el rótulo abre el calendario.
  input.addEventListener('click', () => { if (popover.hidden) open(); });
  input.addEventListener('focus', () => trigger.focus());
  field.addEventListener('focusout', (event) => {
    const next = event.relatedTarget as Node | null;
    if (next && !field.contains(next)) close(false);
  });

  popover.addEventListener('click', (event) => {
    const target = (event.target as HTMLElement).closest<HTMLElement>('button');
    if (!target) return;
    if (target.dataset.nav) moveMonth(Number(target.dataset.nav));
    else if (target.dataset.day) choose(Number(target.dataset.day));
    else if (target.dataset.action === 'clear') { commit(''); close(true); }
    else if (target.dataset.action === 'today') {
      const now = new Date();
      view = { year: now.getFullYear(), month: now.getMonth() };
      choose(now.getDate());
    }
  });

  popover.addEventListener('change', (event) => {
    const select = event.target as HTMLSelectElement;
    if (!select.dataset.part) return;
    const parts = current();
    if (!parts) return; // la hora se aplica cuando se elige el día
    if (select.dataset.part === 'hour') parts.hour = Number(select.value);
    else parts.minute = Number(select.value);
    commit(serialize(parts, mode));
  });

  popover.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation(); // dentro de un <dialog>, Escape cierra solo el calendario
      close(true);
      return;
    }
    const dayButton = (event.target as HTMLElement).closest<HTMLElement>('[data-day]');
    if (!dayButton) return;
    const steps: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 };
    if (event.key in steps) {
      event.preventDefault();
      const target = new Date(view.year, view.month, Number(dayButton.dataset.day) + steps[event.key]);
      const changedMonth = target.getMonth() !== view.month;
      view = { year: target.getFullYear(), month: target.getMonth() };
      focusDay = target.getDate();
      if (changedMonth) render();
      else {
        popover.querySelectorAll<HTMLElement>('[data-day]').forEach((node) => {
          node.tabIndex = Number(node.dataset.day) === focusDay ? 0 : -1;
        });
      }
      popover.querySelector<HTMLElement>(`[data-day="${focusDay}"]`)?.focus();
    } else if (event.key === 'PageUp' || event.key === 'PageDown') {
      event.preventDefault();
      moveMonth(event.key === 'PageUp' ? -1 : 1);
      popover.querySelector<HTMLElement>('.dp-day[tabindex="0"]')?.focus();
    }
  });

  document.addEventListener('click', (event) => {
    // composedPath: el botón del mes ya no está en el DOM cuando llega el evento (el calendario se redibujó).
    if (!event.composedPath().includes(field)) close(false);
  });

  // form.reset() vacía el input sin pasar por la propiedad value: se actualiza el texto después del reset.
  input.form?.addEventListener('reset', () => window.setTimeout(sync, 0));

  sync();
}

/** Aplica el calendario a todos los campos de fecha del contenedor. */
export function enhanceDateInputs(root: ParentNode = document): void {
  root.querySelectorAll<HTMLInputElement>('input[type="date"], input[type="datetime-local"]').forEach(enhance);
}
