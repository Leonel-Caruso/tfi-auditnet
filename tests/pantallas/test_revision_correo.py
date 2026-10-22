"""Recuperación de contraseña, avisos por correo, plazos de revisión y vencimientos."""


def _pedidos(entorno, metodo, ruta):
    return [r for r in entorno["registro"] if r["metodo"] == metodo and r["ruta"] == ruta]


def test_el_login_lleva_a_recuperar_la_contrasena(abrir, entorno):
    pagina = abrir("/login")
    pagina.click("text=¿Olvidaste tu contraseña?")
    pagina.wait_for_url("**/recover")

    pagina.click("#recover-submit")
    assert "Ingresá tu usuario o correo" in pagina.locator("#recover-error").inner_text()
    pagina.fill("#recover-identity", "ana")
    pagina.click("#recover-submit")
    pagina.wait_for_selector("#recover-done:not(.is-hidden)")

    assert "en unos minutos" in pagina.locator("#recover-done").inner_text()
    assert _pedidos(entorno, "POST", "/api/auth/password-recovery")[-1]["cuerpo"] == {"identidad": "ana"}
    assert not pagina.errores


def test_el_enlace_crea_la_contrasena_nueva_y_no_queda_en_la_barra(abrir, entorno):
    pagina = abrir("/reset-password?token=enlace-valido")
    assert pagina.url.endswith("/reset-password"), "el token no queda en la dirección ni en el historial"

    pagina.fill("#reset-password", "corta")
    pagina.click("#reset-submit")
    assert "no cumple la política" in pagina.locator("#reset-error").inner_text()

    pagina.fill("#reset-password", "Clave nueva 2026")
    pagina.fill("#reset-confirm", "Clave nueva 2025")
    pagina.click("#reset-submit")
    assert "no coinciden" in pagina.locator("#reset-error").inner_text()

    pagina.fill("#reset-confirm", "Clave nueva 2026")
    pagina.click("#reset-submit")
    pagina.wait_for_selector("#reset-done:not(.is-hidden)")
    assert _pedidos(entorno, "POST", "/api/auth/password-reset")[-1]["cuerpo"] == {
        "token": "enlace-valido", "password": "Clave nueva 2026"}
    assert pagina.locator("#reset-password").is_disabled()
    assert not pagina.errores


def test_un_enlace_vencido_o_incompleto_avisa(abrir):
    vencido = abrir("/reset-password?token=otro")
    vencido.fill("#reset-password", "Clave nueva 2026")
    vencido.fill("#reset-confirm", "Clave nueva 2026")
    vencido.click("#reset-submit")
    vencido.wait_for_selector("#reset-error:not(.is-hidden)")
    assert "ya venció" in vencido.locator("#reset-error").inner_text()

    incompleto = abrir("/reset-password")
    assert incompleto.locator("#reset-missing").is_visible()
    assert not incompleto.locator("#reset-submit").is_visible()


def test_cada_perfil_elige_solo_los_avisos_que_le_llegan(abrir, entorno):
    analista = abrir("/account", perfil="ANALISTA_RED")
    analista.wait_for_function("document.querySelector('#notifications-form input[name=alertas]').checked")
    assert analista.locator("[data-notice=alertas]").is_visible()
    assert analista.locator("[data-notice=vencimientos]").is_visible()
    assert not analista.locator("[data-notice=resumen]").is_visible(), "el resumen es para el responsable"
    assert not analista.locator("input[name=vencimientos]").is_checked()

    analista.check("input[name=vencimientos]")
    analista.click("#notifications-submit")
    analista.wait_for_selector("#account-message:not(.is-hidden)")
    assert _pedidos(entorno, "PUT", "/api/auth/me/notifications")[-1]["cuerpo"] == {
        "alertas": True, "vencimientos": True, "resumen": True}

    auditor = abrir("/account", perfil="AUDITOR_TECNICO")
    assert auditor.locator("#notifications-none").is_visible()
    assert not auditor.locator("#notifications-submit").is_visible()


def test_el_administrador_de_organizacion_ajusta_los_plazos(abrir, entorno):
    pagina = abrir("/organizations", perfil="ADMINISTRADOR_ORGANIZACION")
    pagina.wait_for_selector("#review-submit:not([disabled])")
    assert pagina.locator("#review-critica").input_value() == "7"
    assert pagina.locator("#review-updated").inner_text() == "valores por defecto"

    pagina.fill("#review-critica", "0")
    pagina.click("#review-submit")
    assert "entre 1 y 365" in pagina.locator("#review-error").inner_text()
    assert not _pedidos(entorno, "PUT", "/api/client-organizations/2/review-policy")

    pagina.fill("#review-critica", "5")
    pagina.uncheck("#review-auto")
    pagina.click("#review-submit")
    pagina.wait_for_selector("#organization-message:not(.is-hidden)")
    cuerpo = _pedidos(entorno, "PUT", "/api/client-organizations/2/review-policy")[-1]["cuerpo"]
    assert cuerpo == {"reauditoriaAutomatica": False, "diasCritica": 5, "diasAlta": 15, "diasMedia": 30,
                      "diasBaja": 60, "diasReauditoria": 30, "diasConfiguracion": 30}
    assert "operador" in pagina.locator("#review-updated").inner_text()
    assert not pagina.errores


def test_los_hallazgos_vencidos_se_marcan_y_se_filtran(abrir, entorno):
    pagina = abrir("/results", perfil="ANALISTA_RED")
    pagina.wait_for_selector("#finding-table-body tr")
    assert pagina.locator("#finding-table-body .status-locked:has-text('vencido')").count() > 0
    assert "vencidos" in pagina.locator("#metric-pending-note").inner_text()

    pagina.wait_for_load_state("networkidle")
    pagina.click("#finding-table-body tr[data-finding-id='4']")
    assert "vencido hace" in pagina.locator("#finding-inspector-content").inner_text()

    filtrado = abrir("/results?estado=VENCIDOS", perfil="ANALISTA_RED")
    filtrado.wait_for_selector("#finding-table-body tr")
    assert any(p["consulta"].get("estado") == "VENCIDOS" for p in _pedidos(entorno, "GET", "/api/findings"))
    filas = filtrado.locator("#finding-table-body tr").count()
    assert filas == filtrado.locator("#finding-table-body tr:has(.status-locked)").count()


def test_el_tablero_muestra_vencidos_y_configuraciones_viejas(abrir):
    pagina = abrir("/dashboard", perfil="RESPONSABLE_GESTION_IT")
    vencidos = pagina.locator("#dash-kpis a[href='/results?estado=VENCIDOS']")
    vencidos.wait_for()
    assert "hallazgos vencidos" in vencidos.inner_text().lower()
    assert "10" in vencidos.inner_text()
    assert "is-alert" in vencidos.get_attribute("class")
    assert "hace más de 30 días" in pagina.locator("#dash-kpis").inner_text()


def test_el_historial_distingue_las_auditorias_automaticas(abrir):
    pagina = abrir("/history", perfil="AUDITOR_TECNICO")
    pagina.wait_for_selector("#history-table-body tr, tbody tr")
    assert pagina.locator("tbody .status-badge:has-text('automática')").count() == 1


def test_el_panel_de_administracion_muestra_el_correo_y_ejecuta_la_revision(abrir, entorno):
    pagina = abrir("/admin", perfil="ADMINISTRADOR_SISTEMA")
    pagina.wait_for_selector("#mail-list tr")
    assert pagina.locator("#mail-provider").inner_text() == "Mailpit"
    assert "vencimientos" in pagina.locator("#mail-list").inner_text()
    assert "HTTP 503" in pagina.locator("#mail-list").inner_text()

    pagina.click("#mail-dispatch")
    pagina.wait_for_function("document.querySelector('#mail-note').textContent.startsWith('Enviados: 1')")
    assert _pedidos(entorno, "POST", "/api/admin/mail/dispatch")

    pagina.click("#review-run")
    pagina.wait_for_selector("#review-result:not(.is-hidden)")
    resultado = pagina.locator("#review-result").inner_text()
    assert "EDGE-02" in resultado
    assert _pedidos(entorno, "POST", "/api/review/run")
    assert not pagina.errores
