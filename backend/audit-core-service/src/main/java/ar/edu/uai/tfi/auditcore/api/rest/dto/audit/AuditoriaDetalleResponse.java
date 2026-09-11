package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoResponse;

import java.util.List;

public record AuditoriaDetalleResponse(
        AuditoriaResumenResponse auditoria,
        List<EvaluacionReglaResponse> evaluaciones,
        List<HallazgoResponse> hallazgos
) {
}
