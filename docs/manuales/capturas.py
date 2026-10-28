"""Capturas de pantalla para los manuales, en cada idioma, sobre el sitio de pruebas y su API simulada
(tests/pantallas/servidor.py): siempre los mismos datos, sin depender de un entorno real."""

import json
import sys
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

RAIZ = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(RAIZ / "tests" / "pantallas"))
sys.path.insert(0, str(Path(__file__).resolve().parent))
import datos  # noqa: E402
import datos_manual  # noqa: E402

datos_manual.aplicar(datos)
import servidor  # noqa: E402

# Detalles que las pruebas de pantalla no necesitan y las capturas sí.
servidor.RUTAS[:0] = [(rf"/api/{ruta}/(\d+)", {"GET"}, servidor._por_id(coleccion)) for ruta, coleccion in
                      (("devices", datos.DISPOSITIVOS), ("baselines", datos.BASELINES), ("rules", datos.REGLAS))]

SITIO = RAIZ / "frontend" / "web-app" / "dist"
VENTANA = {"width": 1280, "height": 800}

ADMIN = "ADMINISTRADOR_SISTEMA"
ORG = "ADMINISTRADOR_ORGANIZACION"
ANALISTA = "ANALISTA_RED"
AUDITOR = "AUDITOR_TECNICO"
RESPONSABLE = "RESPONSABLE_GESTION_IT"


def _clic(selector):
    def accion(pagina):
        pagina.wait_for_selector(selector)
        pagina.click(selector)
    return accion


def _esperar(selector):
    return lambda pagina: pagina.wait_for_selector(selector)


# nombre, perfil (None = sin sesión), ruta, acciones antes de capturar y qué capturar: None = la ventana; "#id" o
# ".clase" = ese elemento (diálogos, menús); "^selector" = la ventana con ese elemento arriba.
PLAN = [
    ("acceso", None, "/login", [], None),
    ("recuperar", None, "/recover", [], None),
    ("tablero", ANALISTA, "/dashboard", [_esperar("#dash-manual:not([hidden])"), _esperar("#dash-kpis .metric-link")],
     None),
    ("menu-usuario", ANALISTA, "/dashboard", [_clic("#user-menu-button")], "#user-popover"),
    ("cuenta", ANALISTA, "/account", [], None),
    ("cuenta-preferencias", ANALISTA, "/account", [_esperar("#account-language [aria-pressed=true]")],
     "^#language-panel"),
    ("estado-cliente", RESPONSABLE, "/client-status", [], None),
    ("organizaciones", ADMIN, "/organizations", [], None),
    ("organizacion-alta", ADMIN, "/organizations", [_clic("#open-organization-dialog")], "#organization-dialog"),
    ("organizacion-propia", ORG, "/organizations", [_esperar("#review-submit")], None),
    ("plazos-revision", ORG, "/organizations", [_esperar("#review-submit")], "^#review-panel"),
    ("usuarios", ADMIN, "/users", [_esperar("[data-user-id]")], None),
    ("usuario-alta", ORG, "/users", [_clic("#user-create")], "#user-dialog"),
    ("planes", ADMIN, "/plans", [], None),
    ("mi-plan", ORG, "/plan", [], None),
    ("dispositivos", ADMIN, "/devices", [_clic(".device-row")], None),
    ("dispositivo-alta", ADMIN, "/devices", [_clic("#open-device-dialog")], "#device-dialog"),
    ("baselines", AUDITOR, "/baselines", [_clic(".policy-row:nth-child(2)")], None),
    ("reglas", AUDITOR, "/rules", [_clic(".policy-row")], None),
    ("regla-alta", AUDITOR, "/rules", [_clic("button:has-text('+')")], "#rule-dialog"),
    ("configuracion-carga", ANALISTA, "/audits", [_clic("#open-config-dialog")], "#config-dialog"),
    ("auditoria-resultado", ANALISTA, "/audits", [_clic("[data-audit-id]")], None),
    ("historial", ANALISTA, "/history", [_clic("[data-audit-id='41']")], None),
    ("hallazgos", ANALISTA, "/results", [_clic("[data-finding-id='4']")], None),
    ("hallazgo-seguimiento", ANALISTA, "/results", [_clic("[data-finding-id='4']"),
                                                    _esperar("#finding-inspector-content select")], ".finding-inspector"),
    ("alertas", RESPONSABLE, "/alerts", [_clic("[data-alert-id='2']")], None),
    ("alerta-derivar", RESPONSABLE, "/alerts", [_clic("[data-alert-id='2']"),
                                                _esperar("#alert-inspector-content select")], ".finding-inspector"),
    ("reportes", AUDITOR, "/reports", [], None),
    ("reporte-documento", AUDITOR, "/reports?reporte=3", [_esperar(".rpt")], "^.report-document"),
    ("bitacoras", ADMIN, "/logs", [_esperar("#logs-body tr, .data-table tbody tr")], None),
    ("administracion", ADMIN, "/admin", [_esperar(".health-component")], None),
]


def _sesion(contexto, perfil, idioma):
    usuario = json.dumps(datos_manual.SESION.get(perfil, {}))
    contexto.add_init_script(f"""
        localStorage.setItem('auditnet.tema', 'claro');
        localStorage.setItem('auditnet.idioma', '{idioma}');
        localStorage.removeItem('auditnet.manual.oculto');
    """)
    if perfil:
        contexto.add_init_script(f"""
            if (!sessionStorage.getItem('tfi.auth.token')) {{
              sessionStorage.setItem('tfi.auth.token', '{perfil}');
              sessionStorage.setItem('tfi.auth.expiresAt', String(Date.now() + 3600e3));
              sessionStorage.setItem('tfi.auth.user', JSON.stringify({{...{usuario}, roles: ['{perfil}'],
                idioma: '{idioma}', tema: 'claro'}}));
            }}
        """)


def sacar(idiomas, destino, solo=None):
    if not (SITIO / "login" / "index.html").exists():
        sys.exit("Falta el sitio de pruebas: en frontend/web-app correr `npm run build:pruebas`.")
    url, _registro, http = servidor.iniciar(SITIO)
    fallidas = []
    try:
        with sync_playwright() as playwright:
            navegador = playwright.chromium.launch()
            for idioma in idiomas:
                carpeta = destino / idioma
                carpeta.mkdir(parents=True, exist_ok=True)
                datos_manual.titular_reportes(idioma)
                for nombre, perfil, ruta, acciones, objetivo in PLAN:
                    if solo and nombre not in solo:
                        continue
                    contexto = navegador.new_context(viewport=VENTANA, color_scheme="light", locale=idioma)
                    _sesion(contexto, perfil, idioma)
                    try:
                        _capturar(contexto, url + ruta, perfil, acciones, objetivo, carpeta / f"{nombre}.png")
                    except Exception as error:  # noqa: BLE001 - se informa y se sigue con las demás
                        fallidas.append(f"{idioma}/{nombre}: {str(error).splitlines()[0]}")
                    contexto.close()
                print(f"  {idioma}: listo")
            navegador.close()
    finally:
        http.shutdown()
    if fallidas:
        sys.exit("No se pudieron sacar estas capturas:\n  " + "\n  ".join(fallidas))


def _capturar(contexto, url, perfil, acciones, objetivo, archivo):
    pagina = contexto.new_page()
    if objetivo and not objetivo.startswith("^"):
        # Ventana alta para que el elemento (diálogo, panel de detalle) entre completo, sin desplazarse.
        pagina.set_viewport_size({"width": VENTANA["width"], "height": 1400})
    pagina.goto(url)
    if perfil:
        pagina.wait_for_selector("#protected-shell:not(.is-hidden)")
    pagina.wait_for_load_state("networkidle")
    for accion in acciones:
        accion(pagina)
    pagina.wait_for_load_state("networkidle")
    pagina.wait_for_timeout(250)
    if "Sin simulación" in pagina.evaluate("document.body.innerText"):
        raise RuntimeError("la pantalla pidió datos que el servidor simulado no tiene")
    if objetivo and objetivo.startswith("^"):
        pagina.locator(objetivo[1:]).first.evaluate(
            "(n) => window.scrollTo(0, n.getBoundingClientRect().top + window.scrollY - 76)")
        pagina.wait_for_timeout(150)
        pagina.screenshot(path=str(archivo))
    elif objetivo:
        pagina.locator(objetivo).first.screenshot(path=str(archivo))
    else:
        pagina.screenshot(path=str(archivo))
    _reducir(archivo)


def _reducir(archivo):
    """Paleta de 192 colores sin tramado: las capturas de la consola se ven igual y pesan un tercio."""
    imagen = Image.open(archivo).convert("RGB")
    imagen.quantize(colors=192, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(archivo, optimize=True)


if __name__ == "__main__":
    sacar(sys.argv[1:] or ["es"], Path(__file__).resolve().parent / ".capturas")
