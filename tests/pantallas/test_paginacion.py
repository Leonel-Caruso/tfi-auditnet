"""Paginador de hallazgos (en el navegador), historial (en el servidor) y bitácoras (sobre lo cargado)."""


def test_hallazgos_de_a_25_con_metricas_sobre_el_total(abrir):
    pagina = abrir("/results", perfil="ANALISTA_RED")
    pagina.wait_for_selector("#finding-table-body tr")
    assert pagina.locator("#finding-table-body tr").count() == 25
    assert "1–25 de 63" in pagina.locator("#finding-pager").inner_text()
    assert int(pagina.locator("#metric-pending").inner_text()) == 42

    primero = pagina.locator("#finding-table-body tr").first.get_attribute("data-finding-id")
    pagina.click("#finding-pager [data-pager=siguiente]")
    assert "26–50 de 63" in pagina.locator("#finding-pager").inner_text()
    assert pagina.locator("#finding-table-body tr").first.get_attribute("data-finding-id") != primero


def test_hallazgos_cambiar_tamanio_y_buscar(abrir):
    pagina = abrir("/results", perfil="ANALISTA_RED")
    pagina.wait_for_selector("#finding-table-body tr")
    pagina.select_option("#finding-pager select", "10")
    assert pagina.locator("#finding-table-body tr").count() == 10
    pagina.click("#finding-pager [data-pager=siguiente]")
    pagina.fill("#finding-search", "R-")
    assert "Página 1 de" in pagina.locator("#finding-pager").inner_text()


def test_hallazgo_pedido_por_url_abre_su_pagina(abrir):
    pagina = abrir("/results?hallazgo=60", perfil="ANALISTA_RED")
    pagina.wait_for_selector("#finding-inspector-content:not(.is-hidden)")
    assert pagina.locator("#finding-table-body tr.is-selected").count() == 1


def test_paginador_conserva_el_foco_del_teclado(abrir):
    pagina = abrir("/results", perfil="ANALISTA_RED")
    pagina.wait_for_selector("#finding-table-body tr")
    pagina.focus("#finding-pager [data-pager=siguiente]")
    pagina.keyboard.press("Enter")
    assert pagina.evaluate("document.activeElement.dataset.pager") == "siguiente"
    assert "Página 2" in pagina.locator("#finding-pager .pager-announce").inner_text()


def test_historial_pide_cada_pagina_al_servidor(abrir, entorno):
    pagina = abrir("/history", perfil="AUDITOR_TECNICO")
    pagina.wait_for_selector("#history-body tr")
    assert pagina.locator("#history-body tr").count() == 25
    assert pagina.locator("#history-count").inner_text() == "42"

    pagina.click("#history-pager [data-pager=siguiente]")
    pagina.esperar("document.querySelectorAll('#history-body tr').length === 17")
    pedidos = [r["consulta"] for r in entorno["registro"] if r["ruta"] == "/api/audits/history"]
    assert pedidos[-1].get("pagina") == "2" and pedidos[-1].get("porPagina") == "25"


def test_bitacoras_paginan_lo_cargado(abrir):
    pagina = abrir("/logs#sistema", perfil="ADMINISTRADOR_SISTEMA")
    pagina.wait_for_selector("#log-body tr")
    assert pagina.locator("#log-body tr").count() == 25
    assert "1–25 de 60" in pagina.locator("#log-pager").inner_text()
    pagina.click("#log-pager [data-pager=siguiente]")
    pagina.locator("#log-body tr").first.click()
    assert "usuario26" in pagina.locator("#log-inspector-content").inner_text()
