import sys
import time
from pathlib import Path

import pytest
from playwright.sync_api import Error as ErrorPlaywright
from playwright.sync_api import sync_playwright

sys.path.insert(0, str(Path(__file__).parent))
import servidor  # noqa: E402

SITIO = Path(__file__).resolve().parents[2] / "frontend" / "web-app" / "dist"

ESCRITORIO = {"width": 1366, "height": 620}
CELULAR = {"width": 390, "height": 844}


@pytest.fixture(scope="session")
def entorno():
    if not (SITIO / "login" / "index.html").exists():
        pytest.exit("Falta el sitio compilado: en frontend/web-app correr `npm run build:pruebas`.", returncode=2)
    if any("localhost:808" in js.read_text(encoding="utf-8") for js in (SITIO / "_astro").glob("*.js")):
        pytest.exit("El sitio apunta a localhost:8081/8082: compilarlo con `npm run build:pruebas`.", returncode=2)
    url, registro, http = servidor.iniciar(SITIO)
    yield {"url": url, "registro": registro}
    http.shutdown()


@pytest.fixture(scope="session")
def navegador():
    with sync_playwright() as playwright:
        chromium = playwright.chromium.launch()
        yield chromium
        chromium.close()


def _esperar(pagina, expresion, ms):
    """Como wait_for_function, que la política de seguridad de la página no deja usar (evalúa texto como código)."""
    limite = time.monotonic() + ms / 1000
    while True:
        try:
            if pagina.evaluate(expresion):
                return
        except ErrorPlaywright:
            pass
        if time.monotonic() > limite:
            raise AssertionError(f"No se cumplió a tiempo: {expresion}")
        pagina.wait_for_timeout(50)


@pytest.fixture
def abrir(entorno, navegador):
    """Abre una página con sesión iniciada (o sin sesión si perfil es None) y junta los errores de JavaScript."""
    contextos = []

    def _abrir(ruta, perfil=None, tamanio=ESCRITORIO, idioma=None, sistema_oscuro=False):
        """idioma: el elegido en el navegador (sin sesión) o el guardado en la cuenta (con sesión).
        sistema_oscuro: el sistema operativo pide colores oscuros."""
        contexto = navegador.new_context(viewport=tamanio, color_scheme="dark" if sistema_oscuro else "light")
        contextos.append(contexto)
        if idioma and not perfil:
            contexto.add_init_script(f"localStorage.getItem('auditnet.idioma') || "
                                     f"localStorage.setItem('auditnet.idioma', '{idioma}');")
        if perfil:
            guardado = f", idioma: '{idioma}'" if idioma else ""
            # Solo al abrir: al recargar se conserva lo que la página haya guardado en la sesión.
            contexto.add_init_script(f"""
                if (!sessionStorage.getItem('tfi.auth.token')) {{
                  sessionStorage.setItem('tfi.auth.token', '{perfil}');
                  sessionStorage.setItem('tfi.auth.expiresAt', String(Date.now() + 3600e3));
                  sessionStorage.setItem('tfi.auth.user', JSON.stringify({{userId: 1, nombre: 'Operador',
                    username: 'operador', email: 'operador@acme.test', organizationId: 2,
                    roles: ['{perfil}']{guardado}}}));
                }}
            """)
        pagina = contexto.new_page()
        pagina.errores = []
        pagina.esperar = lambda expresion, ms=5000: _esperar(pagina, expresion, ms)
        pagina.on("pageerror", lambda error: pagina.errores.append(str(error)))
        pagina.on("console", lambda mensaje: "Content Security Policy" in mensaje.text
                  and pagina.errores.append(mensaje.text))
        pagina.on("dialog", lambda dialogo: (pagina.errores.append("diálogo inesperado: " + dialogo.message),
                                             dialogo.dismiss()))
        pagina.goto(entorno["url"] + ruta)
        if perfil:
            pagina.wait_for_selector("#protected-shell:not(.is-hidden)")
        return pagina

    yield _abrir
    for contexto in contextos:
        contexto.close()
