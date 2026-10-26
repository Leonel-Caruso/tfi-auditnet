"""Administrador de organización, Mi plan, cambio de plan y solicitudes."""


def _pedidos(entorno, metodo, ruta):
    return [r for r in entorno["registro"] if r["metodo"] == metodo and r["ruta"] == ruta]


def test_el_administrador_de_organizacion_gestiona_solo_perfiles_de_operacion(abrir):
    pagina = abrir("/users", perfil="ADMINISTRADOR_ORGANIZACION")
    pagina.wait_for_selector("#users-body tr[data-user-id='4']")

    assert pagina.locator("#users-scope-note").is_visible()
    assert pagina.locator("#users-body tr").count() == 3, "Solo los usuarios de su organización"
    jefa = pagina.locator("#users-body tr[data-user-id='4']")
    assert "lo gestiona el administrador del sistema" in jefa.inner_text()
    assert jefa.locator("[data-action]").count() == 0
    assert pagina.locator("#users-body tr[data-user-id='2'] [data-action=edit]").count() == 1
    assert pagina.locator("#role-list [data-role-id]").count() == 0, "No edita descripciones de perfiles"

    pagina.click("#user-create")
    assert pagina.locator("#form-user-organization").is_disabled()
    perfiles = pagina.locator("#user-roles input[name=roles]").evaluate_all("(nodos) => nodos.map((n) => n.value)")
    assert perfiles == ["ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"]
    assert not pagina.errores


def test_el_administrador_de_organizacion_gestiona_sedes_y_dispositivos(abrir):
    organizaciones = abrir("/organizations", perfil="ADMINISTRADOR_ORGANIZACION")
    organizaciones.wait_for_selector("#plan-summary .usage-row")
    assert organizaciones.locator("#open-site-dialog").is_visible()
    assert not organizaciones.locator("#open-organization-dialog").is_visible()
    assert not organizaciones.locator("#edit-organization").is_visible()
    assert not organizaciones.locator("#change-plan").is_visible()
    assert organizaciones.locator("#site-list button:has-text('Editar')").count() == 1

    dispositivos = abrir("/devices", perfil="ADMINISTRADOR_ORGANIZACION")
    dispositivos.wait_for_selector("#devices-body tr")
    assert dispositivos.locator("#open-device-dialog").is_visible()
    assert not dispositivos.locator("#open-type-dialog").is_visible()
    dispositivos.click("#open-device-dialog")
    assert dispositivos.locator("#device-organization").input_value() == "2", "Su organización queda elegida"


def test_mi_plan_muestra_uso_vencimiento_y_solicitud_pendiente(abrir):
    pagina = abrir("/plan", perfil="ADMINISTRADOR_ORGANIZACION")
    pagina.wait_for_selector("#plan-current .usage-row")

    assert pagina.locator("#plan-name").inner_text() == "Piloto"
    assert "7 de 10" in pagina.locator("#plan-current").inner_text()
    assert pagina.locator("#plan-current .usage-row.is-full").count() == 1, "5 de 5 usuarios: en el límite"
    assert "12 días" in pagina.locator("#plan-expiry").inner_text()
    assert "Piloto → Empresa" in pagina.locator("#plan-pending").inner_text()
    assert pagina.locator("#request-open").is_disabled(), "Con una solicitud pendiente no se pide otra"
    assert pagina.locator("#plan-pending b").count() == 0
    assert "Falta el contrato" in pagina.locator("#plan-history").inner_text()
    assert pagina.locator("#plan-catalog .plan-card.is-current").count() == 1


def test_cancelar_y_pedir_un_plan(abrir, entorno):
    pagina = abrir("/plan", perfil="ADMINISTRADOR_ORGANIZACION")
    pagina.wait_for_selector("[data-cancel]")
    pagina.click("[data-cancel]")
    pagina.wait_for_selector("#plan-message:not(.is-hidden)")
    assert _pedidos(entorno, "POST", "/api/client-organizations/2/plan-requests/2/cancel")

    # La simulación sigue informando la solicitud pendiente: se espera a que la pantalla termine de recargar.
    pagina.wait_for_load_state("networkidle")
    pagina.evaluate("document.querySelector('#request-open').disabled = false")
    pagina.click("#request-open")
    opciones = pagina.locator("#request-plan option").evaluate_all("(nodos) => nodos.map((n) => n.value)")
    assert opciones == ["INICIAL", "PROFESIONAL", "EMPRESA"], "El piloto y el plan actual no se piden"
    pagina.select_option("#request-plan", "PROFESIONAL")
    assert "USD 300" in pagina.locator("#request-conditions").inner_text()
    pagina.check("input[name=modalidad][value=ANUAL]")
    pagina.fill("#request-comment", "Sumamos sedes")
    pagina.click("#request-form button[type=submit]")
    pagina.wait_for_selector("#request-dialog:not([open])", state="attached")

    pedido = _pedidos(entorno, "POST", "/api/client-organizations/2/plan-requests")[-1]
    assert pedido["cuerpo"] == {"plan": "PROFESIONAL", "modalidad": "ANUAL", "comentario": "Sumamos sedes"}


def test_el_responsable_ve_su_plan_pero_no_lo_pide(abrir):
    pagina = abrir("/plan", perfil="RESPONSABLE_GESTION_IT")
    pagina.wait_for_selector("#plan-current .usage-row")
    assert not pagina.locator("#request-open").is_visible()
    assert pagina.locator("[data-cancel]").count() == 0


def test_el_administrador_aprueba_un_plan_a_medida_con_sus_limites(abrir, entorno):
    pagina = abrir("/plans", perfil="ADMINISTRADOR_SISTEMA")
    pagina.wait_for_selector("#pending-list [data-action=approve]")
    assert pagina.locator("#pending-count").inner_text() == "1"
    assert "Falta el contrato" in pagina.locator("#history-list").inner_text()

    pagina.click("#pending-list [data-action=approve]")
    assert pagina.locator("#resolve-devices").is_visible()
    pagina.fill("#resolve-devices", "400")
    pagina.fill("#resolve-users", "120")
    pagina.click("#resolve-submit")
    pagina.wait_for_selector("#plans-message:not(.is-hidden)")

    pedido = _pedidos(entorno, "POST", "/api/admin/plan-requests/2/approve")[-1]
    assert pedido["cuerpo"] == {"respuesta": "", "limiteDispositivos": 400, "limiteUsuarios": 120}


def test_el_administrador_cambia_el_plan_desde_organizaciones(abrir, entorno):
    pagina = abrir("/organizations", perfil="ADMINISTRADOR_SISTEMA")
    pagina.wait_for_selector("#change-plan:not([disabled])")
    pagina.click("#change-plan")
    assert pagina.locator("#plan-trial-days").is_visible(), "Piloto: se eligen los días"
    assert not pagina.locator("#plan-limit-devices").is_visible()

    pagina.select_option("#plan-code", "EMPRESA")
    assert not pagina.locator("#plan-trial-days").is_visible()
    pagina.fill("#plan-limit-devices", "300")
    pagina.fill("#plan-limit-users", "80")
    pagina.click("#plan-form button[type=submit]")
    pagina.wait_for_selector("#organization-message:not(.is-hidden)")

    pedido = _pedidos(entorno, "PUT", "/api/client-organizations/2/plan")[-1]
    assert pedido["cuerpo"] == {"plan": "EMPRESA", "diasPrueba": None, "limiteDispositivos": 300,
                                "limiteUsuarios": 80}


def test_el_alta_de_organizacion_lleva_el_plan_inicial(abrir, entorno):
    pagina = abrir("/organizations", perfil="ADMINISTRADOR_SISTEMA")
    pagina.wait_for_selector("#organization-list .select-row")
    pagina.click("#open-organization-dialog")
    assert pagina.locator("#organization-plan").input_value() == "PILOTO"
    assert pagina.locator("#organization-trial-days").is_visible()

    pagina.keyboard.press("Escape")
    pagina.click("#edit-organization")
    assert not pagina.locator("#organization-plan").is_visible(), "Al editar, el plan se cambia aparte"


def test_el_panel_de_administracion_avisa_las_solicitudes_pendientes(abrir):
    pagina = abrir("/admin", perfil="ADMINISTRADOR_SISTEMA")
    pagina.esperar("!document.querySelector('#admin-plan-requests').textContent.includes('…')")
    assert "1 solicitud de cambio de plan por resolver" in pagina.locator("#admin-plan-requests").inner_text()
