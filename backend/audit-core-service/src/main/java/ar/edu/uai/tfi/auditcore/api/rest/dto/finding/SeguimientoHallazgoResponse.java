package ar.edu.uai.tfi.auditcore.api.rest.dto.finding;

import java.time.Instant;

public record SeguimientoHallazgoResponse(
        Long id,
        Long hallazgoId,
        String estadoAnterior,
        String estadoNuevo,
        String comentario,
        String usuario,
        Instant fecha
) {
}
