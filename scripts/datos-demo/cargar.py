"""Carga datos de demostración en AuditNet usando la API, como lo haría un administrador desde la consola.

Uso (sin dependencias, Python 3.9 o superior):

    python cargar.py --url https://<sitio> --admin <usuario> --correo <casilla@gmail.com>
    python cargar.py --local --admin <usuario> --correo <casilla@gmail.com>
    python cargar.py ... --reportes      (después de distribuir las fechas con historia.sql)

Pide la contraseña del administrador y la que tendrán los usuarios nuevos; no se guardan en ningún lado.
Se puede volver a ejecutar: lo que ya existe se reutiliza y no se duplica.
"""

import argparse
import getpass
import json
import random
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

import catalogo

RUTAS_MANAGEMENT = re.compile(r"^/api/(auth|users|roles|client-organizations|sites|plans|management|admin)(/|$)")
NOMBRES_EQUIPO = {"RTR-01": "Router principal", "RTR-02": "Router de respaldo", "SW-CORE-01": "Switch de núcleo",
                  "SW-OT-01": "Switch de red operativa", "SW-QUI-01": "Switch de quirófanos",
                  "SW-ADM-01": "Switch administrativo", "SW-LAB-01": "Switch de laboratorio"}
SIN_AVISOS = {"alertas": False, "vencimientos": False, "resumen": False}


class ErrorApi(Exception):
    pass


class Api:
    def __init__(self, management, core, idioma="es"):
        self.management = management.rstrip("/")
        self.core = core.rstrip("/")
        self.idioma = idioma
        self.token = None

    def pedir(self, metodo, ruta, cuerpo=None):
        base = self.management if RUTAS_MANAGEMENT.match(ruta) else self.core
        datos = None if cuerpo is None else json.dumps(cuerpo).encode()
        pedido = urllib.request.Request(base + ruta, data=datos, method=metodo)
        pedido.add_header("Accept", "application/json")
        pedido.add_header("Accept-Language", self.idioma)
        if datos is not None:
            pedido.add_header("Content-Type", "application/json")
        if self.token:
            pedido.add_header("Authorization", "Bearer " + self.token)
        try:
            with urllib.request.urlopen(pedido, timeout=60) as respuesta:
                texto = respuesta.read().decode()
                return json.loads(texto) if texto else None
        except urllib.error.HTTPError as error:
            texto = error.read().decode(errors="replace")
            try:
                cuerpo_error = json.loads(texto)
                texto = cuerpo_error.get("message") or cuerpo_error.get("mensaje") or texto
            except (ValueError, AttributeError):
                pass
            raise ErrorApi(f"{metodo} {ruta} → {error.code}: {texto}") from None
        except urllib.error.URLError as error:
            raise ErrorApi(f"No se pudo conectar con {base}: {error.reason}") from None

    def ingresar(self, identidad, password):
        respuesta = self.pedir("POST", "/api/auth/login", {"identidad": identidad, "password": password})
        self.token = respuesta["accessToken"]
        return respuesta

    def como(self, idioma="es"):
        """Otra sesión contra el mismo servidor."""
        return Api(self.management, self.core, idioma)


def aviso(texto):
    print("  " + texto)


def correo_alias(base, usuario):
    local, dominio = base.split("@", 1)
    return f"{local}+{usuario}@{dominio}"


def contrasena_valida(password, usuarios):
    if not 10 <= len(password) <= 72:
        return "debe tener entre 10 y 72 caracteres"
    if not re.search(r"[A-Za-z]", password) or not re.search(r"\d", password):
        return "debe combinar letras y números"
    for usuario in usuarios:
        if usuario in password.lower():
            return f"no puede contener el nombre de usuario ({usuario})"
    return None


def nombre_equipo(sufijo, sede):
    if sufijo in NOMBRES_EQUIPO:
        return f"{NOMBRES_EQUIPO[sufijo]} {sede}"
    numero = sufijo.rsplit("-", 1)[-1].lstrip("0")
    return f"Switch de acceso {numero} {sede}" if "ACC" in sufijo else f"Switch {numero} {sede}"


def tipos_de_dispositivo(admin):
    existentes = admin.pedir("GET", "/api/device-types")
    ids = {}
    for clave, tipo in catalogo.TIPOS.items():
        encontrado = next((t for t in existentes if t["nombre"] == tipo["nombre"]
                           and t["fabricante"] == tipo["fabricante"]), None)
        if not encontrado:
            encontrado = admin.pedir("POST", "/api/device-types", {k: tipo[k] for k in ("nombre", "fabricante", "familia")})
            aviso(f"tipo de dispositivo {tipo['nombre']}")
        ids[clave] = encontrado["id"]
    return ids


def organizacion(admin, org):
    existente = next((o for o in admin.pedir("GET", "/api/client-organizations")
                      if o["identificador"] == org["identificador"]), None)
    if existente:
        return existente["id"]
    creada = admin.pedir("POST", "/api/client-organizations",
                         {"identificador": org["identificador"], "razonSocial": org["razonSocial"], "plan": org["plan"]})
    aviso(f"organización {org['razonSocial']} (plan {org['plan']})")
    return creada["id"]


def sedes(admin, org_id, org):
    existentes = {s["nombre"]: s["id"] for s in admin.pedir("GET", f"/api/client-organizations/{org_id}/sites")}
    ids = {}
    for nombre, ubicacion, codigo, _ in org["sedes"]:
        if nombre not in existentes:
            existentes[nombre] = admin.pedir("POST", f"/api/client-organizations/{org_id}/sites",
                                             {"nombre": nombre, "ubicacion": ubicacion})["id"]
            aviso(f"sede {nombre}")
        ids[codigo] = existentes[nombre]
    return ids


def usuarios(admin, org_id, org, correo_base, password):
    """Crea los usuarios y, con la sesión de cada uno, apaga sus avisos por correo y elige su idioma."""
    existentes = {u["nombreUsuario"] for u in admin.pedir("GET", "/api/users")}
    sesiones = {}
    for usuario, nombre, roles, idioma in org["usuarios"]:
        if usuario not in existentes:
            admin.pedir("POST", "/api/users", {"organizationId": org_id, "nombre": nombre,
                                               "email": correo_alias(correo_base, usuario), "nombreUsuario": usuario,
                                               "password": password, "roles": roles})
            aviso(f"usuario {usuario} ({', '.join(roles)})")
        sesion = admin.como(idioma)
        try:
            sesion.ingresar(usuario, password)
        except ErrorApi as error:
            aviso(f"no se pudo ingresar como {usuario} ({error}); sus operaciones las hace el administrador")
            continue
        sesion.pedir("PUT", "/api/auth/me/notifications", SIN_AVISOS)
        if idioma != "es":
            sesion.pedir("PUT", "/api/auth/me/language", {"idioma": idioma})
        for rol in roles:
            sesiones.setdefault(rol, sesion)
    return sesiones


def baselines(admin, org_id, org, tipos):
    """Una baseline por tipo de equipo de la organización, con sus reglas. Si ya existía no se toca."""
    existentes = [b for b in admin.pedir("GET", "/api/baselines") if b["organizacionId"] == org_id]
    for clase in ("router", "switch"):
        definicion = catalogo.BASELINES[clase]
        tipo_id = tipos[org[clase]]
        if any(b["nombre"] == definicion["nombre"] for b in existentes):
            continue
        baseline = admin.pedir("POST", "/api/baselines", {
            "nombre": definicion["nombre"], "descripcion": definicion["descripcion"],
            "tipoDispositivoId": tipo_id, "organizacionId": org_id})
        for clave in definicion["controles"]:
            control = catalogo.CONTROLES[clave]
            admin.pedir("POST", f"/api/baselines/{baseline['id']}/rules", {
                "codigo": control["codigo"], "nombre": control["nombre"], "descripcion": control["descripcion"],
                "tipo": control["tipo"], "patron": control["patron"], "valorEsperado": control.get("valor"),
                "severidad": control["severidad"], "impacto": control["impacto"],
                "recomendacion": control["recomendacion"]})
        aviso(f"baseline {definicion['nombre']} con {len(definicion['controles'])} reglas")


def equipos(admin, org_id, org, tipos, ids_sede):
    existentes = {d["identificador"]: d for d in admin.pedir("GET", "/api/devices") if d["organizacionId"] == org_id}
    lista = []
    for _, _, codigo, dispositivos in org["sedes"]:
        for sufijo, clase, criticidad in dispositivos:
            identificador = f"{codigo}-{sufijo}"
            equipo = existentes.get(identificador)
            if not equipo:
                tipo = catalogo.TIPOS[org[clase]]
                equipo = admin.pedir("POST", "/api/devices", {
                    "nombre": nombre_equipo(sufijo, codigo), "identificador": identificador,
                    "tipoDispositivoId": tipos[org[clase]], "fabricante": tipo["fabricante"],
                    "organizacionId": org_id, "sedeId": ids_sede[codigo], "criticidad": criticidad})
            lista.append((equipo, clase))
    aviso(f"{len(lista)} dispositivos")
    return lista


def auditar(analista, admin, org, lista):
    """Importa las versiones de configuración de cada equipo y las audita una por una. Al terminar cada equipo,
    deja algunos hallazgos de la última auditoría en revisión o con riesgo aceptado."""
    con_configuracion = {c["dispositivoId"] for c in admin.pedir("GET", "/api/configurations/auditable")}
    auditorias = 0
    for equipo, clase in lista:
        if equipo["id"] in con_configuracion:
            continue
        semilla = f"{org['identificador']}:{equipo['identificador']}"
        red = 16 + sum(map(ord, org["identificador"])) % 200
        ultima = None
        for numero, cumplidos in enumerate(catalogo.versiones(clase, org["madurez"], semilla), start=1):
            texto = catalogo.configuracion(equipo["identificador"], clase, org["dominio"], cumplidos, red)
            configuracion = analista.pedir("POST", f"/api/devices/{equipo['id']}/configurations", {
                "contenido": texto, "formato": "CFG",
                "nombreFuente": f"{equipo['identificador'].lower()}-v{numero}.cfg"})
            ultima = analista.pedir("POST", "/api/audits", {"configuracionId": configuracion["id"]})
            auditorias += 1
        seguimiento(analista, ultima["hallazgos"], semilla)
    aviso(f"{auditorias} auditorías")


def seguimiento(analista, hallazgos, semilla):
    azar = random.Random(semilla + ":seguimiento")
    for hallazgo in hallazgos:
        sorteo = azar.random()
        if sorteo < 0.3:
            analista.pedir("PATCH", f"/api/findings/{hallazgo['id']}/status", {
                "estado": "EN_REVISION", "comentario": "Corrección planificada para la próxima ventana de mantenimiento."})
        elif sorteo < 0.4 and hallazgo["severidad"] in ("BAJA", "MEDIA"):
            analista.pedir("PATCH", f"/api/findings/{hallazgo['id']}/status", {
                "estado": "ACEPTADO", "comentario": "Riesgo aceptado: el equipo se reemplaza este trimestre."})


def derivar_alertas(responsable, semilla):
    azar = random.Random(semilla + ":alertas")
    for alerta in responsable.pedir("GET", "/api/alerts/critical?estado=ACTIVA"):
        if azar.random() < 0.4:
            responsable.pedir("PATCH", f"/api/alerts/{alerta['id']}/status", {
                "estado": "EN_ATENCION", "perfil": "ANALISTA_RED",
                "comentario": "Derivada a redes para corregir en el equipo."})


def reportes(admin, password):
    """Un reporte de las últimas 8 semanas y otro del mes anterior por organización, generados por su auditor."""
    hoy = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    inicio_mes = hoy.replace(day=1)
    mes_anterior = (inicio_mes - timedelta(days=1)).replace(day=1)
    periodos = [(hoy - timedelta(weeks=8), hoy + timedelta(days=1) - timedelta(seconds=1)),
                (mes_anterior, inicio_mes - timedelta(seconds=1))]
    for org in catalogo.ORGANIZACIONES:
        usuario, _, _, idioma = next(u for u in org["usuarios"] if "AUDITOR_TECNICO" in u[2])
        auditor = admin.como(idioma)
        try:
            auditor.ingresar(usuario, password)
        except ErrorApi as error:
            aviso(f"{org['identificador']}: no se pudo ingresar como {usuario} ({error})")
            continue
        for desde, hasta in periodos:
            respuesta = auditor.pedir("POST", "/api/reports/audits", {
                "desde": desde.strftime("%Y-%m-%dT%H:%M:%SZ"), "hasta": hasta.strftime("%Y-%m-%dT%H:%M:%SZ")})
            if respuesta and respuesta.get("titulo"):
                aviso(f"{org['identificador']}: {respuesta['titulo']}")


def main():
    parametros = argparse.ArgumentParser(description="Carga datos de demostración en AuditNet.")
    destino = parametros.add_mutually_exclusive_group(required=True)
    destino.add_argument("--url", help="dirección del sitio (la API se llama en el mismo origen)")
    destino.add_argument("--local", action="store_true", help="servicios locales en los puertos 8081 y 8082")
    parametros.add_argument("--admin", required=True, help="usuario o correo del administrador del sistema")
    parametros.add_argument("--correo", required=True,
                            help="casilla base para los usuarios nuevos: se usa casilla+usuario@dominio")
    parametros.add_argument("--reportes", action="store_true",
                            help="solo generar los reportes (después de ejecutar historia.sql)")
    args = parametros.parse_args()

    if not re.fullmatch(r"[^\s@+]+@[^\s@]+\.[^\s@]+", args.correo):
        sys.exit("--correo debe ser una casilla sin alias, por ejemplo nombre@gmail.com")
    if args.local:
        api = Api("http://localhost:8081", "http://localhost:8082")
    else:
        api = Api(args.url, args.url)

    nombres = [u[0] for org in catalogo.ORGANIZACIONES for u in org["usuarios"]]
    try:
        api.ingresar(args.admin, getpass.getpass("Contraseña del administrador: "))
        password = getpass.getpass("Contraseña para los usuarios de demostración: ")
        problema = contrasena_valida(password, nombres)
        if problema:
            sys.exit(f"La contraseña {problema}.")
        if not args.reportes and password != getpass.getpass("Repetila: "):
            sys.exit("Las contraseñas no coinciden.")

        if args.reportes:
            print("Reportes")
            reportes(api, password)
            return

        print("Tipos de dispositivo")
        tipos = tipos_de_dispositivo(api)
        for org in catalogo.ORGANIZACIONES:
            print(org["razonSocial"])
            org_id = organizacion(api, org)
            ids_sede = sedes(api, org_id, org)
            sesiones = usuarios(api, org_id, org, args.correo, password)
            baselines(api, org_id, org, tipos)
            lista = equipos(api, org_id, org, tipos, ids_sede)
            auditar(sesiones.get("ANALISTA_RED", api), api, org, lista)
            derivar_alertas(sesiones.get("RESPONSABLE_GESTION_IT", api), org["identificador"])
    except ErrorApi as error:
        sys.exit(f"Se detuvo la carga: {error}")
    print("Listo. Para repartir la historia en 8 semanas, ejecutar historia.sql y después este script con --reportes.")


if __name__ == "__main__":
    main()
