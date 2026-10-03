package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

import java.util.List;
import java.util.Map;

/**
 * Comparación de una auditoría con la anterior del mismo dispositivo.
 *
 * @param anterior null si es la primera auditoría del dispositivo
 * @param resumen  cantidad de reglas por categoría
 */
public record ComparacionAuditoriaResponse(
        AuditoriaResumenResponse actual,
        AuditoriaResumenResponse anterior,
        List<CambioReglaResponse> cambios,
        Map<String, Long> resumen
) {
}
