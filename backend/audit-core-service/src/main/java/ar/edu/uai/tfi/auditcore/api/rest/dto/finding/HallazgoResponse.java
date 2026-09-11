package ar.edu.uai.tfi.auditcore.api.rest.dto.finding;

import java.time.Instant;

public record HallazgoResponse(
        Long id,
        Long auditoriaId,
        Long reglaId,
        Long dispositivoId,
        Long baselineId,
        Long organizacionId,
        String codigoRegla,
        String nombreRegla,
        String tipoRegla,
        String patron,
        String severidad,
        String evidencia,
        String recomendacion,
        String estado,
        Instant fechaDeteccion
) {
}
