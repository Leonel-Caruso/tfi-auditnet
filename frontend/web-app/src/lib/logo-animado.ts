// Logo del login (Bloque 7): el "_" titila como un cursor y cada tanto se escribe "AuditNet_", queda un momento y se
// borra hasta volver a "A_". Con "reducir movimiento" activado en el sistema, el logo queda fijo.

const PALABRA = 'uditNet';
const ESPERA_INICIAL = 1800;
const ENTRE_CICLOS = 5000;
const TECLEO = 110;
const BORRADO = 55;
const PAUSA_ESCRITO = 2600;

const esperar = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

export function animarLogo(logo: HTMLElement | null): void {
  const escrito = logo?.querySelector<HTMLElement>('.hero-mark-typed');
  if (!logo || !escrito) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    logo.classList.add('is-static');
    return;
  }

  const ciclo = async () => {
    await esperar(ESPERA_INICIAL);
    for (;;) {
      if (document.visibilityState === 'visible') {
        logo.classList.add('is-typing');
        for (let i = 1; i <= PALABRA.length; i += 1) {
          escrito.textContent = PALABRA.slice(0, i);
          await esperar(TECLEO + Math.random() * 60);
        }
        logo.classList.remove('is-typing');
        await esperar(PAUSA_ESCRITO);
        logo.classList.add('is-typing');
        for (let i = PALABRA.length - 1; i >= 0; i -= 1) {
          escrito.textContent = PALABRA.slice(0, i);
          await esperar(BORRADO);
        }
        logo.classList.remove('is-typing');
      }
      await esperar(ENTRE_CICLOS);
    }
  };
  void ciclo();
}
