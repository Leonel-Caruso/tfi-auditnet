package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

public record AuditoriaConfiguracion(
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
        EstadoAuditoria estado
) {
}
