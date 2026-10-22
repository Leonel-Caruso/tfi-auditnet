"""Todas las pantallas, con cada perfil: cargan sin errores, no muestran textos técnicos y entran en un celular."""

import re

import pytest

from conftest import CELULAR

PANTALLAS = ["/dashboard", "/client-status", "/devices", "/baselines", "/rules", "/audits", "/history", "/results",
             "/alerts", "/reports", "/account"]
PERFILES = ["ADMINISTRADOR_SISTEMA", "ADMINISTRADOR_ORGANIZACION", "ANALISTA_RED", "AUDITOR_TECNICO",
            "RESPONSABLE_GESTION_IT"]
# Pantallas que no ven todos los perfiles: perfil → rutas propias.
PANTALLAS_POR_PERFIL = {
    "ADMINISTRADOR_SISTEMA": ["/organizations", "/users", "/plans", "/logs", "/admin"],
    "ADMINISTRADOR_ORGANIZACION": ["/organizations", "/users", "/plan"],
    "ANALISTA_RED": [],
    "AUDITOR_TECNICO": [],
    "RESPONSABLE_GESTION_IT": ["/plan"],
}
RESTRINGIDAS = sorted({ruta for rutas in PANTALLAS_POR_PERFIL.values() for ruta in rutas})

CASOS = [(perfil, ruta) for perfil in PERFILES for ruta in PANTALLAS + PANTALLAS_POR_PERFIL[perfil]]

# Rutas de API, etiquetas en inglés o en formato de código que no deben verse en pantalla (incluye diálogos ocultos).
TEXTO_TECNICO = re.compile(
    r"(GET|POST|PUT|PATCH) /api|select [a-z]+;|\b[a-z]+\.(count|id|by|at|preview|changed|to|type|pass|total|max)\b"
    r"|\b(identifier|display_name|device_type|rule_code|expected_value|recommendation|criticality|severity)\b"
    r"|CU-\d{3}|RF-\d{3}|RNF-\d{3}"
)


def _texto_completo(pagina):
    return pagina.locator("body").inner_text() + "\n" + pagina.evaluate("document.body.textContent")


@pytest.mark.parametrize("perfil,ruta", CASOS)
def test_pantalla_carga_sin_errores_ni_textos_tecnicos(abrir, perfil, ruta):
    pagina = abrir(ruta, perfil=perfil)
    pagina.wait_for_load_state("networkidle")
    tecnicos = sorted({m.group(0) for m in TEXTO_TECNICO.finditer(_texto_completo(pagina))})
    assert not tecnicos, f"textos técnicos visibles: {tecnicos}"
    assert not pagina.errores


@pytest.mark.parametrize("perfil,ruta", CASOS)
def test_pantalla_entra_en_un_celular(abrir, perfil, ruta):
    pagina = abrir(ruta, perfil=perfil, tamanio=CELULAR)
    pagina.wait_for_load_state("networkidle")
    assert pagina.evaluate("document.documentElement.scrollWidth") <= CELULAR["width"]


@pytest.mark.parametrize("perfil", PERFILES)
def test_el_menu_muestra_solo_las_pantallas_del_perfil(abrir, perfil):
    pagina = abrir("/dashboard", perfil=perfil)
    visibles = {ruta for ruta in RESTRINGIDAS if pagina.locator(f".rail-link[href='{ruta}']").is_visible()}
    assert visibles == set(PANTALLAS_POR_PERFIL[perfil])


def test_html_de_los_datos_se_muestra_como_texto(abrir):
    pagina = abrir("/users", perfil="ADMINISTRADOR_SISTEMA")
    pagina.wait_for_selector("text=<b>Beto</b>")
    assert pagina.locator("#users-body b").count() == 0
