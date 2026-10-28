"""Genera los manuales de AuditNet en PDF: uno de usuario por perfil y el de instalación y desarrollo, en español,
inglés y portugués.

Desde la raíz del repositorio, con el sitio de pruebas compilado (las capturas se sacan de ahí):

    cd frontend/web-app && npm run build:pruebas && cd ../..
    python -m pip install -r docs/manuales/requirements.txt
    python docs/manuales/generar.py                    # capturas y todos los PDF
    python docs/manuales/generar.py --sin-capturas     # reutiliza las capturas ya sacadas

Los manuales de usuario quedan en frontend/web-app/public/manuales (la consola los ofrece para descargar) y el de
instalación y desarrollo en docs/manuales/pdf.
"""

import argparse
import re
import sys
from datetime import date
from pathlib import Path

import markdown
from playwright.sync_api import sync_playwright

RAIZ = Path(__file__).resolve().parents[2]
MANUALES = Path(__file__).resolve().parent
CAPTURAS = MANUALES / ".capturas"
SALIDA_USUARIO = RAIZ / "frontend" / "web-app" / "public" / "manuales"
SALIDA_DESARROLLO = MANUALES / "pdf"
FUENTE_MARCA = RAIZ / "frontend" / "web-app" / "public" / "fonts" / "jetbrains-mono-800.woff2"
IDIOMAS = ("es", "en", "pt")

# Capítulos (archivos de <idioma>/usuario/) de cada manual de usuario, en orden.
COMUNES_INICIO = ["acceso", "consola", "mi-cuenta", "tablero"]
COMUNES_FIN = ["avisos", "problemas"]
USUARIO = {
    "administrador-sistema": ("ADMINISTRADOR_SISTEMA", [
        "perfil-administrador-sistema", *COMUNES_INICIO, "organizaciones-sistema", "usuarios-sistema",
        "planes-sistema", "dispositivos-gestion", "baselines-gestion", "auditorias", "historial",
        "hallazgos-seguimiento", "alertas-derivar", "reportes-generar", "bitacoras", "administracion", *COMUNES_FIN]),
    "administrador-organizacion": ("ADMINISTRADOR_ORGANIZACION", [
        "perfil-administrador-organizacion", *COMUNES_INICIO, "organizaciones-propia", "usuarios-organizacion",
        "plan-organizacion", "dispositivos-gestion", "baselines-consulta", "historial", "hallazgos-consulta",
        "alertas-consulta", "reportes-consulta", *COMUNES_FIN]),
    "analista-redes": ("ANALISTA_RED", [
        "perfil-analista-redes", *COMUNES_INICIO, "dispositivos-consulta", "baselines-consulta", "auditorias",
        "historial", "hallazgos-seguimiento", "alertas-derivar", "reportes-generar", *COMUNES_FIN]),
    "auditor-tecnico": ("AUDITOR_TECNICO", [
        "perfil-auditor-tecnico", *COMUNES_INICIO, "dispositivos-consulta", "baselines-gestion", "auditorias",
        "historial", "hallazgos-seguimiento", "alertas-consulta", "reportes-generar", *COMUNES_FIN]),
    "responsable-gestion-it": ("RESPONSABLE_GESTION_IT", [
        "perfil-responsable-gestion-it", *COMUNES_INICIO, "plan-consulta", "dispositivos-consulta", "historial",
        "hallazgos-consulta", "alertas-derivar", "reportes-consulta", *COMUNES_FIN]),
}

TEXTOS = {
    "es": {
        "usuario": "Manual de usuario", "desarrollo": "Manual de instalación y desarrollo",
        "bajadaUsuario": "Guía paso a paso de la consola de auditoría de configuraciones de red.",
        "bajadaDesarrollo": "Arquitectura, puesta en marcha, pruebas, despliegue en Azure y cómo extender la plataforma.",
        "marca": "AuditNet · Auditoría de configuraciones de red", "contenido": "Contenido", "figura": "Figura",
        "pagina": "Página", "version": "Versión", "idioma": "Español",
        "meses": "enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre",
        "perfiles": {"ADMINISTRADOR_SISTEMA": "Administrador del sistema",
                     "ADMINISTRADOR_ORGANIZACION": "Administrador de organización",
                     "ANALISTA_RED": "Analista de redes", "AUDITOR_TECNICO": "Auditor técnico",
                     "RESPONSABLE_GESTION_IT": "Responsable de gestión IT"},
    },
    "en": {
        "usuario": "User guide", "desarrollo": "Installation and development guide",
        "bajadaUsuario": "Step-by-step guide to the network configuration auditing console.",
        "bajadaDesarrollo": "Architecture, setup, testing, Azure deployment and how to extend the platform.",
        "marca": "AuditNet · Network configuration auditing", "contenido": "Contents", "figura": "Figure",
        "pagina": "Page", "version": "Version", "idioma": "English",
        "meses": "January February March April May June July August September October November December",
        "perfiles": {"ADMINISTRADOR_SISTEMA": "System administrator",
                     "ADMINISTRADOR_ORGANIZACION": "Organization administrator",
                     "ANALISTA_RED": "Network analyst", "AUDITOR_TECNICO": "Technical auditor",
                     "RESPONSABLE_GESTION_IT": "IT management lead"},
    },
    "pt": {
        "usuario": "Manual do usuário", "desarrollo": "Manual de instalação e desenvolvimento",
        "bajadaUsuario": "Guia passo a passo do console de auditoria de configurações de rede.",
        "bajadaDesarrollo": "Arquitetura, instalação, testes, implantação no Azure e como estender a plataforma.",
        "marca": "AuditNet · Auditoria de configurações de rede", "contenido": "Sumário", "figura": "Figura",
        "pagina": "Página", "version": "Versão", "idioma": "Português",
        "meses": "janeiro fevereiro março abril maio junho julho agosto setembro outubro novembro dezembro",
        "perfiles": {"ADMINISTRADOR_SISTEMA": "Administrador do sistema",
                     "ADMINISTRADOR_ORGANIZACION": "Administrador da organização",
                     "ANALISTA_RED": "Analista de redes", "AUDITOR_TECNICO": "Auditor técnico",
                     "RESPONSABLE_GESTION_IT": "Responsável pela gestão de TI"},
    },
}
VERSION = "1.0"


def _fecha(idioma):
    hoy = date.today()
    mes = TEXTOS[idioma]["meses"].split()[hoy.month - 1]
    return f"{mes} {hoy.year}"


def _capitulo(ruta, idioma, numero_figura):
    """Markdown de un capítulo a HTML; las imágenes quedan como figuras numeradas con su epígrafe."""
    conversor = markdown.Markdown(extensions=["tables", "toc", "fenced_code", "sane_lists"],
                                  extension_configs={"toc": {"toc_depth": "2-3"}})
    html = conversor.convert(ruta.read_text(encoding="utf-8"))
    figura = TEXTOS[idioma]["figura"]

    def a_figura(coincidencia):
        numero_figura[0] += 1
        alt, src = coincidencia.group(1), coincidencia.group(2)
        imagen = src if "/" in src else (CAPTURAS / idioma / src).as_uri()
        return (f'<figure><img src="{imagen}" alt="{alt}"><figcaption><b>{figura} {numero_figura[0]}.</b> {alt}'
                f"</figcaption></figure>")

    html = re.sub(r'<p><img alt="([^"]*)" src="([^"]+)" ?/?></p>', a_figura, html)
    return html, conversor.toc_tokens


def _indice(tokens):
    def items(lista):
        return "".join(f'<li><a href="#{t["id"]}">{t["name"]}</a>'
                       + (f"<ul>{items(t['children'])}</ul>" if t["children"] else "") + "</li>" for t in lista)
    return f"<ul>{items(tokens)}</ul>"


def _documento(idioma, titulo, perfil, bajada, archivos, pie):
    textos = TEXTOS[idioma]
    numero_figura = [0]
    capitulos, tokens = [], []
    for archivo in archivos:
        html, toc = _capitulo(archivo, idioma, numero_figura)
        capitulos.append(f'<section class="capitulo">{html}</section>')
        tokens.extend(toc)
    estilo = (MANUALES / "plantilla" / "estilo.css").read_text(encoding="utf-8")
    estilo = (estilo.replace("FUENTE_MARCA", FUENTE_MARCA.as_uri())
              .replace("FUENTES", (MANUALES / "plantilla" / "fuentes").as_uri()).replace("PIE_IZQUIERDA", pie)
              .replace("PIE_PAGINA", textos["pagina"]))
    subtitulo = f'<p class="perfil">{perfil}</p>' if perfil else ""
    return f"""<!doctype html>
<html lang="{idioma}">
<head><meta charset="utf-8"><title>AuditNet · {titulo}{' · ' + perfil if perfil else ''}</title>
<style>{estilo}</style></head>
<body>
<section class="portada">
  <span class="logo">A_</span>
  <p class="eyebrow">{textos['marca']}</p>
  <h1>{titulo}</h1>
  {subtitulo}
  <p class="bajada">{bajada}</p>
  <div class="datos"><span>{textos['version']} {VERSION} · {_fecha(idioma)}</span><span>{textos['idioma']}</span></div>
</section>
<nav class="indice"><h2>{textos['contenido']}</h2>{_indice(tokens)}</nav>
{''.join(capitulos)}
</body></html>"""


def _imprimir(navegador, html, destino):
    destino.parent.mkdir(parents=True, exist_ok=True)
    temporal = CAPTURAS / f"{destino.stem}.html"
    temporal.write_text(html, encoding="utf-8")
    pagina = navegador.new_page()
    pagina.goto(temporal.as_uri(), wait_until="load")
    pagina.evaluate("document.fonts.ready")
    pagina.pdf(path=str(destino), format="A4", print_background=True, prefer_css_page_size=True)
    pagina.close()
    temporal.unlink()
    print(f"  {destino.relative_to(RAIZ)} ({destino.stat().st_size // 1024} KB)")


def manuales_de_usuario(navegador, idiomas):
    for idioma in idiomas:
        textos = TEXTOS[idioma]
        for nombre, (perfil, capitulos) in USUARIO.items():
            etiqueta = textos["perfiles"][perfil]
            archivos = [MANUALES / idioma / "usuario" / f"{c}.md" for c in capitulos]
            html = _documento(idioma, textos["usuario"], etiqueta, textos["bajadaUsuario"], archivos,
                              f"AuditNet · {textos['usuario']} · {etiqueta}")
            _imprimir(navegador, html, SALIDA_USUARIO / f"manual-{nombre}-{idioma}.pdf")


def manual_de_desarrollo(navegador, idiomas):
    for idioma in idiomas:
        textos = TEXTOS[idioma]
        archivos = sorted((MANUALES / idioma / "desarrollo").glob("*.md"))
        html = _documento(idioma, textos["desarrollo"], None, textos["bajadaDesarrollo"], archivos,
                          f"AuditNet · {textos['desarrollo']}")
        _imprimir(navegador, html, SALIDA_DESARROLLO / f"manual-desarrollo-{idioma}.pdf")


def main():
    parametros = argparse.ArgumentParser(description="Genera los manuales de AuditNet en PDF.")
    parametros.add_argument("--sin-capturas", action="store_true", help="reutiliza las capturas ya sacadas")
    parametros.add_argument("--solo", choices=["usuario", "desarrollo"], help="genera solo esos manuales")
    parametros.add_argument("--idioma", choices=IDIOMAS, help="genera solo ese idioma")
    args = parametros.parse_args()
    idiomas = [args.idioma] if args.idioma else list(IDIOMAS)
    CAPTURAS.mkdir(exist_ok=True)

    if not args.sin_capturas and args.solo != "desarrollo":
        import capturas
        print("Capturas")
        capturas.sacar(idiomas, CAPTURAS)

    with sync_playwright() as playwright:
        navegador = playwright.chromium.launch()
        if args.solo != "desarrollo":
            print("Manuales de usuario")
            manuales_de_usuario(navegador, idiomas)
        if args.solo != "usuario":
            print("Manual de instalación y desarrollo")
            manual_de_desarrollo(navegador, idiomas)
        navegador.close()


if __name__ == "__main__":
    sys.path.insert(0, str(MANUALES))
    main()
