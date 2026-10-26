"""Tema claro u oscuro: elección en el acceso, en el menú del usuario y en Mi cuenta, guardada en la cuenta."""

import pytest

from test_pantallas import CASOS

# Elementos visibles con fondo claro fuera del documento del reporte (que siempre es una hoja clara).
FONDOS_CLAROS = """() => Array.from(document.querySelectorAll('body *')).filter((n) => {
    if (n.closest('.report-document') || !n.getClientRects().length) return false;
    const [r, g, b, a = 1] = (getComputedStyle(n).backgroundColor.match(/[\\d.]+/g) || []).map(Number);
    return a > 0.5 && Math.min(r, g, b) > 200;
}).map((n) => n.className || n.tagName)"""


def _tema(pagina):
    return pagina.evaluate("document.documentElement.dataset.tema")


def _guardados(entorno):
    return [r["cuerpo"] for r in entorno["registro"] if r["metodo"] == "PUT" and r["ruta"] == "/api/auth/me/theme"]


def _ultimo_guardado(pagina, entorno, antes):
    """Espera el PUT del tema (se envía sin bloquear la pantalla)."""
    for _ in range(100):
        if len(_guardados(entorno)) > antes:
            return _guardados(entorno)[-1]
        pagina.wait_for_timeout(50)
    return None


def _ingresar(pagina, usuario):
    pagina.fill("#identity", usuario)
    pagina.fill("#password", "Clave correcta 2026")
    pagina.click("#login-button")
    pagina.wait_for_url("**/dashboard**")
    pagina.wait_for_selector("#protected-shell:not(.is-hidden)")


def test_en_el_acceso_se_elige_el_tema_y_queda_en_la_cuenta(abrir, entorno):
    pagina = abrir("/login")
    assert pagina.locator(".public-switches [data-opcion-tema]").count() == 2
    pagina.click(".public-switches [data-opcion-tema=oscuro]")
    assert _tema(pagina) == "oscuro"
    assert pagina.evaluate("localStorage.getItem('auditnet.tema')") == "oscuro"

    antes = len(_guardados(entorno))
    _ingresar(pagina, "ana")
    assert _guardados(entorno)[antes:] == [{"tema": "oscuro"}], "la cuenta no tenía tema: se guarda el elegido"
    assert _tema(pagina) == "oscuro"
    assert not pagina.errores


def test_al_ingresar_manda_el_tema_de_la_cuenta(abrir, entorno):
    pagina = abrir("/login")
    pagina.click(".public-switches [data-opcion-tema=claro]")
    antes = len(_guardados(entorno))
    _ingresar(pagina, "beto")
    assert _tema(pagina) == "oscuro"
    assert pagina.evaluate("localStorage.getItem('auditnet.tema')") == "oscuro"
    assert len(_guardados(entorno)) == antes, "la cuenta ya tenía tema: no se pisa"
    assert pagina.locator("#user-popover [data-opcion-tema=oscuro]").get_attribute("aria-pressed") == "true"


def test_sin_eleccion_sigue_al_sistema(abrir):
    assert _tema(abrir("/login")) == "claro"
    oscura = abrir("/login", sistema_oscuro=True)
    assert _tema(oscura) == "oscuro"
    assert oscura.evaluate("getComputedStyle(document.body).backgroundColor") != "rgb(236, 239, 241)"


def test_el_menu_del_usuario_cambia_el_tema_y_lo_guarda(abrir, entorno):
    pagina = abrir("/account", perfil="ANALISTA_RED", sistema_oscuro=True)
    assert _tema(pagina) == "oscuro"
    assert pagina.locator("#user-popover [data-opcion-tema]").count() == 2
    assert pagina.locator("#user-popover [data-opcion-tema=oscuro]").get_attribute("aria-pressed") == "true"

    antes = len(_guardados(entorno))
    pagina.click("#user-menu-button")
    pagina.click("#user-popover [data-opcion-tema=claro]")
    assert _tema(pagina) == "claro"
    assert pagina.evaluate("localStorage.getItem('auditnet.tema')") == "claro"
    assert _ultimo_guardado(pagina, entorno, antes) == {"tema": "claro"}
    assert pagina.locator("#account-theme [data-opcion-tema=claro]").get_attribute("aria-pressed") == "true"

    pagina.reload()
    pagina.wait_for_selector("#protected-shell:not(.is-hidden)")
    assert _tema(pagina) == "claro", "se recuerda aunque el sistema esté en oscuro"

    pagina.click("#account-theme [data-opcion-tema=oscuro]")
    assert _tema(pagina) == "oscuro"
    assert pagina.evaluate("localStorage.getItem('auditnet.tema')") == "oscuro"
    assert pagina.locator("#user-popover [data-opcion-tema=oscuro]").get_attribute("aria-pressed") == "true"
    assert not pagina.errores


def test_los_textos_del_tema_se_traducen(abrir):
    pagina = abrir("/account", perfil="ANALISTA_RED", idioma="en")
    assert pagina.locator("#account-theme [data-opcion-tema=oscuro]").inner_text() == "Dark"
    assert pagina.locator("#user-popover [data-opcion-tema=claro]").text_content() == "Light"


def test_el_reporte_se_ve_como_hoja_clara_en_tema_oscuro(abrir):
    pagina = abrir("/reports?reporte=1", perfil="AUDITOR_TECNICO", sistema_oscuro=True)
    pagina.wait_for_selector(".rpt")
    assert pagina.evaluate("getComputedStyle(document.querySelector('.report-document')).backgroundColor") == "rgb(255, 255, 255)"
    assert pagina.evaluate("getComputedStyle(document.querySelector('.rpt-title')).color") == "rgb(17, 20, 24)"
    ancho = pagina.evaluate("[document.querySelector('.rpt-section > p').offsetWidth, "
                            "document.querySelector('.rpt-section').offsetWidth]")
    assert ancho[0] == ancho[1], "el resumen ocupa todo el ancho del documento"

    pagina.emulate_media(media="print")
    assert pagina.evaluate("getComputedStyle(document.documentElement).colorScheme") == "light", "el PDF sale en blanco"


@pytest.mark.parametrize("perfil,ruta", CASOS)
def test_en_tema_oscuro_no_quedan_fondos_blancos(abrir, perfil, ruta):
    pagina = abrir(ruta, perfil=perfil, sistema_oscuro=True)
    pagina.wait_for_load_state("networkidle")
    assert _tema(pagina) == "oscuro"
    assert pagina.evaluate(FONDOS_CLAROS) == []
    assert not pagina.errores
