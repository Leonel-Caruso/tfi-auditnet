"""Datos de las capturas de los manuales: una organización ficticia con nombres realistas en lugar de los datos de las
pruebas de pantalla (que incluyen HTML para probar el escapado). Reemplazan los de tests/pantallas/datos.py antes de
arrancar el servidor simulado."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts" / "datos-demo"))
import catalogo  # noqa: E402

ADMIN, ORG, ANALISTA, AUDITOR, RESPONSABLE = ("ADMINISTRADOR_SISTEMA", "ADMINISTRADOR_ORGANIZACION", "ANALISTA_RED",
                                              "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT")

# Usuario de la sesión de cada perfil en las capturas.
SESION = {
    ADMIN: {"userId": 1, "nombre": "Administrador AuditNet", "username": "admin", "email": "admin@auditnet.test",
            "organizationId": 1},
    ORG: {"userId": 2, "nombre": "Mariana García", "username": "mgarcia.litoral", "email": "mgarcia@litoral.coop",
          "organizationId": 2},
    ANALISTA: {"userId": 3, "nombre": "Julián Peralta", "username": "jperalta.litoral",
               "email": "jperalta@litoral.coop", "organizationId": 2},
    AUDITOR: {"userId": 4, "nombre": "Carlos Benítez", "username": "cbenitez.litoral",
              "email": "cbenitez@litoral.coop", "organizationId": 2},
    RESPONSABLE: {"userId": 5, "nombre": "Laura Molina", "username": "lmolina.litoral",
                  "email": "lmolina@litoral.coop", "organizationId": 2},
}

ORGANIZACIONES = [
    {"id": 2, "identificador": "COOP-LITORAL", "razonSocial": "Cooperativa Eléctrica del Litoral", "estado": "ACTIVO"},
    {"id": 1, "identificador": "AUDITNET", "razonSocial": "AuditNet", "estado": "ACTIVO"},
    {"id": 3, "identificador": "SALUD-AUSTRAL", "razonSocial": "Red Sanitaria Austral", "estado": "ACTIVO"},
    {"id": 4, "identificador": "AGRO-PAMPA", "razonSocial": "Agroindustrias La Pampa S.A.", "estado": "DESHABILITADO"},
]
SEDES = {
    1: [],
    2: [{"id": 11, "organizationId": 2, "nombre": "Casa central Rosario", "ubicacion": "Córdoba 1450, Rosario",
         "estado": "ACTIVO"},
        {"id": 12, "organizationId": 2, "nombre": "Sucursal Santa Fe", "ubicacion": "San Martín 2230, Santa Fe",
         "estado": "ACTIVO"},
        {"id": 13, "organizationId": 2, "nombre": "Estación transformadora Reconquista",
         "ubicacion": "Ruta 11 km 790, Reconquista", "estado": "ACTIVO"},
        {"id": 14, "organizationId": 2, "nombre": "Depósito Funes", "ubicacion": "Av. Santa Fe 300, Funes",
         "estado": "DESHABILITADO"}],
    3: [{"id": 21, "organizationId": 3, "nombre": "Hospital central Bahía Blanca", "ubicacion": "Estomba 968",
         "estado": "ACTIVO"}],
    4: [],
}
TIPOS_DISPOSITIVO = [
    {"id": 1, "nombre": "Router ISR 4331", "fabricante": "Cisco", "familia": "IOS-XE", "activo": True},
    {"id": 2, "nombre": "Switch Catalyst 9200", "fabricante": "Cisco", "familia": "IOS-XE", "activo": True},
    {"id": 3, "nombre": "Switch Catalyst 2960-X", "fabricante": "Cisco", "familia": "IOS", "activo": True},
]


def _equipo(numero, identificador, nombre, tipo, sede, criticidad, estado="ACTIVO"):
    return {"id": numero, "nombre": nombre, "identificador": identificador, "tipoDispositivoId": tipo,
            "fabricante": "Cisco", "organizacionId": 2, "sedeId": sede, "criticidad": criticidad, "estado": estado}


DISPOSITIVOS = [
    _equipo(1, "ROS-RTR-01", "Router principal ROS", 1, 11, "CRITICA"),
    _equipo(2, "ROS-SW-CORE-01", "Switch de núcleo ROS", 2, 11, "CRITICA"),
    _equipo(3, "ROS-SW-ACC-01", "Switch de acceso 1 ROS", 2, 11, "MEDIA"),
    _equipo(4, "SFE-RTR-01", "Router principal SFE", 1, 12, "ALTA"),
    _equipo(5, "SFE-SW-ACC-01", "Switch de acceso 1 SFE", 2, 12, "MEDIA"),
    _equipo(6, "RQT-RTR-01", "Router principal RQT", 1, 13, "CRITICA"),
    _equipo(7, "RQT-SW-OT-01", "Switch de red operativa RQT", 2, 13, "ALTA"),
    _equipo(8, "ROS-SW-ACC-02", "Switch de acceso 2 ROS", 2, 11, "BAJA", "INACTIVO"),
]
CLASE = {1: "router", 2: "switch", 3: "switch"}

BASELINES = [
    {"id": 1, "nombre": "Hardening de routers", "descripcion": catalogo.BASELINES["router"]["descripcion"],
     "version": 1, "tipoDispositivoId": 1, "organizacionId": 2, "estado": "INACTIVO", "auditorias": 6},
    {"id": 2, "nombre": "Hardening de routers", "descripcion": catalogo.BASELINES["router"]["descripcion"],
     "version": 2, "tipoDispositivoId": 1, "organizacionId": 2, "estado": "ACTIVO", "auditorias": 12},
    {"id": 3, "nombre": "Hardening de switches", "descripcion": catalogo.BASELINES["switch"]["descripcion"],
     "version": 1, "tipoDispositivoId": 2, "organizacionId": 2, "estado": "ACTIVO", "auditorias": 10},
]
BASELINE_DE = {"router": 2, "switch": 3}


def _reglas():
    reglas, numero = [], 0
    for clase, baseline in BASELINE_DE.items():
        for clave in catalogo.BASELINES[clase]["controles"]:
            numero += 1
            c = catalogo.CONTROLES[clave]
            reglas.append({"id": numero, "baselineId": baseline, "codigo": c["codigo"], "nombre": c["nombre"],
                           "descripcion": c["descripcion"], "tipo": c["tipo"], "patron": c["patron"],
                           "valorEsperado": c.get("valor"), "severidad": c["severidad"], "impacto": c["impacto"],
                           "recomendacion": c["recomendacion"], "estado": "ACTIVA"})
    return reglas


REGLAS = _reglas()
CONTROL_DE_CODIGO = {c["codigo"]: clave for clave, c in catalogo.CONTROLES.items()}

# Controles que todavía no cumple cada equipo en su última configuración.
PENDIENTES = {1: ["sin_telnet", "snmp", "timeout"], 2: ["bpdu", "ntp"], 3: ["vtp", "dhcp", "sin_cdp"],
              4: ["ssh2", "syslog"], 5: ["dhcp"], 6: ["sin_telnet", "secret", "aaa", "banner"], 7: ["bpdu", "snmp"]}


def _cumplidos(equipo):
    clase = CLASE[equipo["tipoDispositivoId"]]
    return set(catalogo.BASELINES[clase]["controles"]) - set(PENDIENTES.get(equipo["id"], []))


CONFIGURACIONES = []
for _e in DISPOSITIVOS[:7]:
    _texto = catalogo.configuracion(_e["identificador"], CLASE[_e["tipoDispositivoId"]], "litoral.coop",
                                    _cumplidos(_e), 40)
    CONFIGURACIONES.append({"id": _e["id"], "dispositivoId": _e["id"], "organizacionId": 2, "version": 3,
                            "formato": "CFG", "nombreFuente": f"{_e['identificador'].lower()}-v3.cfg",
                            "contenidoOriginal": _texto, "contenidoNormalizado": _texto,
                            "fechaImportacion": "2026-10-%02dT13:%02d:00Z" % (8 + _e["id"], 10 + _e["id"]),
                            "usuarioResponsable": "jperalta.litoral"})


def _auditoria(numero, equipo, dia, pendientes, origen="MANUAL"):
    clase = CLASE[equipo["tipoDispositivoId"]]
    total = len(catalogo.BASELINES[clase]["controles"])
    severidades = [catalogo.CONTROLES[c]["severidad"] for c in pendientes]
    orden = ["BAJA", "MEDIA", "ALTA", "CRITICA"]
    maxima = max(severidades, key=orden.index) if severidades else "NINGUNA"
    return {"id": numero, "configuracionId": equipo["id"], "dispositivoId": equipo["id"],
            "baselineId": BASELINE_DE[clase], "organizacionId": 2, "fechaEjecucion": "2026-%s" % dia,
            "ejecutadoPor": "jperalta.litoral" if origen == "MANUAL" else "SISTEMA", "totalReglas": total,
            "reglasCumplidas": total - len(pendientes), "totalHallazgos": len(pendientes), "severidadMaxima": maxima,
            "resultado": "CUMPLE" if not pendientes else "CON_HALLAZGOS", "estado": "FINALIZADA", "origen": origen}


def _auditorias():
    lista, numero = [], 42
    # La última auditoría de cada equipo (las más recientes primero) y una anterior con más desvíos.
    for indice, equipo in enumerate(DISPOSITIVOS[:7]):
        pendientes = PENDIENTES[equipo["id"]]
        lista.append(_auditoria(numero, equipo, "10-%02dT14:%02d:00Z" % (20 - indice, 5 * indice), pendientes,
                                "AUTOMATICA" if indice == 1 else "MANUAL"))
        numero -= 1
    for indice, equipo in enumerate(DISPOSITIVOS[:7]):
        anteriores = PENDIENTES[equipo["id"]] + [c for c in catalogo.BASELINES[CLASE[equipo["tipoDispositivoId"]]][
            "controles"] if c not in PENDIENTES[equipo["id"]]][:2]
        lista.append(_auditoria(numero, equipo, "09-%02dT15:%02d:00Z" % (28 - 2 * indice, 3 * indice), anteriores))
        numero -= 1
    return lista


AUDITORIAS = _auditorias()

_EVIDENCIA = {
    "DEBE_CONTENER": lambda c: f"No se encontró el patrón requerido: {c['patron']}",
    "NO_DEBE_CONTENER": lambda c: f"Patrón no permitido detectado: {c['patron']} ssh",
}


def _evidencia(control, equipo):
    if control["tipo"] != "VALOR_ESPERADO":
        return _EVIDENCIA[control["tipo"]](control)
    valores = {"ssh2": "1", "timeout": "0 0, 30 0", "vtp": "server"}
    clave = CONTROL_DE_CODIGO[control["codigo"]]
    return f"Parámetro {control['patron']} con valor {valores[clave]}; se esperaba: {control['valor']}."


def _hallazgos():
    lista, numero = [], 0
    estados = ["ABIERTO", "EN_REVISION", "ABIERTO", "ABIERTO", "EN_REVISION", "ACEPTADO", "RESUELTO"]
    plazos = {"CRITICA": 7, "ALTA": 15, "MEDIA": 30, "BAJA": 60}
    for auditoria in AUDITORIAS[:7]:
        equipo = next(d for d in DISPOSITIVOS if d["id"] == auditoria["dispositivoId"])
        for clave in PENDIENTES[equipo["id"]]:
            numero += 1
            control = catalogo.CONTROLES[clave]
            regla = next(r for r in REGLAS if r["codigo"] == control["codigo"]
                         and r["baselineId"] == auditoria["baselineId"])
            estado = estados[numero % len(estados)]
            dia = int(auditoria["fechaEjecucion"][8:10])
            vence = dia + plazos[control["severidad"]]
            lista.append({
                "id": numero, "auditoriaId": auditoria["id"], "reglaId": regla["id"], "dispositivoId": equipo["id"],
                "baselineId": auditoria["baselineId"], "organizacionId": 2, "codigoRegla": control["codigo"],
                "nombreRegla": control["nombre"], "tipoRegla": control["tipo"],
                "patron": control["patron"] + (f" = {control['valor']}" if control.get("valor") else ""),
                "severidad": control["severidad"], "evidencia": _evidencia(control, equipo),
                "recomendacion": control["recomendacion"], "impacto": control["impacto"], "estado": estado,
                "fechaDeteccion": auditoria["fechaEjecucion"],
                "fechaEstado": None if estado == "ABIERTO" else "2026-10-21T16:00:00Z",
                "usuarioEstado": None if estado == "ABIERTO" else "jperalta.litoral",
                "criticidadDispositivo": equipo["criticidad"],
                "venceEn": None if estado in ("RESUELTO", "ACEPTADO") else (
                    "2026-10-%02dT14:00:00Z" % vence if vence <= 31 else "2026-11-%02dT14:00:00Z" % (vence - 31))})
    return lista


HALLAZGOS = _hallazgos()


def _alertas():
    lista = []
    for hallazgo in HALLAZGOS:
        if hallazgo["severidad"] not in ("CRITICA", "ALTA"):
            continue
        equipo = next(d for d in DISPOSITIVOS if d["id"] == hallazgo["dispositivoId"])
        estado = {"ABIERTO": "ACTIVA", "EN_REVISION": "EN_ATENCION"}.get(hallazgo["estado"], "CERRADA")
        lista.append({"id": hallazgo["id"], "hallazgoId": hallazgo["id"], "auditoriaId": hallazgo["auditoriaId"],
                      "dispositivoId": equipo["id"], "organizacionId": 2, "severidad": hallazgo["severidad"],
                      "estado": estado, "fechaGeneracion": hallazgo["fechaDeteccion"],
                      "perfilAsignado": ANALISTA if estado == "EN_ATENCION" else None,
                      "comentario": "Derivada a redes para corregir en el equipo." if estado == "EN_ATENCION" else None,
                      "fechaEstado": hallazgo["fechaEstado"], "usuarioEstado": hallazgo["usuarioEstado"],
                      "dispositivoIdentificador": equipo["identificador"], "dispositivoNombre": equipo["nombre"],
                      "sedeId": equipo["sedeId"], "criticidadDispositivo": equipo["criticidad"],
                      "codigoRegla": hallazgo["codigoRegla"], "nombreRegla": hallazgo["nombreRegla"],
                      "estadoHallazgo": hallazgo["estado"]})
    return lista


ALERTAS = _alertas()

REPORTES = [
    {"id": 3, "organizacionId": 2, "titulo": "Reporte de auditorías del 01/10/2026 al 20/10/2026",
     "totalAuditorias": 7, "totalHallazgos": len(HALLAZGOS), "generadoPor": "cbenitez.litoral",
     "fechaGeneracion": "2026-10-21T12:30:00Z"},
    {"id": 2, "organizacionId": 2, "titulo": "Reporte de auditorías del 01/09/2026 al 30/09/2026",
     "totalAuditorias": 7, "totalHallazgos": 27, "generadoPor": "cbenitez.litoral",
     "fechaGeneracion": "2026-10-01T11:00:00Z"},
    {"id": 1, "organizacionId": 2, "titulo": "Reporte de auditoría AUD-042", "totalAuditorias": 1,
     "totalHallazgos": 3, "generadoPor": "jperalta.litoral", "fechaGeneracion": "2026-10-20T15:10:00Z"},
]

# El título de un reporte se arma en el idioma de quien lo genera; en cada manual, en el idioma del manual.
TITULOS_REPORTES = {
    "es": ["Reporte de auditorías del 01/10/2026 al 20/10/2026", "Reporte de auditorías del 01/09/2026 al 30/09/2026",
           "Reporte de auditoría AUD-042"],
    "en": ["Audit report from 10/01/2026 to 10/20/2026", "Audit report from 09/01/2026 to 09/30/2026",
           "Audit report AUD-042"],
    "pt": ["Relatório de auditorias de 01/10/2026 a 20/10/2026", "Relatório de auditorias de 01/09/2026 a 30/09/2026",
           "Relatório de auditoria AUD-042"],
}


def titular_reportes(idioma):
    for reporte_guardado, titulo in zip(REPORTES, TITULOS_REPORTES[idioma]):
        reporte_guardado["titulo"] = titulo


def reporte(id_reporte):
    base = next((r for r in REPORTES if r["id"] == id_reporte), REPORTES[0])
    incluidas = AUDITORIAS[:7]
    hallazgos = [{"id": h["id"], "auditoriaId": h["auditoriaId"], "dispositivoId": h["dispositivoId"],
                  "dispositivoIdentificador": next(d["identificador"] for d in DISPOSITIVOS
                                                   if d["id"] == h["dispositivoId"]),
                  "reglaId": h["reglaId"], "codigoRegla": h["codigoRegla"], "nombreRegla": h["nombreRegla"],
                  "severidad": h["severidad"], "impacto": h["impacto"], "evidencia": h["evidencia"],
                  "recomendacion": h["recomendacion"], "estado": h["estado"], "fechaDeteccion": h["fechaDeteccion"]}
                 for h in HALLAZGOS]
    por_severidad, por_estado = {}, {}
    for h in hallazgos:
        por_severidad[h["severidad"]] = por_severidad.get(h["severidad"], 0) + 1
        por_estado[h["estado"]] = por_estado.get(h["estado"], 0) + 1
    evaluadas = sum(a["totalReglas"] for a in incluidas)
    cumplidas = sum(a["reglasCumplidas"] for a in incluidas)
    pendientes = sum(1 for h in hallazgos if h["estado"] in ("ABIERTO", "EN_REVISION"))
    return base | {
        "criterios": {"organizacionId": 2, "desde": "2026-10-01T03:00:00Z", "hasta": "2026-10-21T02:59:59Z",
                      "dispositivoIds": [], "severidades": [], "estados": [], "auditoriaIds": []},
        "contenido": {
            "resumen": {"auditorias": len(incluidas), "dispositivos": len(incluidas), "reglasEvaluadas": evaluadas,
                        "reglasCumplidas": cumplidas, "cumplimiento": round(cumplidas * 100 / evaluadas),
                        "hallazgos": len(hallazgos), "hallazgosPendientes": pendientes,
                        "hallazgosPorSeveridad": por_severidad, "hallazgosPorEstado": por_estado,
                        "severidadMaxima": "CRITICA", "resultadoGeneral": "CON_DESVIOS"},
            "auditorias": [{"id": a["id"], "fechaEjecucion": a["fechaEjecucion"], "ejecutadoPor": a["ejecutadoPor"],
                            "dispositivoId": a["dispositivoId"],
                            "dispositivoIdentificador": next(d["identificador"] for d in DISPOSITIVOS
                                                             if d["id"] == a["dispositivoId"]),
                            "dispositivoNombre": next(d["nombre"] for d in DISPOSITIVOS
                                                      if d["id"] == a["dispositivoId"]),
                            "criticidadDispositivo": next(d["criticidad"] for d in DISPOSITIVOS
                                                          if d["id"] == a["dispositivoId"]),
                            "sedeId": next(d["sedeId"] for d in DISPOSITIVOS if d["id"] == a["dispositivoId"]),
                            "configuracionId": a["configuracionId"], "configuracionVersion": 3,
                            "baselineId": a["baselineId"],
                            "baselineNombre": next(b["nombre"] for b in BASELINES if b["id"] == a["baselineId"]),
                            "baselineVersion": next(b["version"] for b in BASELINES if b["id"] == a["baselineId"]),
                            "totalReglas": a["totalReglas"], "reglasCumplidas": a["reglasCumplidas"],
                            "totalHallazgos": a["totalHallazgos"], "severidadMaxima": a["severidadMaxima"],
                            "resultado": a["resultado"]} for a in incluidas],
            "hallazgos": hallazgos,
            "inconsistencias": []}}


def _riesgo(equipo):
    propios = [h for h in HALLAZGOS if h["dispositivoId"] == equipo["id"] and h["estado"] in ("ABIERTO", "EN_REVISION")]
    criticos = sum(1 for h in propios if h["severidad"] == "CRITICA")
    altos = sum(1 for h in propios if h["severidad"] == "ALTA")
    nivel = "ALTO" if criticos else ("MEDIO" if altos else "BAJO")
    ultima = next(a["fechaEjecucion"] for a in AUDITORIAS if a["dispositivoId"] == equipo["id"])
    return {"dispositivoId": equipo["id"], "identificador": equipo["identificador"], "nombre": equipo["nombre"],
            "sedeId": equipo["sedeId"], "criticidad": equipo["criticidad"], "nivel": nivel, "pendientes": len(propios),
            "criticosPendientes": criticos, "altosPendientes": altos,
            "alertasVigentes": sum(1 for a in ALERTAS if a["dispositivoId"] == equipo["id"] and a["estado"] != "CERRADA"),
            "ultimaAuditoria": ultima}


def resumen_tablero():
    pendientes = [h for h in HALLAZGOS if h["estado"] in ("ABIERTO", "EN_REVISION")]
    cerrados = [h for h in HALLAZGOS if h["estado"] in ("RESUELTO", "ACEPTADO")]
    por = lambda lista: {s: sum(1 for h in lista if h["severidad"] == s) for s in ("BAJA", "MEDIA", "ALTA", "CRITICA")}
    riesgos = sorted((_riesgo(e) for e in DISPOSITIVOS[:7]), key=lambda r: ("ALTO", "MEDIO", "BAJO").index(r["nivel"]))
    vigentes = [a for a in ALERTAS if a["estado"] != "CERRADA"]
    return {
        "generado": "2026-10-22T15:00:00Z", "nivelRiesgo": "ALTO",
        "dispositivos": {"registrados": 8, "activos": 7, "auditados": 7, "activosSinAuditar": 0,
                         "porCriticidad": {"BAJA": 0, "MEDIA": 2, "ALTA": 2, "CRITICA": 3}},
        "hallazgos": {"pendientes": len(pendientes), "enRevision": sum(1 for h in pendientes if h["estado"] == "EN_REVISION"),
                      "cerrados": len(cerrados) + 21, "pendientesPorSeveridad": por(pendientes),
                      "cerradosPorSeveridad": por(cerrados)},
        "alertas": {"vigentes": len(vigentes), "activas": sum(1 for a in vigentes if a["estado"] == "ACTIVA"),
                    "enAtencion": sum(1 for a in vigentes if a["estado"] == "EN_ATENCION"),
                    "criticasVigentes": sum(1 for a in vigentes if a["severidad"] == "CRITICA")},
        "auditorias": {"total": len(AUDITORIAS) + 14, "ultimos30Dias": len(AUDITORIAS), "cumplimientoActual": 76},
        "auditoriasRecientes": AUDITORIAS[:5],
        "dispositivosEnRiesgo": [r for r in riesgos if r["nivel"] != "BAJO"],
        "evolucion": [{"semana": semana, "auditorias": a, "hallazgosDetectados": d, "hallazgosCerrados": c}
                      for semana, a, d, c in (("2026-08-31", 3, 9, 1), ("2026-09-07", 2, 6, 2), ("2026-09-14", 4, 8, 5),
                                              ("2026-09-21", 3, 5, 6), ("2026-09-28", 2, 3, 4), ("2026-10-05", 3, 4, 3),
                                              ("2026-10-12", 4, 6, 5), ("2026-10-19", 5, 7, 4))],
    }


def estado_cliente(organizacion):
    resumen = resumen_tablero()
    sedes = []
    for sede in SEDES[2][:3]:
        equipos = [e for e in DISPOSITIVOS[:7] if e["sedeId"] == sede["id"]]
        riesgos = [_riesgo(e) for e in equipos]
        sedes.append({"sedeId": sede["id"], "nivel": "ALTO" if any(r["nivel"] == "ALTO" for r in riesgos) else "MEDIO",
                      "dispositivos": len(equipos), "auditados": len(equipos),
                      "pendientes": sum(r["pendientes"] for r in riesgos),
                      "criticosPendientes": sum(r["criticosPendientes"] for r in riesgos),
                      "alertasVigentes": sum(r["alertasVigentes"] for r in riesgos)})
    return {"organizacionId": organizacion, "resumen": resumen, "reportes": REPORTES, "sedes": sedes,
            "dispositivos": [_riesgo(e) for e in DISPOSITIVOS[:7]]}


USUARIOS = [
    {"id": 1, "organizationId": 1, "nombre": "Administrador AuditNet", "email": "admin@auditnet.test",
     "nombreUsuario": "admin", "estado": "ACTIVO", "roles": [ADMIN], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-10-22T11:40:00Z"},
    {"id": 2, "organizationId": 2, "nombre": "Mariana García", "email": "mgarcia@litoral.coop",
     "nombreUsuario": "mgarcia.litoral", "estado": "ACTIVO", "roles": [ORG], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-10-22T09:15:00Z"},
    {"id": 3, "organizationId": 2, "nombre": "Julián Peralta", "email": "jperalta@litoral.coop",
     "nombreUsuario": "jperalta.litoral", "estado": "ACTIVO", "roles": [ANALISTA], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-10-22T10:02:00Z"},
    {"id": 4, "organizationId": 2, "nombre": "Carlos Benítez", "email": "cbenitez@litoral.coop",
     "nombreUsuario": "cbenitez.litoral", "estado": "ACTIVO", "roles": [AUDITOR], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-10-21T16:20:00Z"},
    {"id": 5, "organizationId": 2, "nombre": "Laura Molina", "email": "lmolina@litoral.coop",
     "nombreUsuario": "lmolina.litoral", "estado": "ACTIVO", "roles": [RESPONSABLE], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-10-20T08:45:00Z"},
    {"id": 6, "organizationId": 2, "nombre": "Sofía Ruiz", "email": "sruiz@litoral.coop",
     "nombreUsuario": "sruiz.litoral", "estado": "ACTIVO", "roles": [ANALISTA],
     "bloqueadoHasta": "2999-01-01T13:30:00Z", "ultimoAcceso": "2026-10-18T14:10:00Z"},
    {"id": 7, "organizationId": 2, "nombre": "Diego Herrera", "email": "dherrera@litoral.coop",
     "nombreUsuario": "dherrera.litoral", "estado": "DESHABILITADO", "roles": [AUDITOR], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-08-30T12:00:00Z"},
    {"id": 8, "organizationId": 3, "nombre": "Paula Vázquez", "email": "pvazquez@saludaustral.org",
     "nombreUsuario": "pvazquez.austral", "estado": "ACTIVO", "roles": [ORG], "bloqueadoHasta": None,
     "ultimoAcceso": "2026-10-19T10:00:00Z"},
]
_DESCRIPCIONES = {
    ADMIN: "Administra usuarios, organizaciones, sedes, bitácoras y la salud de la plataforma.",
    ORG: "Administra los usuarios, las sedes y los dispositivos de su organización y consulta su plan.",
    ANALISTA: "Importa configuraciones, ejecuta auditorías, atiende hallazgos y deriva alertas.",
    AUDITOR: "Define baselines y reglas, revisa hallazgos y genera reportes de auditoría.",
    RESPONSABLE: "Consulta el tablero, las alertas y los reportes de su organización y deriva alertas.",
}
PERFILES = [{"id": i + 1, "nombre": nombre, "descripcion": _DESCRIPCIONES[nombre], "estado": "ACTIVO"}
            for i, nombre in enumerate((ADMIN, ORG, ANALISTA, AUDITOR, RESPONSABLE))]
_TODOS = [ADMIN, ORG, ANALISTA, AUDITOR, RESPONSABLE]
PERMISOS = [{"modulo": m, "accion": a, "perfiles": p} for m, a, p in (
    ("Tablero y estado del cliente", "Consultar", _TODOS),
    ("Dispositivos", "Consultar", _TODOS),
    ("Dispositivos", "Registrar, modificar y deshabilitar", [ADMIN, ORG]),
    ("Tipos de dispositivo", "Registrar", [ADMIN]),
    ("Baselines y reglas", "Consultar", _TODOS),
    ("Baselines y reglas", "Crear, versionar y modificar", [ADMIN, AUDITOR]),
    ("Configuraciones", "Importar", [ADMIN, ANALISTA, AUDITOR]),
    ("Auditorías", "Ejecutar", [ADMIN, ANALISTA, AUDITOR]),
    ("Auditorías e historial", "Consultar", _TODOS),
    ("Hallazgos", "Consultar", _TODOS),
    ("Hallazgos", "Cambiar el estado de seguimiento", [ADMIN, ANALISTA, AUDITOR]),
    ("Alertas críticas", "Consultar", _TODOS),
    ("Alertas críticas", "Derivar o devolver", [ADMIN, ANALISTA, RESPONSABLE]),
    ("Reportes", "Consultar y descargar", _TODOS),
    ("Reportes", "Generar", [ADMIN, ANALISTA, AUDITOR]),
)]

SEGURIDAD = {"usuariosActivos": 7, "usuariosDeshabilitados": 1, "cuentasBloqueadas": 1,
             "eventosUltimas24h": {"LOGIN_EXITOSO": 14, "LOGIN_RECHAZADO": 3, "CUENTA_BLOQUEADA": 1},
             "ultimosRechazos": [], "desde": "2026-10-21T12:00:00Z"}
ALMACENAMIENTO = {"configurado": True, "contenedor": "auditnet-archivos",
                  "archivos": {"CONFIGURACION": {"cantidad": 21, "bytes": 52_300},
                               "REPORTE": {"cantidad": 3, "bytes": 18_400},
                               "RESPALDO": {"cantidad": 2, "bytes": 1_850_000}},
                  "configuracionesPendientes": 0, "reportesPendientes": 0}

_EVENTOS = [("LOGIN_EXITOSO", "EXITO", "jperalta.litoral"), ("LOGIN_EXITOSO", "EXITO", "cbenitez.litoral"),
            ("LOGIN_RECHAZADO", "FALLO", "sruiz.litoral"), ("CUENTA_BLOQUEADA", "FALLO", "sruiz.litoral"),
            ("USUARIO_MODIFICADO", "EXITO", "lmolina.litoral"), ("LOGIN_EXITOSO", "EXITO", "mgarcia.litoral"),
            ("CONTRASENA_CAMBIADA", "EXITO", "mgarcia.litoral"), ("LOGIN_EXITOSO", "EXITO", "admin")]
BITACORA_SISTEMA = [{"id": 60 - n, "fecha": "2026-10-22T%02d:%02d:00Z" % (14 - n // 6, (n * 7) % 60),
                     "evento": _EVENTOS[n % 8][0], "resultado": _EVENTOS[n % 8][1], "actor": _EVENTOS[n % 8][2],
                     "usuarioAfectadoId": None, "organizacionId": 2, "origenIp": "190.12.40.%d" % (20 + n % 9),
                     "userAgent": "Chrome 130 · Windows", "correlacion": "8f2c%04d" % (60 - n),
                     "detalle": None} for n in range(60)]

SOLICITUDES_LISTA = [
    {"id": 1, "organizationId": 2, "organizacion": "Cooperativa Eléctrica del Litoral", "planActual": "PILOTO",
     "planSolicitado": "INICIAL", "modalidad": "ANUAL", "estado": "APROBADA",
     "comentario": "Terminó la prueba y seguimos con la operación.", "solicitadoPor": "mgarcia.litoral",
     "fechaSolicitud": "2026-07-28T12:00:00Z", "resueltoPor": "admin", "fechaResolucion": "2026-07-29T10:00:00Z",
     "respuesta": "Aprobado con descuento anual."},
    {"id": 2, "organizationId": 2, "organizacion": "Cooperativa Eléctrica del Litoral", "planActual": "INICIAL",
     "planSolicitado": "PROFESIONAL", "modalidad": "ANUAL", "estado": "PENDIENTE",
     "comentario": "En noviembre sumamos dos estaciones transformadoras.", "solicitadoPor": "mgarcia.litoral",
     "fechaSolicitud": "2026-10-20T12:00:00Z", "resueltoPor": None, "fechaResolucion": None, "respuesta": None},
]


def plan_organizacion(organizacion):
    planes = {p["codigo"]: p for p in PLANES_LISTA}
    return {"organizationId": organizacion, "plan": planes["INICIAL"], "maxDispositivos": 50, "maxUsuarios": 15,
            "dispositivosActivos": 7, "usuariosActivos": 6, "desde": "2026-07-29T10:00:00Z", "vence": None,
            "diasRestantes": None, "vencido": False, "solicitudPendiente": SOLICITUDES_LISTA[1]}


PLANES_LISTA = []

RESULTADO_REVISION = {"ejecutada": "2026-10-22T09:00:00Z", "organizaciones": 3, "reauditorias": 2,
                      "hallazgosVencidos": 4, "dispositivosDesactualizados": 1, "correos": 5, "resumenes": 0,
                      "omitidas": []}

BANDEJA_CORREO = {
    "proveedor": "Azure Communication Services", "pendientes": 0, "enviados24h": 6, "fallidos24h": 0,
    "ultimos": [
        {"id": 12, "origen": "audit-core-service", "tipo": "ALERTAS_CRITICAS", "destinatario": "jperalta@litoral.coop",
         "estado": "ENVIADO", "intentos": 1, "creadoEn": "2026-10-22T09:01:00Z", "proximoIntento": "2026-10-22T09:01:00Z",
         "enviadoEn": "2026-10-22T09:01:03Z", "ultimoError": None},
        {"id": 11, "origen": "audit-core-service", "tipo": "VENCIMIENTOS", "destinatario": "lmolina@litoral.coop",
         "estado": "ENVIADO", "intentos": 1, "creadoEn": "2026-10-22T09:00:00Z", "proximoIntento": "2026-10-22T09:00:00Z",
         "enviadoEn": "2026-10-22T09:00:04Z", "ultimoError": None},
        {"id": 10, "origen": "management-service", "tipo": "SOLICITUD_PLAN", "destinatario": "admin@auditnet.test",
         "estado": "ENVIADO", "intentos": 1, "creadoEn": "2026-10-20T12:00:00Z", "proximoIntento": "2026-10-20T12:00:00Z",
         "enviadoEn": "2026-10-20T12:00:02Z", "ultimoError": None},
    ],
}


def aplicar(datos):
    """Reemplaza los datos del servidor simulado. Hay que llamarla antes de importar servidor."""
    PLANES_LISTA[:] = datos.PLANES
    datos.ORGANIZACIONES, datos.SEDES, datos.TIPOS_DISPOSITIVO = ORGANIZACIONES, SEDES, TIPOS_DISPOSITIVO
    datos.DISPOSITIVOS, datos.BASELINES, datos.REGLAS = DISPOSITIVOS, BASELINES, REGLAS
    datos.CONFIGURACIONES, datos.AUDITORIAS, datos.HALLAZGOS = CONFIGURACIONES, AUDITORIAS, HALLAZGOS
    datos.ALERTAS, datos.REPORTES, datos.reporte = ALERTAS, REPORTES, reporte
    datos.resumen_tablero, datos.estado_cliente = resumen_tablero, estado_cliente
    datos.USUARIOS, datos.PERFILES, datos.PERMISOS = USUARIOS, PERFILES, PERMISOS
    datos.SEGURIDAD, datos.ALMACENAMIENTO, datos.BITACORA_SISTEMA = SEGURIDAD, ALMACENAMIENTO, BITACORA_SISTEMA
    datos.SOLICITUDES, datos.plan_organizacion = SOLICITUDES_LISTA, plan_organizacion
    datos.RESULTADO_REVISION, datos.BANDEJA_CORREO = RESULTADO_REVISION, BANDEJA_CORREO
    datos.vencidos = lambda: [h for h in HALLAZGOS if h["venceEn"] and h["venceEn"] < "2026-10-22"]
