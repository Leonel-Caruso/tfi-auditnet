package ar.edu.uai.tfi.management.api.rest.dto.common;

import ar.edu.uai.tfi.management.api.rest.dto.bitacora.RegistroBitacoraSistemaResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** GET /api/admin/security: usuarios por estado, cuentas bloqueadas y eventos de seguridad de las últimas 24 h. */
public record ResumenSeguridadResponse(
        long usuariosActivos,
        long usuariosDeshabilitados,
        long cuentasBloqueadas,
        Map<String, Long> eventosUltimas24h,
        List<RegistroBitacoraSistemaResponse> ultimosRechazos,
        Instant desde
) {
}
