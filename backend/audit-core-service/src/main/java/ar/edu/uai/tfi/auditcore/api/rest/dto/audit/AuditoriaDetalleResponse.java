package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoResponse;

import java.util.List;

/**
 * @param configuracion configuración evaluada (null solo si faltara por un error previo)
 * @param baseline      baseline usada como referencia (ídem)
 */
public record AuditoriaDetalleResponse(
        AuditoriaResumenResponse auditoria,
        List<EvaluacionReglaResponse> evaluaciones,
        List<HallazgoResponse> hallazgos,
        ConfiguracionResumenResponse configuracion,
        BaselineResumenResponse baseline
) {
}
