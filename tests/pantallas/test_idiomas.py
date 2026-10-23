"""Idioma de la consola: elección en el acceso, guardado en la cuenta y textos traducidos."""

import re

import pytest

from conftest import CELULAR
from test_pantallas import CASOS

# Una clave sin traducir se vería como "cuenta.titulo" o "hallazgos.ceroHallazgos".
PREFIJOS = ("acceso|admin|alerta|alertaTexto|alertas|archivo|auditorias|baselines|bitacoras|calendario|cambio|cliente|"
            "condicion|consola|contrasena|correo|cuenta|dispositivos|error|estadoCorreo|general|hallazgo|hallazgoTexto|"
            "hallazgos|historial|idioma|informe|informeEstado|informeResultado|informeSeveridad|login|menu|modalidad|"
            "organizaciones|paginador|perfil|permiso|plan|planes|recuperar|registro|regla|reglas|reportes|restablecer|"
            "resultado|riesgo|riesgoAyuda|ruta|severidad|solicitud|tablero|usuarios")
CLAVE_SUELTA = re.compile(r"\b(" + PREFIJOS + r")\.[a-zA-Z][\w-]*\b")
# Textos en español que no deberían quedar en inglés o portugués (los que se escriben igual en portugués no sirven).
ESPANOL = re.compile(r"\b(Guardar cambios|Seleccioná|Todavía no|No se pudo|Ver hallazgos|hallazgos pendientes|"
                     r"Dispositivo seleccionado|Detalle del|Generar reporte|Actualizar estado)\b")


def _pedidos(entorno, metodo, ruta):
    return [r for r in entorno["registro"] if r["metodo"] == metodo and r["ruta"] == ruta]


def _texto_y_atributos(pagina):
    """Todo el texto (también el de diálogos ocultos) y los atributos que lee un lector de pantalla."""
    return pagina.evaluate("""() => document.body.textContent + ' ' + Array.from(document.querySelectorAll(
        '[placeholder], [title], [aria-label], [data-label]')).map((n) => [n.getAttribute('placeholder'),
        n.getAttribute('title'), n.getAttribute('aria-label'), n.getAttribute('data-label')].join(' ')).join(' ')""")


def _sin_claves(pagina):
    texto = _texto_y_atributos(pagina)
    assert not CLAVE_SUELTA.search(texto), CLAVE_SUELTA.search(texto)


def test_el_acceso_se_muestra_en_el_idioma_elegido(abrir):
    pagina = abrir("/login", idioma="en")
    assert pagina.evaluate("document.documentElement.lang") == "en"
    assert pagina.locator("label:has(#identity) span").inner_text() == "username or email"
    assert pagina.locator("#login-button").inner_text() == "Sign in"
    assert pagina.title() == "Network auditing console access · AuditNet"
    assert pagina.locator("body").is_visible(), "la página no queda oculta después de traducir"
    assert pagina.locator(".language-switch [data-idioma=en]").get_attribute("aria-pressed") == "true"

    pagina.click(".language-switch [data-idioma=pt]")
    pagina.wait_for_function("document.documentElement.lang === 'pt'")
    assert pagina.locator("label:has(#password) span").inner_text() == "senha"
    assert pagina.evaluate("localStorage.getItem('auditnet.idioma')") == "pt"
    assert not pagina.errores


def test_las_pantallas_de_acceso_no_muestran_claves(abrir):
    for idioma in ("en", "pt"):
        for ruta in ("/login", "/recover", "/reset-password?token=x", "/reset-password"):
            pagina = abrir(ruta, idioma=idioma)
            pagina.wait_for_load_state("networkidle")
            _sin_claves(pagina)
            assert not pagina.errores


def test_al_ingresar_se_usa_el_idioma_de_la_cuenta(abrir, entorno):
    pagina = abrir("/login")
    pagina.fill("#identity", "ana")
    pagina.fill("#password", "Clave correcta 2026")
    pagina.click("#login-button")
    pagina.wait_for_url("**/dashboard**")
    pagina.wait_for_selector("#protected-shell:not(.is-hidden)")

    assert _pedidos(entorno, "POST", "/api/auth/login")[-1]["idioma"] == "es"
    assert pagina.evaluate("document.documentElement.lang") == "pt"
    assert pagina.locator(".rail-link[href='/results']").get_attribute("aria-label") == "Achados"
    assert pagina.locator("#logout-button").inner_text() == "Sair"
    assert pagina.locator("#user-role").inner_text() == "Analista de redes"
    assert _pedidos(entorno, "GET", "/api/auth/me")[-1]["idioma"] == "pt", "la API recibe el idioma"
    assert not pagina.errores


def test_el_menu_del_usuario_cambia_el_idioma_y_lo_guarda(abrir, entorno):
    pagina = abrir("/dashboard", perfil="ANALISTA_RED")
    assert pagina.locator(".rail-link[href='/results']").get_attribute("aria-label") == "Hallazgos"

    pagina.click("#user-menu-button")
    pagina.click("#user-popover [data-idioma=en]")
    pagina.wait_for_function("document.documentElement.lang === 'en'")
    pagina.wait_for_selector("#protected-shell:not(.is-hidden)")

    assert _pedidos(entorno, "PUT", "/api/auth/me/language")[-1]["cuerpo"] == {"idioma": "en"}
    assert pagina.locator(".rail-link[href='/results']").get_attribute("aria-label") == "Findings"
    assert pagina.locator("#command-palette").get_attribute("placeholder") == "go to module..."
    assert pagina.locator("#logout-button").inner_text() == "Sign out"
    assert not pagina.errores


def test_mi_cuenta_traducida_con_el_panel_de_idioma(abrir, entorno):
    pagina = abrir("/account", perfil="ANALISTA_RED", idioma="en")
    assert pagina.locator("h1").inner_text() == "My account"
    assert pagina.title() == "My account · AuditNet"
    assert pagina.locator(".console-breadcrumb code").inner_text() == "my account"
    assert pagina.locator("#account-language [data-idioma=en]").get_attribute("aria-pressed") == "true"
    assert pagina.locator("#account-rules li").first.inner_text() == "between 10 and 72 characters"
    _sin_claves(pagina)

    pagina.click("#account-submit")
    assert pagina.locator("#account-error").inner_text() == "Enter your current password."

    pagina.click("#account-language [data-idioma=pt]")
    pagina.wait_for_function("document.querySelector('h1').textContent === 'Minha conta'")
    assert _pedidos(entorno, "PUT", "/api/auth/me/language")[-1]["cuerpo"] == {"idioma": "pt"}
    assert not pagina.errores


def test_en_espanol_no_cambia_nada(abrir):
    pagina = abrir("/account", perfil="AUDITOR_TECNICO")
    assert pagina.evaluate("document.documentElement.lang") == "es"
    assert pagina.locator("h1").inner_text() == "Mi cuenta"
    assert pagina.locator("#account-language [data-idioma=es]").get_attribute("aria-pressed") == "true"
    assert pagina.locator("#notifications-none").inner_text().startswith("Tu perfil no recibe avisos")


# Todas las pantallas, con cada perfil, en inglés y en portugués.
@pytest.mark.parametrize("idioma", ["en", "pt"])
@pytest.mark.parametrize("perfil,ruta", CASOS)
def test_pantalla_traducida_sin_claves_ni_restos_en_espanol(abrir, perfil, ruta, idioma):
    pagina = abrir(ruta, perfil=perfil, idioma=idioma)
    pagina.wait_for_load_state("networkidle")
    assert pagina.evaluate("document.documentElement.lang") == idioma
    _sin_claves(pagina)
    restos = sorted({m.group(0) for m in ESPANOL.finditer(_texto_y_atributos(pagina))})
    assert not restos, f"textos en español: {restos}"
    assert not pagina.errores


@pytest.mark.parametrize("perfil,ruta", CASOS)
def test_en_portugues_entra_en_un_celular(abrir, perfil, ruta):
    pagina = abrir(ruta, perfil=perfil, tamanio=CELULAR, idioma="pt")
    pagina.wait_for_load_state("networkidle")
    assert pagina.evaluate("document.documentElement.scrollWidth") <= CELULAR["width"]


def test_los_textos_que_arman_los_scripts_salen_traducidos(abrir):
    pagina = abrir("/results", perfil="ANALISTA_RED", idioma="en")
    pagina.wait_for_selector("#finding-table-body tr")
    assert pagina.locator("#finding-table-body .status-locked:has-text('overdue')").count() > 0
    assert pagina.locator("#finding-table-body .severity-badge").first.inner_text() in ("CRITICAL", "HIGH", "MEDIUM", "LOW")
    pagina.wait_for_load_state("networkidle")
    pagina.click("#finding-table-body tr[data-finding-id='4']")
    pagina.wait_for_function("document.querySelector('#finding-inspector-content').textContent.includes('overdue by')")
    assert "Update status" in pagina.locator("#finding-inspector-content").inner_text()

    tablero = abrir("/dashboard", perfil="RESPONSABLE_GESTION_IT", idioma="pt")
    tablero.wait_for_selector("#dash-kpis a[href='/results?estado=VENCIDOS']")
    assert "achados vencidos" in tablero.locator("#dash-kpis").text_content()


def test_el_reporte_pdf_sale_en_el_idioma_de_la_consola(abrir):
    pagina = abrir("/reports?reporte=1", perfil="AUDITOR_TECNICO", idioma="en")
    pagina.wait_for_selector(".rpt")
    documento = pagina.locator(".rpt").inner_text()
    assert "Executive summary" in documento
    assert "This report covers 2 audits on 1 device" in documento
    assert "Forzar SSH versión 2" in documento, "los datos cargados no se traducen"
    estilo = pagina.evaluate("document.querySelector('#report-print-style').textContent")
    assert '"Page "' in estilo

    portugues = abrir("/reports?reporte=1", perfil="AUDITOR_TECNICO", idioma="pt")
    portugues.wait_for_selector(".rpt")
    assert "Resumo executivo" in portugues.locator(".rpt").inner_text()


def test_el_calendario_y_el_paginador_se_traducen(abrir):
    pagina = abrir("/logs", perfil="ADMINISTRADOR_SISTEMA", idioma="pt")
    pagina.wait_for_load_state("networkidle")
    pagina.locator(".dp-trigger").first.click()
    assert "seg" in pagina.locator(".dp-popover").first.text_content()
    assert pagina.locator(".dp-popover [data-action=today]").first.inner_text() == "hoje"
