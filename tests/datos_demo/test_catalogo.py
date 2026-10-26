"""Datos de demostración: cada configuración cumple exactamente los controles de su versión y se respetan los
límites de los planes."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts" / "datos-demo"))
import catalogo  # noqa: E402

LIMITES = {"PILOTO": (10, 5), "INICIAL": (50, 15), "PROFESIONAL": (150, 40)}


def _compactar(texto):
    return " ".join(texto.split())


def _cumple(configuracion, control):
    """Misma evaluación que MotorAuditoriaService."""
    if control["tipo"] == "VALOR_ESPERADO":
        parametro = _compactar(control["patron"]).lower()
        valores = [_compactar(linea)[len(parametro):].strip() for linea in configuracion.splitlines()
                   if _compactar(linea).lower() == parametro or _compactar(linea).lower().startswith(parametro + " ")]
        return any(valor.lower() == control["valor"].lower() for valor in valores)
    contiene = control["patron"].lower() in configuracion.lower()
    return contiene if control["tipo"] == "DEBE_CONTENER" else not contiene


def test_cada_version_cumple_exactamente_sus_controles():
    for org in catalogo.ORGANIZACIONES:
        for _, _, codigo, equipos in org["sedes"]:
            for sufijo, clase, _ in equipos:
                for cumplidos in catalogo.versiones(clase, org["madurez"], f"{org['identificador']}:{codigo}-{sufijo}"):
                    texto = catalogo.configuracion(f"{codigo}-{sufijo}", clase, org["dominio"], cumplidos, 20)
                    for clave in catalogo.BASELINES[clase]["controles"]:
                        assert _cumple(texto, catalogo.CONTROLES[clave]) == (clave in cumplidos), (org["identificador"], sufijo, clave)


def test_los_planes_alcanzan_y_los_datos_no_se_repiten():
    usuarios = [u[0] for org in catalogo.ORGANIZACIONES for u in org["usuarios"]]
    assert len(usuarios) == len(set(usuarios))
    for org in catalogo.ORGANIZACIONES:
        equipos = sum(len(sede[3]) for sede in org["sedes"])
        assert equipos <= LIMITES[org["plan"]][0] and len(org["usuarios"]) <= LIMITES[org["plan"]][1]
        identificadores = [f"{sede[2]}-{e[0]}" for sede in org["sedes"] for e in sede[3]]
        assert len(identificadores) == len(set(identificadores))
        roles = {r for u in org["usuarios"] for r in u[2]}
        assert {"ANALISTA_RED", "AUDITOR_TECNICO", "RESPONSABLE_GESTION_IT"} <= roles


def test_las_reglas_de_cada_baseline_tienen_codigos_distintos():
    for definicion in catalogo.BASELINES.values():
        codigos = [catalogo.CONTROLES[c]["codigo"] for c in definicion["controles"]]
        assert len(codigos) == len(set(codigos))
        assert all((catalogo.CONTROLES[c]["tipo"] == "VALOR_ESPERADO") == ("valor" in catalogo.CONTROLES[c])
                   for c in definicion["controles"])
