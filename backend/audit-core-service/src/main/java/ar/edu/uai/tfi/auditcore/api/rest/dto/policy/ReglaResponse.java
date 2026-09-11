package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

public record ReglaResponse(
        Long id,
        Long baselineId,
        String codigo,
        String nombre,
        String descripcion,
        String tipo,
        String patron,
        String severidad,
        String recomendacion,
        String estado
) {
}
