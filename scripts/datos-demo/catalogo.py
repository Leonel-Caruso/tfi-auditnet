"""Datos de demostración: organizaciones, sedes, usuarios, equipos, reglas y configuraciones Cisco.

Todas las empresas y personas son ficticias. Los correos se arman con el alias del correo base que se indica al
ejecutar (base+usuario@dominio), así llegan a una sola casilla.
"""

import random

TIPOS = {
    "ISR4331": {"nombre": "Router ISR 4331", "fabricante": "Cisco", "familia": "IOS-XE", "clase": "router"},
    "ISR1100": {"nombre": "Router ISR 1100", "fabricante": "Cisco", "familia": "IOS-XE", "clase": "router"},
    "C9200": {"nombre": "Switch Catalyst 9200", "fabricante": "Cisco", "familia": "IOS-XE", "clase": "switch"},
    "C2960X": {"nombre": "Switch Catalyst 2960-X", "fabricante": "Cisco", "familia": "IOS", "clase": "switch"},
}

# Cada organización: plan, sedes con sus equipos (sufijo de identificador, tipo, criticidad) y usuarios.
# "madurez" es la proporción de controles que ya cumplen los equipos en su primera configuración.
ORGANIZACIONES = [
    {
        "identificador": "COOP-LITORAL", "razonSocial": "Cooperativa Eléctrica del Litoral", "plan": "PROFESIONAL",
        "madurez": 0.65, "router": "ISR4331", "switch": "C9200", "dominio": "litoral.coop",
        "sedes": [
            ("Casa central Rosario", "Córdoba 1450, Rosario, Santa Fe", "ROS", [
                ("RTR-01", "router", "CRITICA"), ("RTR-02", "router", "ALTA"), ("SW-CORE-01", "switch", "CRITICA"),
                ("SW-ACC-01", "switch", "MEDIA"), ("SW-ACC-02", "switch", "MEDIA")]),
            ("Sucursal Santa Fe", "San Martín 2230, Santa Fe", "SFE", [
                ("RTR-01", "router", "ALTA"), ("SW-ACC-01", "switch", "MEDIA")]),
            ("Sucursal Paraná", "Urquiza 980, Paraná, Entre Ríos", "PNA", [
                ("RTR-01", "router", "ALTA"), ("SW-ACC-01", "switch", "BAJA")]),
            ("Estación transformadora Reconquista", "Ruta 11 km 790, Reconquista", "RQT", [
                ("RTR-01", "router", "CRITICA"), ("SW-OT-01", "switch", "ALTA")]),
        ],
        "usuarios": [
            ("mgarcia.litoral", "Mariana García", ["ADMINISTRADOR_ORGANIZACION"], "es"),
            ("jperalta.litoral", "Julián Peralta", ["ANALISTA_RED"], "es"),
            ("sruiz.litoral", "Sofía Ruiz", ["ANALISTA_RED"], "es"),
            ("cbenitez.litoral", "Carlos Benítez", ["AUDITOR_TECNICO"], "es"),
            ("lmolina.litoral", "Laura Molina", ["RESPONSABLE_GESTION_IT"], "es"),
        ],
    },
    {
        "identificador": "BODEGA-CERRO-ALTO", "razonSocial": "Bodega Cerro Alto S.A.", "plan": "INICIAL",
        "madurez": 0.5, "router": "ISR1100", "switch": "C2960X", "dominio": "cerroalto.wine",
        "sedes": [
            ("Planta Luján de Cuyo", "Ruta 7 km 1032, Luján de Cuyo, Mendoza", "LDC", [
                ("RTR-01", "router", "ALTA"), ("SW-01", "switch", "ALTA"), ("SW-02", "switch", "MEDIA")]),
            ("Oficina comercial Mendoza", "Av. San Martín 1120, Mendoza", "MZA", [
                ("RTR-01", "router", "MEDIA"), ("SW-01", "switch", "BAJA")]),
            ("Depósito San Juan", "Av. Libertador 3400, San Juan", "SJN", [
                ("RTR-01", "router", "MEDIA"), ("SW-01", "switch", "BAJA")]),
        ],
        "usuarios": [
            ("aquiroga.cerroalto", "Andrés Quiroga", ["ADMINISTRADOR_ORGANIZACION", "RESPONSABLE_GESTION_IT"], "es"),
            ("vlucero.cerroalto", "Valentina Lucero", ["ANALISTA_RED"], "en"),
            ("mfunes.cerroalto", "Martín Funes", ["AUDITOR_TECNICO"], "es"),
        ],
    },
    {
        "identificador": "AGRO-PAMPA", "razonSocial": "Agroindustrias La Pampa S.A.", "plan": "INICIAL",
        "madurez": 0.45, "router": "ISR1100", "switch": "C2960X", "dominio": "agropampa.com.ar",
        "sedes": [
            ("Administración Santa Rosa", "Av. San Martín 450, Santa Rosa, La Pampa", "SRO", [
                ("RTR-01", "router", "ALTA"), ("SW-01", "switch", "MEDIA")]),
            ("Acopio General Pico", "Calle 9 n.º 1250, General Pico, La Pampa", "GPI", [
                ("RTR-01", "router", "MEDIA"), ("SW-01", "switch", "BAJA"), ("SW-02", "switch", "BAJA")]),
        ],
        "usuarios": [
            ("ndiaz.agropampa", "Nicolás Díaz", ["ADMINISTRADOR_ORGANIZACION"], "es"),
            ("rgomez.agropampa", "Romina Gómez", ["ANALISTA_RED", "AUDITOR_TECNICO"], "es"),
            ("efernandez.agropampa", "Esteban Fernández", ["RESPONSABLE_GESTION_IT"], "es"),
        ],
    },
    {
        "identificador": "SALUD-AUSTRAL", "razonSocial": "Red Sanitaria Austral", "plan": "PROFESIONAL",
        "madurez": 0.7, "router": "ISR4331", "switch": "C9200", "dominio": "saludaustral.org",
        "sedes": [
            ("Hospital central Bahía Blanca", "Estomba 968, Bahía Blanca, Buenos Aires", "BBL", [
                ("RTR-01", "router", "CRITICA"), ("SW-CORE-01", "switch", "CRITICA"), ("SW-QUI-01", "switch", "CRITICA"),
                ("SW-ADM-01", "switch", "MEDIA")]),
            ("Centro de diagnóstico", "Alsina 245, Bahía Blanca, Buenos Aires", "CDX", [
                ("RTR-01", "router", "ALTA"), ("SW-01", "switch", "ALTA")]),
            ("Consultorios Punta Alta", "Bernardo de Irigoyen 210, Punta Alta, Buenos Aires", "PAL", [
                ("RTR-01", "router", "MEDIA"), ("SW-01", "switch", "BAJA")]),
        ],
        "usuarios": [
            ("pvazquez.austral", "Paula Vázquez", ["ADMINISTRADOR_ORGANIZACION"], "es"),
            ("dherrera.austral", "Diego Herrera", ["ANALISTA_RED"], "es"),
            ("gcastro.austral", "Gabriela Castro", ["AUDITOR_TECNICO"], "es"),
            ("fsosa.austral", "Federico Sosa", ["RESPONSABLE_GESTION_IT"], "es"),
        ],
    },
    {
        "identificador": "LOG-MERIDIANO", "razonSocial": "Logística Meridiano S.R.L.", "plan": "PILOTO",
        "madurez": 0.4, "router": "ISR4331", "switch": "C2960X", "dominio": "meridiano.log",
        "sedes": [
            ("Centro de distribución Pacheco", "Av. de los Constituyentes 2900, General Pacheco", "PAC", [
                ("RTR-01", "router", "ALTA"), ("SW-01", "switch", "MEDIA"), ("SW-02", "switch", "MEDIA")]),
            ("Centro de distribución Porto Alegre", "Av. Assis Brasil 4200, Porto Alegre, RS", "POA", [
                ("RTR-01", "router", "ALTA"), ("SW-01", "switch", "MEDIA")]),
        ],
        "usuarios": [
            ("tsilva.meridiano", "Tiago Silva", ["ADMINISTRADOR_ORGANIZACION", "ANALISTA_RED"], "pt"),
            ("cromero.meridiano", "Camila Romero", ["AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"], "es"),
        ],
    },
    {
        "identificador": "EDU-VALLE-NORTE", "razonSocial": "Instituto Educativo Valle Norte", "plan": "INICIAL",
        "madurez": 0.55, "router": "ISR1100", "switch": "C9200", "dominio": "vallenorte.edu.ar",
        "sedes": [
            ("Campus Salta", "Av. Bolivia 5150, Salta", "SAL", [
                ("RTR-01", "router", "ALTA"), ("SW-01", "switch", "MEDIA"), ("SW-LAB-01", "switch", "MEDIA")]),
            ("Anexo San Salvador de Jujuy", "Belgrano 760, San Salvador de Jujuy", "JUJ", [
                ("RTR-01", "router", "MEDIA"), ("SW-01", "switch", "BAJA")]),
        ],
        "usuarios": [
            ("hacosta.vallenorte", "Hernán Acosta", ["ADMINISTRADOR_ORGANIZACION"], "es"),
            ("jmendez.vallenorte", "Julieta Méndez", ["ANALISTA_RED"], "es"),
            ("oortiz.vallenorte", "Oscar Ortiz", ["AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"], "en"),
        ],
    },
]

# Controles de hardening. Cada uno es una regla de baseline y una parte de la configuración que la cumple o no.
CONTROLES = {
    "ssh2": {"codigo": "SEC-SSH-01", "nombre": "SSH versión 2", "tipo": "VALOR_ESPERADO", "patron": "ip ssh version",
             "valor": "2", "severidad": "ALTA",
             "descripcion": "El acceso remoto usa solo SSH versión 2.",
             "impacto": "SSH v1 tiene debilidades criptográficas conocidas que permiten interceptar la sesión.",
             "recomendacion": "ip ssh version 2"},
    "sin_telnet": {"codigo": "SEC-VTY-01", "nombre": "Telnet deshabilitado en las líneas VTY", "tipo": "NO_DEBE_CONTENER",
                   "patron": "transport input telnet", "severidad": "CRITICA",
                   "descripcion": "Las líneas de acceso remoto no aceptan Telnet.",
                   "impacto": "Telnet envía usuario y contraseña sin cifrar.",
                   "recomendacion": "line vty 0 4 / transport input ssh"},
    "cifrado": {"codigo": "SEC-PWD-01", "nombre": "Cifrado de contraseñas", "tipo": "DEBE_CONTENER",
                "patron": "service password-encryption", "severidad": "ALTA",
                "descripcion": "Las contraseñas no quedan en texto plano en la configuración.",
                "impacto": "Cualquier copia de la configuración expone las contraseñas.",
                "recomendacion": "service password-encryption"},
    "secret": {"codigo": "SEC-PWD-02", "nombre": "Sin enable password", "tipo": "NO_DEBE_CONTENER",
               "patron": "enable password", "severidad": "CRITICA",
               "descripcion": "El modo privilegiado se protege con enable secret.",
               "impacto": "enable password usa un cifrado reversible.",
               "recomendacion": "no enable password / enable secret <clave>"},
    "aaa": {"codigo": "SEC-AAA-01", "nombre": "AAA habilitado", "tipo": "DEBE_CONTENER", "patron": "aaa new-model",
            "severidad": "ALTA", "descripcion": "La autenticación se gestiona con AAA.",
            "impacto": "Sin AAA no hay control centralizado de accesos ni registro de quién ingresó.",
            "recomendacion": "aaa new-model / aaa authentication login default local"},
    "sin_http": {"codigo": "SEC-HTTP-01", "nombre": "Servidor HTTP deshabilitado", "tipo": "DEBE_CONTENER",
                 "patron": "no ip http server", "severidad": "MEDIA",
                 "descripcion": "La administración web sin cifrar está apagada.",
                 "impacto": "La interfaz HTTP expone credenciales y amplía la superficie de ataque.",
                 "recomendacion": "no ip http server"},
    "snmp": {"codigo": "SEC-SNMP-01", "nombre": "Sin comunidad SNMP public", "tipo": "NO_DEBE_CONTENER",
             "patron": "snmp-server community public", "severidad": "CRITICA",
             "descripcion": "No se usa la comunidad SNMP por defecto.",
             "impacto": "Con la comunidad por defecto cualquiera puede leer la configuración del equipo.",
             "recomendacion": "no snmp-server community public"},
    "timeout": {"codigo": "SEC-VTY-02", "nombre": "Cierre de sesión por inactividad", "tipo": "VALOR_ESPERADO",
                "patron": "exec-timeout", "valor": "10 0", "severidad": "MEDIA",
                "descripcion": "Las sesiones inactivas se cierran a los 10 minutos.",
                "impacto": "Una sesión abierta y desatendida puede ser usada por otra persona.",
                "recomendacion": "exec-timeout 10 0"},
    "marcas": {"codigo": "LOG-01", "nombre": "Registro con fecha y hora", "tipo": "DEBE_CONTENER",
               "patron": "service timestamps log datetime", "severidad": "BAJA",
               "descripcion": "Los eventos del registro tienen fecha y hora.",
               "impacto": "Sin marca de tiempo no se puede reconstruir un incidente.",
               "recomendacion": "service timestamps log datetime msec localtime"},
    "syslog": {"codigo": "LOG-02", "nombre": "Envío a servidor syslog", "tipo": "DEBE_CONTENER",
               "patron": "logging host", "severidad": "MEDIA",
               "descripcion": "Los eventos se envían a un servidor central.",
               "impacto": "Los registros locales se pierden al reiniciar el equipo.",
               "recomendacion": "logging host <servidor>"},
    "ntp": {"codigo": "NTP-01", "nombre": "Sincronización NTP", "tipo": "DEBE_CONTENER", "patron": "ntp server",
            "severidad": "MEDIA", "descripcion": "El reloj se sincroniza con un servidor NTP.",
            "impacto": "Relojes desfasados impiden correlacionar eventos entre equipos.",
            "recomendacion": "ntp server <servidor>"},
    "banner": {"codigo": "BAN-01", "nombre": "Aviso legal al ingresar", "tipo": "DEBE_CONTENER",
               "patron": "banner login", "severidad": "BAJA",
               "descripcion": "Se muestra un aviso de acceso restringido.",
               "impacto": "Sin aviso legal se debilita la acción ante accesos no autorizados.",
               "recomendacion": "banner login ^C Acceso restringido a personal autorizado ^C"},
    "vtp": {"codigo": "SW-VTP-01", "nombre": "VTP en modo transparente", "tipo": "VALOR_ESPERADO", "patron": "vtp mode",
            "valor": "transparent", "severidad": "MEDIA",
            "descripcion": "El switch no propaga ni acepta cambios de VLAN por VTP.",
            "impacto": "Un switch con revisión VTP mayor puede borrar las VLAN de toda la red.",
            "recomendacion": "vtp mode transparent"},
    "bpdu": {"codigo": "SW-STP-01", "nombre": "BPDU guard en puertos de acceso", "tipo": "DEBE_CONTENER",
             "patron": "spanning-tree portfast bpduguard default", "severidad": "ALTA",
             "descripcion": "Los puertos de acceso se bloquean si reciben BPDU.",
             "impacto": "Un switch conectado por error puede convertirse en raíz y cortar la red.",
             "recomendacion": "spanning-tree portfast bpduguard default"},
    "dhcp": {"codigo": "SW-DHCP-01", "nombre": "DHCP snooping", "tipo": "DEBE_CONTENER", "patron": "ip dhcp snooping",
             "severidad": "ALTA", "descripcion": "Solo los puertos confiables responden DHCP.",
             "impacto": "Un servidor DHCP falso puede desviar el tráfico de los usuarios.",
             "recomendacion": "ip dhcp snooping / ip dhcp snooping vlan <vlans>"},
    "sin_cdp": {"codigo": "SW-CDP-01", "nombre": "CDP deshabilitado", "tipo": "DEBE_CONTENER", "patron": "no cdp run",
                "severidad": "BAJA", "descripcion": "El equipo no anuncia su modelo y versión por CDP.",
                "impacto": "CDP entrega a un atacante información útil sobre la red.",
                "recomendacion": "no cdp run"},
}

BASELINES = {
    "router": {"nombre": "Hardening de routers", "descripcion": "Controles mínimos de acceso, registro y gestión para routers de borde.",
               "controles": ["ssh2", "sin_telnet", "cifrado", "secret", "aaa", "sin_http", "snmp", "timeout", "marcas",
                             "syslog", "ntp", "banner"]},
    "switch": {"nombre": "Hardening de switches", "descripcion": "Controles de acceso y de capa 2 para switches de acceso y núcleo.",
               "controles": ["ssh2", "sin_telnet", "cifrado", "secret", "snmp", "timeout", "syslog", "ntp", "vtp",
                             "bpdu", "dhcp", "sin_cdp"]},
}


def versiones(clase, madurez, semilla):
    """Las versiones de configuración de un equipo: cada una corrige algo de la anterior y a veces se pierde un
    control (un cambio que pisó la configuración). Devuelve una lista de conjuntos de controles cumplidos."""
    azar = random.Random(semilla)
    controles = BASELINES[clase]["controles"]
    cumplidos = {c for c in controles if azar.random() < madurez}
    cantidad = azar.choices([1, 2, 3, 4], weights=[15, 30, 35, 20])[0]
    lista = [set(cumplidos)]
    for _ in range(cantidad - 1):
        for control in controles:
            if control not in cumplidos and azar.random() < 0.5:
                cumplidos.add(control)
        if cumplidos and azar.random() < 0.2:
            cumplidos.discard(azar.choice(sorted(cumplidos)))
        lista.append(set(cumplidos))
    return lista


def configuracion(hostname, clase, dominio, cumplidos, red):
    """Configuración Cisco IOS / IOS-XE con los controles indicados. red: segundo octeto de las direcciones."""
    c = cumplidos.__contains__
    lineas = ["version 17.6" if clase == "router" else "version 15.2",
              "service timestamps debug datetime msec"]
    if c("marcas"):
        lineas.append("service timestamps log datetime msec localtime")
    if c("cifrado"):
        lineas.append("service password-encryption")
    lineas += ["!", f"hostname {hostname}", "!"]
    lineas.append("enable secret 9 $9$Q2h0aW1hcmdvbmEx$k3E1x0ZPo6dQkqV8t1mU" if c("secret")
                  else "enable password 7 0822455D0A16544541")
    if c("aaa"):
        lineas += ["!", "aaa new-model", "aaa authentication login default local",
                   "aaa authorization exec default local"]
    lineas += ["!", f"ip domain name {dominio}", "ip ssh version 2" if c("ssh2") else "ip ssh version 1"]
    if clase == "router":
        lineas.append("no ip http server" if c("sin_http") else "ip http server")
        lineas += ["no ip http secure-server", "!",
                   "interface GigabitEthernet0/0/0", " description Enlace WAN",
                   f" ip address 10.{red}.0.2 255.255.255.252", " no shutdown", "!",
                   "interface GigabitEthernet0/0/1", " description Red interna",
                   f" ip address 10.{red}.10.1 255.255.255.0", " no shutdown", "!",
                   f"ip route 0.0.0.0 0.0.0.0 10.{red}.0.1"]
    else:
        lineas.append("vtp mode transparent" if c("vtp") else "vtp mode server")
        lineas += ["spanning-tree mode rapid-pvst"]
        if c("bpdu"):
            lineas.append("spanning-tree portfast bpduguard default")
        if c("dhcp"):
            lineas += ["ip dhcp snooping", "ip dhcp snooping vlan 10,20"]
        lineas += ["!", "vlan 10", " name USUARIOS", "vlan 20", " name SERVIDORES", "!",
                   "interface range GigabitEthernet1/0/1 - 24", " switchport mode access",
                   " switchport access vlan 10", " spanning-tree portfast", "!",
                   "interface Vlan10", f" ip address 10.{red}.10.{20 + len(hostname)} 255.255.255.0"]
    lineas.append("!")
    lineas.append("snmp-server community public RO" if not c("snmp") else "snmp-server community Mon1t0r-RO RO 10")
    if c("syslog"):
        lineas += ["logging buffered 16384", f"logging host 10.{red}.20.5"]
    if c("ntp"):
        lineas.append(f"ntp server 10.{red}.20.1")
    if clase == "switch" and c("sin_cdp"):
        lineas.append("no cdp run")
    if c("banner"):
        lineas += ["banner login ^C", "Acceso restringido a personal autorizado.", "^C"]
    lineas += ["!", "line con 0", " exec-timeout 10 0" if c("timeout") else " exec-timeout 0 0",
               "line vty 0 4", " exec-timeout 10 0" if c("timeout") else " exec-timeout 30 0",
               " transport input ssh" if c("sin_telnet") else " transport input telnet ssh", "!", "end", ""]
    return "\n".join(lineas)
