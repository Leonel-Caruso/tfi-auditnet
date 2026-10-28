"""Los reportes de demostración cubren días completos en hora de Argentina."""

import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts" / "datos-demo"))
import cargar  # noqa: E402


def _utc(momento):
    return momento.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def test_el_mes_anterior_va_de_las_0_a_las_23_59_59_de_argentina():
    _, (desde, hasta) = cargar.periodos_reportes(datetime(2026, 10, 15, 14, 0, tzinfo=timezone.utc))
    assert _utc(desde) == "2026-09-01T03:00:00Z"
    assert _utc(hasta) == "2026-10-01T02:59:59Z"


def test_a_la_noche_de_argentina_todavia_cuenta_el_dia_local():
    # 1 de noviembre 01:30 UTC es todavía 31 de octubre a las 22:30 en Argentina.
    periodos = cargar.periodos_reportes(datetime(2026, 11, 1, 1, 30, tzinfo=timezone.utc))
    (desde, hasta), (mes_desde, mes_hasta) = periodos
    assert _utc(hasta) == "2026-11-01T02:59:59Z"
    assert _utc(desde) == "2026-09-05T03:00:00Z"
    assert _utc(mes_desde) == "2026-09-01T03:00:00Z"
    assert _utc(mes_hasta) == "2026-10-01T02:59:59Z"
