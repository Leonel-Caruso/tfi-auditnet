package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

import java.time.Instant;

public record AuditoriaResumenResponse(
        Long id,
        Long configuracionId,
        Long dispositivoId,
        Long baselineId,
        Long organizacionId,
        Instant fechaEjecucion,
        String ejecutadoPor,
        Integer totalReglas,
        Integer reglasCumplidas,
        Integer totalHallazgos,
        String severidadMaxima,
        String resultado,
        String estado
) {
}
