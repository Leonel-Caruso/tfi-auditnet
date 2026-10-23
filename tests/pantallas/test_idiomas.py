"""Idioma de la consola: elección en el acceso, guardado en la cuenta y textos traducidos."""

import re

# Una clave sin traducir se vería como "cuenta.titulo" o "menu.results".
CLAVE_SUELTA = re.compile(r"\b(idioma|perfil|error|contrasena|consola|menu|ruta|acceso|login|recuperar|restablecer|cuenta)"
                          r"\.[a-zA-Z-]+\b")


def _pedidos(entorno, metodo, ruta):
    return [r for r in entorno["registro"] if r["metodo"] == metodo and r["ruta"] == ruta]


def _sin_claves(pagina):
    marcados = pagina.eval_on_selector_all("[data-i18n]", "nodos => nodos.map(n => n.textContent)")
    texto = pagina.locator("body").inner_text() + " ".join(marcados)
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
