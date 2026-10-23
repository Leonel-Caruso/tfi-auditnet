"""Servidor para las pruebas de pantalla: sirve el sitio compilado (frontend/web-app/dist) y simula la API.

El token de sesión es el nombre del perfil (por ejemplo "Bearer ANALISTA_RED"), así cada prueba elige con qué perfil
entra sin pasar por el login real. Fuera del administrador del sistema, todos los perfiles son de la organización 2
(ACME). Las solicitudes a /api quedan registradas, con su cuerpo, para poder verificarlas.
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
        if codigo == 204:
            self.send_response(204)
            self.end_headers()
            return
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
        self.perfil = perfil
        self.registro.append({"metodo": metodo, "ruta": url.path, "consulta": consulta, "cuerpo": cuerpo,
                              "idioma": self.headers.get("Accept-Language")})

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
    if estado == "VENCIDOS":
        lista = datos.vencidos()
    else:
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


def _propias(coleccion, campo):
    """El administrador del sistema ve todo; el resto, solo lo de su organización (la 2)."""
    def responder(manejador, _m, _c, _b):
        manejador._json(200, [e for e in coleccion
                              if manejador.perfil == "ADMINISTRADOR_SISTEMA" or e[campo] == 2])
    return responder


def _creado(armar):
    return lambda manejador, coincidencia, _c, cuerpo: manejador._json(201, armar(coincidencia, cuerpo or {}))


def _por_id(coleccion, armar=lambda elemento: elemento):
    def responder(manejador, coincidencia, _c, _b):
        elemento = next((e for e in coleccion if e["id"] == int(coincidencia[1])), None)
        if elemento is None:
            return manejador._json(404, {"message": "No existe"})
        manejador._json(200, armar(elemento))
    return responder


def _ingresar(manejador, _m, _c, cuerpo):
    """Solo existe la cuenta "ana" (analista), que eligió portugués."""
    if cuerpo.get("identidad") != "ana" or cuerpo.get("password") != "Clave correcta 2026":
        return manejador._json(401, {"message": "Usuario o contraseña incorrectos."})
    manejador._json(200, {"accessToken": "ANALISTA_RED", "tokenType": "Bearer", "expiresIn": 3600, "userId": 1,
                          "organizationId": 2, "nombre": "Ana", "username": "ana", "email": "ana@acme.test",
                          "roles": ["ANALISTA_RED"], "idioma": "pt"})


def _idioma(manejador, _m, _c, cuerpo):
    if cuerpo.get("idioma") not in ("es", "en", "pt"):
        return manejador._json(400, {"message": "El idioma debe ser es, en o pt."})
    manejador._json(200, cuerpo)


def _restablecer(manejador, _m, _c, cuerpo):
    if cuerpo.get("token") != "enlace-valido":
        return manejador._json(400, {"message": "El enlace no es válido o ya venció."})
    manejador._json(204, None)


RUTAS = [
    (r"/api/auth/login", {"POST"}, _ingresar),
    (r"/api/auth/me/language", {"PUT"}, _idioma),
    (r"/api/auth/password-recovery", {"POST"},
     lambda h, m, c, b: h._json(202, {"mensaje": "Si los datos corresponden a una cuenta activa, en unos minutos vas "
                                                  "a recibir un correo con el enlace."})),
    (r"/api/auth/password-reset", {"POST"}, _restablecer),
    (r"/api/auth/me/notifications", {"GET"},
     _lista({"alertas": True, "vencimientos": False, "resumen": True})),
    (r"/api/auth/me/notifications", {"PUT"}, lambda h, m, c, b: h._json(200, b)),
    (r"/api/client-organizations/(\d+)/review-policy", {"GET"}, _lista(datos.POLITICA_REVISION)),
    (r"/api/client-organizations/(\d+)/review-policy", {"PUT"},
     lambda h, m, c, b: h._json(200, b | {"modificadaEn": "2026-10-22T12:00:00Z", "modificadaPor": "operador"})),
    (r"/api/review/status", {"GET"}, _lista(datos.estado_revision)),
    (r"/api/review/run", {"POST"}, _lista(datos.RESULTADO_REVISION)),
    (r"/api/admin/mail", {"GET"}, _lista(datos.BANDEJA_CORREO)),
    (r"/api/admin/mail/dispatch", {"POST"},
     _lista({"proveedor": "Mailpit", "enviados": 1, "reintentos": 0, "fallidos": 0, "pendientes": 0})),
    (r"/api/dashboard/summary", {"GET"}, _lista(datos.resumen_tablero)),
    (r"/api/client-organizations", {"GET"}, _propias(datos.ORGANIZACIONES, "id")),
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
    (r"/api/reports/(\d+)", {"GET"}, lambda h, m, c, b: h._json(200, datos.reporte(int(m[1])))),
    (r"/api/users", {"GET"}, _propias(datos.USUARIOS, "organizationId")),
    (r"/api/plans", {"GET"}, _lista(datos.PLANES)),
    (r"/api/plans/(\w+)", {"PUT"},
     lambda h, m, c, b: h._json(200, next(p for p in datos.PLANES if p["codigo"] == m[1]) | b)),
    (r"/api/client-organizations/(\d+)/plan", {"GET"},
     lambda h, m, c, b: h._json(200, datos.plan_organizacion(int(m[1])))),
    (r"/api/client-organizations/(\d+)/plan", {"PUT"},
     lambda h, m, c, b: h._json(200, datos.plan_organizacion(int(m[1])) | {
         "plan": next(p for p in datos.PLANES if p["codigo"] == b["plan"])})),
    (r"/api/client-organizations/(\d+)/plan-requests", {"GET"}, _lista(datos.SOLICITUDES)),
    (r"/api/client-organizations/(\d+)/plan-requests", {"POST"},
     _creado(lambda m, b: datos.SOLICITUDES[1] | {"id": 3, "planSolicitado": b["plan"], "modalidad": b["modalidad"]})),
    (r"/api/client-organizations/(\d+)/plan-requests/(\d+)/cancel", {"POST"},
     lambda h, m, c, b: h._json(200, datos.SOLICITUDES[1] | {"estado": "CANCELADA"})),
    (r"/api/admin/plan-requests", {"GET"},
     lambda h, m, c, b: h._json(200, [s for s in datos.SOLICITUDES if not c.get("estado") or s["estado"] == c["estado"]])),
    (r"/api/admin/plan-requests/(\d+)/(approve|reject)", {"POST"},
     lambda h, m, c, b: h._json(200, datos.SOLICITUDES[1] | {"estado": "APROBADA" if m[2] == "approve" else "RECHAZADA"})),
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
