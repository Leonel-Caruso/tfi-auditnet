"""Datos de ejemplo que devuelve el servidor simulado. Incluyen textos con HTML para comprobar que se escapan."""

ROLES = ["ADMINISTRADOR_SISTEMA", "ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"]
SEVERIDADES = ["CRITICA", "ALTA", "MEDIA", "BAJA"]

ORGANIZACIONES = [
    {"id": 2, "identificador": "ACME", "razonSocial": "ACME S.A.", "estado": "ACTIVO"},
    {"id": 3, "identificador": "BETA", "razonSocial": "Beta SRL", "estado": "ACTIVO"},
]
SEDES = {
    2: [{"id": 11, "organizationId": 2, "nombre": "Casa central", "ubicacion": "CABA", "estado": "ACTIVO"}],
    3: [],
}

TIPOS_DISPOSITIVO = [{"id": 1, "nombre": "Router ISR 4000", "fabricante": "Cisco", "familia": "IOS-XE", "activo": True}]

DISPOSITIVOS = [
    {"id": 1, "nombre": "Router Core", "identificador": "RTR-CORE-01", "tipoDispositivoId": 1, "fabricante": "Cisco",
     "organizacionId": 2, "sedeId": 11, "criticidad": "ALTA", "estado": "ACTIVO"},
    {"id": 2, "nombre": "Switch", "identificador": "SW-01", "tipoDispositivoId": 1, "fabricante": "Cisco",
     "organizacionId": 2, "sedeId": None, "criticidad": "BAJA", "estado": "ACTIVO"},
]

BASELINES = [
    {"id": 1, "nombre": "Hardening", "descripcion": "Versión anterior", "version": 1, "tipoDispositivoId": 1,
     "organizacionId": 2, "estado": "INACTIVO", "auditorias": 1},
    {"id": 2, "nombre": "Hardening", "descripcion": "Controles mínimos", "version": 2, "tipoDispositivoId": 1,
     "organizacionId": 2, "estado": "ACTIVO", "auditorias": 41},
]

REGLAS = [
    {"id": 1, "baselineId": 2, "codigo": "SEC-SSH-01", "nombre": "Forzar SSH versión 2", "descripcion": "Solo SSH v2.",
     "tipo": "DEBE_CONTENER", "patron": "ip ssh version 2", "valorEsperado": None, "severidad": "ALTA",
     "impacto": "Credenciales expuestas.", "recomendacion": "ip ssh version 2", "estado": "ACTIVA"},
    {"id": 2, "baselineId": 2, "codigo": "LOG-BUF-01", "nombre": "Buffer de logs", "descripcion": "16 KB.",
     "tipo": "VALOR_ESPERADO", "patron": "logging buffered", "valorEsperado": "16384", "severidad": "MEDIA",
     "impacto": "Sin evidencia local.", "recomendacion": "logging buffered 16384", "estado": "ACTIVA"},
]

CONFIGURACIONES = [
    {"id": 1, "dispositivoId": 1, "organizacionId": 2, "version": 1, "formato": "CFG", "nombreFuente": "rtr.cfg",
     "contenidoOriginal": "hostname RTR-CORE-01", "contenidoNormalizado": "hostname RTR-CORE-01",
     "fechaImportacion": "2026-10-02T14:00:00Z", "usuarioResponsable": "ana"},
]


def _auditoria(numero, fecha, baseline, hallazgos, severidad):
    return {"id": numero, "configuracionId": 1, "dispositivoId": 1, "baselineId": baseline, "organizacionId": 2,
            "fechaEjecucion": fecha, "ejecutadoPor": "admin", "totalReglas": 3, "reglasCumplidas": 3 - hallazgos,
            "totalHallazgos": hallazgos, "severidadMaxima": severidad,
            "resultado": "CUMPLE" if hallazgos == 0 else "CON_HALLAZGOS", "estado": "FINALIZADA"}


# 42 auditorías: alcanzan para dos páginas de historial.
AUDITORIAS = [_auditoria(n, "2026-09-%02dT15:00:00Z" % (1 + n % 28), 2, n % 3, "MEDIA" if n % 3 else "NINGUNA")
              for n in range(42, 0, -1)]


def _hallazgo(numero):
    return {"id": numero, "auditoriaId": 42, "reglaId": 1, "dispositivoId": 1 + numero % 2, "baselineId": 2,
            "organizacionId": 2, "codigoRegla": "R-%02d" % numero, "nombreRegla": "Regla %d" % numero,
            "tipoRegla": "DEBE_CONTENER", "patron": "p <b>x</b>", "severidad": SEVERIDADES[numero % 4],
            "evidencia": "No se encontró <script>alert(1)</script>", "recomendacion": "r", "impacto": "i",
            "estado": ["ABIERTO", "EN_REVISION", "RESUELTO"][numero % 3], "fechaDeteccion": "2026-10-02T15:00:00Z",
            "fechaEstado": None, "usuarioEstado": None, "criticidadDispositivo": "ALTA"}


# 63 hallazgos: tres páginas de 25.
HALLAZGOS = [_hallazgo(n) for n in range(1, 64)]


def _alerta(numero, estado):
    return {"id": numero, "hallazgoId": numero, "auditoriaId": 42, "dispositivoId": 1, "organizacionId": 2,
            "severidad": "CRITICA", "estado": estado, "fechaGeneracion": "2026-10-02T15:00:00Z", "perfilAsignado": None,
            "comentario": None, "fechaEstado": None, "usuarioEstado": None, "dispositivoIdentificador": "RTR-CORE-01",
            "dispositivoNombre": "Router Core", "sedeId": 11, "criticidadDispositivo": "ALTA",
            "codigoRegla": "R-%02d" % numero, "nombreRegla": "Regla", "estadoHallazgo": "ABIERTO"}


ALERTAS = [_alerta(4, "ACTIVA"), _alerta(8, "EN_ATENCION"), _alerta(12, "CERRADA")]

REPORTES = [{"id": 1, "organizacionId": 2, "titulo": "Reporte de septiembre", "totalAuditorias": 2,
             "totalHallazgos": 2, "generadoPor": "auditor", "fechaGeneracion": "2026-10-01T12:00:00Z"}]


def resumen_tablero():
    return {
        "generado": "2026-10-08T15:00:00Z", "nivelRiesgo": "ALTO",
        "dispositivos": {"registrados": 2, "activos": 2, "auditados": 1, "activosSinAuditar": 1,
                         "porCriticidad": {"BAJA": 1, "MEDIA": 0, "ALTA": 1, "CRITICA": 0}},
        "hallazgos": {"pendientes": 42, "enRevision": 21, "cerrados": 21,
                      "pendientesPorSeveridad": {"BAJA": 10, "MEDIA": 10, "ALTA": 11, "CRITICA": 11},
                      "cerradosPorSeveridad": {"BAJA": 5, "MEDIA": 5, "ALTA": 5, "CRITICA": 6}},
        "alertas": {"vigentes": 2, "activas": 1, "enAtencion": 1, "criticasVigentes": 2},
        "auditorias": {"total": 42, "ultimos30Dias": 42, "cumplimientoActual": 33},
        "auditoriasRecientes": AUDITORIAS[:5],
        "dispositivosEnRiesgo": [{"dispositivoId": 1, "identificador": "RTR-CORE-01", "nombre": "Router Core",
                                  "sedeId": 11, "criticidad": "ALTA", "nivel": "ALTO", "pendientes": 2,
                                  "criticosPendientes": 1, "altosPendientes": 1, "alertasVigentes": 2,
                                  "ultimaAuditoria": "2026-10-02T15:00:00Z"}],
        "evolucion": [{"semana": "2026-09-%02d" % dia, "auditorias": 1, "hallazgosDetectados": 2, "hallazgosCerrados": 1}
                      for dia in (1, 8, 15, 22, 29)],
    }


def estado_cliente(organizacion):
    return {"organizacionId": organizacion, "resumen": resumen_tablero(), "reportes": REPORTES,
            "sedes": [{"sedeId": 11, "nivel": "ALTO", "dispositivos": 1, "auditados": 1, "pendientes": 2,
                       "criticosPendientes": 1, "alertasVigentes": 2}],
            "dispositivos": resumen_tablero()["dispositivosEnRiesgo"]}


USUARIOS = [
    {"id": 1, "organizationId": 2, "nombre": "Admin", "email": "admin@acme.test", "nombreUsuario": "admin",
     "estado": "ACTIVO", "roles": ["ADMINISTRADOR_SISTEMA"], "bloqueadoHasta": None, "ultimoAcceso": "2026-10-11T10:00:00Z"},
    {"id": 2, "organizationId": 2, "nombre": "Ana Pérez", "email": "ana@acme.test", "nombreUsuario": "ana",
     "estado": "ACTIVO", "roles": ["ANALISTA_RED"], "bloqueadoHasta": "2999-01-01T10:15:00Z", "ultimoAcceso": None},
    {"id": 3, "organizationId": 3, "nombre": "<b>Beto</b>", "email": "beto@beta.test", "nombreUsuario": "beto",
     "estado": "DESHABILITADO", "roles": ["AUDITOR_TECNICO"], "bloqueadoHasta": None, "ultimoAcceso": None},
]
PERFILES = [{"id": i + 1, "nombre": nombre, "descripcion": "Descripción de " + nombre, "estado": "ACTIVO"}
            for i, nombre in enumerate(ROLES)]
PERMISOS = [{"modulo": "Tablero", "accion": "Consultar", "perfiles": ROLES},
            {"modulo": "Usuarios y perfiles", "accion": "Gestionar", "perfiles": ["ADMINISTRADOR_SISTEMA"]}]


def salud(servicio):
    return {"servicio": servicio, "estado": "UP", "version": "sha-prueba", "inicio": "2026-10-10T10:00:00Z",
            "segundosEnLinea": 3600, "memoriaUsadaMb": 120, "memoriaMaximaMb": 512, "verificado": "2026-10-11T12:00:00Z",
            "componentes": [{"nombre": "PostgreSQL", "estado": "UP", "latenciaMs": 4, "detalle": "esquema"}]}


SEGURIDAD = {"usuariosActivos": 2, "usuariosDeshabilitados": 1, "cuentasBloqueadas": 1,
             "eventosUltimas24h": {"LOGIN_EXITOSO": 5, "LOGIN_RECHAZADO": 6}, "ultimosRechazos": [],
             "desde": "2026-10-10T12:00:00Z"}
ALMACENAMIENTO = {"configurado": True, "contenedor": "auditnet-archivos",
                  "archivos": {"CONFIGURACION": {"cantidad": 1, "bytes": 120}, "REPORTE": {"cantidad": 0, "bytes": 0},
                               "RESPALDO": {"cantidad": 0, "bytes": 0}},
                  "configuracionesPendientes": 0, "reportesPendientes": 0}

# 60 registros: el paginador de bitácoras muestra tres páginas.
BITACORA_SISTEMA = [{"id": n, "fecha": "2026-10-0%dT10:00:00Z" % (1 + n % 9), "evento": "LOGIN_EXITOSO",
                     "resultado": "EXITO", "actor": "usuario%d" % n, "usuarioAfectadoId": None, "organizacionId": 2,
                     "origenIp": "10.0.0.%d" % n, "userAgent": "navegador", "correlacion": "c-%d" % n, "detalle": None}
                    for n in range(1, 61)]
