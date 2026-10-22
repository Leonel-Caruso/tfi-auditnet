"""Pantalla de acceso: textos, marca, metadatos para buscadores y app instalable."""

import json


def test_formulario_en_castellano(abrir):
    pagina = abrir("/login")
    assert pagina.locator("label:has(#identity) span").inner_text() == "usuario o correo"
    assert pagina.locator("label:has(#password) span").inner_text() == "contraseña"
    assert not pagina.errores


def test_logo_con_la_tipografia_del_icono(abrir):
    pagina = abrir("/login")
    pagina.wait_for_function("document.fonts.check('800 16px \"AuditNet Mono\"')")
    estilo = pagina.evaluate("(() => { const s = getComputedStyle(document.querySelector('#hero-mark'));"
                             " return {fuente: s.fontFamily, color: s.color}; })()")
    assert "AuditNet Mono" in estilo["fuente"]
    assert estilo["color"] == "rgb(255, 255, 255)"


def test_icono_blanco_sobre_naranja(abrir, entorno):
    pagina = abrir("/login")
    svg = pagina.request.get(entorno["url"] + "/favicon.svg").text()
    assert "#f57c00" in svg and 'fill="#fff"' in svg and "<text" not in svg


def test_login_indexable_y_consola_no(abrir):
    login = abrir("/login")
    assert login.locator("meta[name=robots]").get_attribute("content") == "index, follow"
    assert login.locator("meta[property='og:title']").count() == 1
    assert login.locator("link[rel=canonical]").get_attribute("href").endswith("/login")

    consola = abrir("/dashboard", perfil="ANALISTA_RED")
    assert consola.locator("meta[name=robots]").get_attribute("content") == "noindex, nofollow"


def test_manifiesto_de_app_instalable(abrir, entorno):
    pagina = abrir("/login")
    assert pagina.locator("link[rel=manifest]").get_attribute("href") == "/manifest.webmanifest"
    manifiesto = json.loads(pagina.request.get(entorno["url"] + "/manifest.webmanifest").text())
    assert manifiesto["display"] == "standalone"
    tamanios = {icono["sizes"] for icono in manifiesto["icons"]}
    assert {"192x192", "512x512"} <= tamanios
    assert any(icono.get("purpose") == "maskable" for icono in manifiesto["icons"])
    for icono in manifiesto["icons"]:
        assert pagina.request.get(entorno["url"] + icono["src"]).status == 200


def test_service_worker_registrado(abrir):
    pagina = abrir("/login")
    pagina.wait_for_function("navigator.serviceWorker.ready.then(() => true)")
    assert pagina.evaluate("navigator.serviceWorker.getRegistrations().then(r => r.length)") == 1


def test_sin_sesion_vuelve_al_login(abrir):
    pagina = abrir("/results")
    pagina.wait_for_url("**/login**")
