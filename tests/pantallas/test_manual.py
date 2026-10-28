"""Descarga del manual de uso desde el tablero y el menú del usuario, por perfil e idioma."""

import json
import urllib.request

import pytest

from conftest import ESCRITORIO, SITIO

PERFILES = {
    "ADMINISTRADOR_SISTEMA": "administrador-sistema",
    "ADMINISTRADOR_ORGANIZACION": "administrador-organizacion",
    "ANALISTA_RED": "analista-redes",
    "AUDITOR_TECNICO": "auditor-tecnico",
    "RESPONSABLE_GESTION_IT": "responsable-gestion-it",
}


def _enlaces(pagina, selector):
    return pagina.locator(selector).evaluate_all("(nodos) => nodos.map((n) => n.getAttribute('href'))")


@pytest.mark.parametrize("perfil", PERFILES)
def test_el_tablero_ofrece_el_manual_del_perfil(abrir, perfil):
    pagina = abrir("/dashboard", perfil=perfil)
    pagina.wait_for_selector("#dash-manual:not([hidden]) a")

    assert _enlaces(pagina, "#dash-manual-links a") == [f"/manuales/manual-{PERFILES[perfil]}-es.pdf"]
    assert "Descargar manual" in pagina.locator("#dash-manual-links a").inner_text()
    assert not pagina.errores


@pytest.mark.parametrize("idioma", ["es", "en", "pt"])
def test_el_manual_sale_en_el_idioma_de_la_cuenta(abrir, idioma):
    pagina = abrir("/dashboard", perfil="AUDITOR_TECNICO", idioma=idioma)
    pagina.wait_for_selector("#dash-manual:not([hidden]) a")

    assert _enlaces(pagina, "#dash-manual-links a") == [f"/manuales/manual-auditor-tecnico-{idioma}.pdf"]
    assert not pagina.errores


def test_los_quince_manuales_estan_publicados(entorno):
    for nombre in PERFILES.values():
        for idioma in ("es", "en", "pt"):
            assert (SITIO / "manuales" / f"manual-{nombre}-{idioma}.pdf").exists(), f"{nombre}-{idioma}"
    with urllib.request.urlopen(entorno["url"] + "/manuales/manual-analista-redes-pt.pdf") as respuesta:
        assert respuesta.read(5) == b"%PDF-"


def test_ocultar_el_manual_queda_recordado_y_sigue_en_el_menu(abrir):
    pagina = abrir("/dashboard", perfil="ANALISTA_RED")
    pagina.wait_for_selector("#dash-manual:not([hidden]) a")
    pagina.click("#dash-manual-hide")
    assert pagina.locator("#dash-manual").is_hidden()

    pagina.reload()
    pagina.wait_for_selector("#protected-shell:not(.is-hidden)")
    pagina.wait_for_load_state("networkidle")
    assert pagina.locator("#dash-manual").is_hidden()

    pagina.click("#user-menu-button")
    assert _enlaces(pagina, "#user-manuals a") == ["/manuales/manual-analista-redes-es.pdf"]
    assert "Manual · Analista de redes" in pagina.locator("#user-manuals").inner_text()
    assert not pagina.errores


def test_cambiar_el_idioma_cambia_el_manual(abrir):
    pagina = abrir("/dashboard", perfil="RESPONSABLE_GESTION_IT")
    pagina.wait_for_selector("#dash-manual:not([hidden]) a")
    pagina.click("#user-menu-button")
    pagina.click("#user-popover [data-idioma=en]")
    pagina.wait_for_selector("#dash-manual:not([hidden]) a[href$='-en.pdf']")

    assert _enlaces(pagina, "#dash-manual-links a") == ["/manuales/manual-responsable-gestion-it-en.pdf"]
    assert "Download guide" in pagina.locator("#dash-manual-links a").inner_text()
    assert not pagina.errores


def test_un_usuario_con_dos_perfiles_tiene_un_manual_por_perfil(entorno, navegador):
    contexto = navegador.new_context(viewport=ESCRITORIO)
    usuario = json.dumps({"userId": 2, "nombre": "Operador", "username": "operador", "email": "operador@acme.test",
                          "organizationId": 2, "roles": ["ANALISTA_RED", "ADMINISTRADOR_ORGANIZACION"]})
    contexto.add_init_script(f"""
        sessionStorage.setItem('tfi.auth.token', 'ADMINISTRADOR_ORGANIZACION');
        sessionStorage.setItem('tfi.auth.expiresAt', String(Date.now() + 3600e3));
        sessionStorage.setItem('tfi.auth.user', JSON.stringify({usuario}));
    """)
    pagina = contexto.new_page()
    pagina.goto(entorno["url"] + "/dashboard")
    pagina.wait_for_selector("#dash-manual:not([hidden]) a")

    assert _enlaces(pagina, "#dash-manual-links a") == [
        "/manuales/manual-administrador-organizacion-es.pdf", "/manuales/manual-analista-redes-es.pdf"]
    pagina.click("#user-menu-button")
    assert pagina.locator("#user-manuals a").count() == 2
    contexto.close()
