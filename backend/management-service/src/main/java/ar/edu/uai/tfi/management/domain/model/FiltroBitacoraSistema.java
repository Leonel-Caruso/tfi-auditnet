package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/**
 * Criterios de consulta de la bitácora de auditoría de sistema. Los campos null no filtran.
 */
public record FiltroBitacoraSistema(
        TipoEventoSistema tipo,
        ResultadoEvento resultado,
        String actor,
        Instant desde,
        Instant hasta,
        int limite
) {
}
