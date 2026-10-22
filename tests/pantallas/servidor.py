"""Servidor para las pruebas de pantalla: sirve el sitio compilado (frontend/web-app/dist) y simula la API.

El token de sesión es el nombre del perfil (por ejemplo "Bearer ANALISTA_RED"), así cada prueba elige con qué perfil
entra sin pasar por el login real. Las solicitudes a /api quedan registradas para poder verificarlas.
"""

import json
import re
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread
from urllib.parse import parse_qs, urlparse

import datos


def _pagina(lista, consulta):
    """Aplica pagina/porPagina como la API real y devuelve (elementos, total)."""
    total = len(lista)
    if "pagina" not in consulta and "porPagina" not in consulta:
        return lista, total
    pagina = int(consulta.get("pagina", 1))
    por_pagina = int(consulta.get("porPagina", 25))
    return lista[(pagina - 1) * por_pagina:pagina * por_pagina], total


def _detalle_auditoria(auditoria):
    return {
        "auditoria": auditoria,
        "evaluaciones": [{"id": 1, "auditoriaId": auditoria["id"], "reglaId": 1, "codigoRegla": "SEC-SSH-01",
                          "nombreRegla": "Forzar SSH versión 2", "tipo": "DEBE_CONTENER", "patron": "ip ssh version 2",
                          "severidad": "ALTA", "cumple": True, "evidencia": "encontrado"}],
        "hallazgos": [h for h in datos.HALLAZGOS if h["auditoriaId"] == auditoria["id"]][:3],
        "configuracion": datos.CONFIGURACIONES[0] | {"version": 1},
        "baseline": {"id": auditoria["baselineId"], "nombre": "Hardening", "version": 2, "estado": "ACTIVO"},
    }


def _comparacion(auditoria):
    anterior = next((a for a in datos.AUDITORIAS if a["id"] == auditoria["id"] - 1), None)
    return {"actual": auditoria, "anterior": anterior,
            "cambios": [{"codigoRegla": "SEC-SSH-01", "nombreRegla": "SSH", "severidad": "ALTA", "cumpleAntes": False,
                         "cumpleAhora": True, "categoria": "CORREGIDO"}] if anterior else [],
            "resumen": {"CORREGIDO": 1} if anterior else {}}


class ManejadorAuditNet(SimpleHTTPRequestHandler):
    sitio = None
    registro = None

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(self.sitio), **kwargs)

    def log_message(self, *args):
        pass

    def do_GET(self):
        if self.path.startswith("/api/"):
            return self._api("GET")
        ruta = urlparse(self.path).path
        # Igual que nginx: /dashboard sirve /dashboard/index.html sin redirigir.
        if not Path(ruta).suffix and not ruta.endswith("/"):
            ruta += "/"
        self.path = ruta
        return super().do_GET()

    def do_POST(self):
        self._api("POST")

    def do_PUT(self):
        self._api("PUT")

    def do_PATCH(self):
        self._api("PATCH")

    def _json(self, codigo, cuerpo, total=None):
        datos_json = json.dumps(cuerpo).encode()
        self.send_response(codigo)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(datos_json)))
        if total is not None:
            self.send_header("X-Total-Count", str(total))
        self.end_headers()
        self.wfile.write(datos_json)

    def _api(self, metodo):
        url = urlparse(self.path)
        consulta = {clave: valores[0] for clave, valores in parse_qs(url.query).items()}
        largo = int(self.headers.get("Content-Length") or 0)
        cuerpo = json.loads(self.rfile.read(largo)) if largo else None
        perfil = (self.headers.get("Authorization") or "").removeprefix("Bearer ").strip()
        self.registro.append({"metodo": metodo, "ruta": url.path, "consulta": consulta})

        if url.path == "/api/auth/me":
            if perfil not in datos.ROLES:
                return self._json(401, {"message": "Sesión inválida"})
            return self._json(200, {"username": "operador", "subject": "1", "organizationId": 2, "roles": [perfil]})

        for patron, metodos, respuesta in RUTAS:
            coincidencia = re.fullmatch(patron, url.path)
            if coincidencia and metodo in metodos:
                return respuesta(self, coincidencia, consulta, cuerpo)
        return self._json(404, {"message": "Sin simulación para " + url.path})


def _lista(contenido):
    return lambda manejador, _m, _c, _b: manejador._json(200, contenido() if callable(contenido) else contenido)


def _hallazgos(manejador, _m, consulta, _b):
    estado = consulta.get("estado")
    lista = [h for h in datos.HALLAZGOS
             if not estado or h["estado"] == estado
             or (estado == "PENDIENTES" and h["estado"] in ("ABIERTO", "EN_REVISION"))]
    pagina, total = _pagina(lista, consulta)
    manejador._json(200, pagina, total)


def _historial(manejador, _m, consulta, _b):
    resultado = consulta.get("resultado")
    lista = [a for a in datos.AUDITORIAS if not resultado or a["resultado"] == resultado]
    pagina, total = _pagina(lista, consulta)
    manejador._json(200, pagina, total)


def _por_id(coleccion, armar=lambda elemento: elemento):
    def responder(manejador, coincidencia, _c, _b):
        elemento = next((e for e in coleccion if e["id"] == int(coincidencia[1])), None)
        if elemento is None:
            return manejador._json(404, {"message": "No existe"})
        manejador._json(200, armar(elemento))
    return responder


RUTAS = [
    (r"/api/dashboard/summary", {"GET"}, _lista(datos.resumen_tablero)),
    (r"/api/client-organizations", {"GET"}, _lista(datos.ORGANIZACIONES)),
    (r"/api/client-organizations/(\d+)", {"GET"}, _por_id(datos.ORGANIZACIONES)),
    (r"/api/client-organizations/(\d+)/sites", {"GET"},
     lambda h, m, c, b: h._json(200, datos.SEDES.get(int(m[1]), []))),
    (r"/api/client-organizations/(\d+)/audit-status", {"GET"},
     lambda h, m, c, b: h._json(200, datos.estado_cliente(int(m[1])))),
    (r"/api/device-types", {"GET"}, _lista(datos.TIPOS_DISPOSITIVO)),
    (r"/api/devices", {"GET"}, _lista(datos.DISPOSITIVOS)),
    (r"/api/baselines", {"GET"}, _lista(datos.BASELINES)),
    (r"/api/baselines/(\d+)/rules", {"GET"},
     lambda h, m, c, b: h._json(200, [r for r in datos.REGLAS if r["baselineId"] == int(m[1])])),
    (r"/api/rules", {"GET"}, _lista(datos.REGLAS)),
    (r"/api/configurations/auditable", {"GET"}, _lista(datos.CONFIGURACIONES)),
    (r"/api/configurations/(\d+)", {"GET"}, _por_id(datos.CONFIGURACIONES)),
    (r"/api/audits/history", {"GET"}, _historial),
    (r"/api/audits/(\d+)", {"GET"}, _por_id(datos.AUDITORIAS, _detalle_auditoria)),
    (r"/api/audits/(\d+)/comparison", {"GET"}, _por_id(datos.AUDITORIAS, _comparacion)),
    (r"/api/findings", {"GET"}, _hallazgos),
    (r"/api/findings/(\d+)", {"GET"}, _por_id(datos.HALLAZGOS)),
    (r"/api/findings/(\d+)/history", {"GET"}, lambda h, m, c, b: h._json(200, [])),
    (r"/api/alerts/critical", {"GET"}, _lista(datos.ALERTAS)),
    (r"/api/alerts/(\d+)", {"GET"}, _por_id(datos.ALERTAS, lambda a: {"alerta": a, "hallazgo": datos.HALLAZGOS[0]})),
    (r"/api/reports", {"GET"}, _lista(datos.REPORTES)),
    (r"/api/users", {"GET"}, _lista(datos.USUARIOS)),
    (r"/api/roles", {"GET"}, _lista(datos.PERFILES)),
    (r"/api/roles/permissions", {"GET"}, _lista(datos.PERMISOS)),
    (r"/api/admin/health", {"GET"}, _lista(lambda: datos.salud("management-service"))),
    (r"/api/system/health", {"GET"}, _lista(lambda: datos.salud("audit-core-service"))),
    (r"/api/admin/security", {"GET"}, _lista(datos.SEGURIDAD)),
    (r"/api/system/storage", {"GET"}, _lista(datos.ALMACENAMIENTO)),
    (r"/api/system/backups", {"GET"}, _lista([])),
    (r"/api/admin/bitacoras/sistema", {"GET"}, _lista(datos.BITACORA_SISTEMA)),
    (r"/api/admin/bitacoras/(transacciones|excepciones)", {"GET"}, _lista([])),
]


def iniciar(sitio: Path):
    """Arranca el servidor en un puerto libre y devuelve (url_base, registro_de_solicitudes, servidor)."""
    registro = []
    manejador = type("Manejador", (ManejadorAuditNet,), {"sitio": sitio, "registro": registro})
    servidor = ThreadingHTTPServer(("127.0.0.1", 0), manejador)
    Thread(target=servidor.serve_forever, daemon=True).start()
    return f"http://127.0.0.1:{servidor.server_address[1]}", registro, servidor
