package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

public record EvaluacionReglaResponse(
        Long id,
        Long auditoriaId,
        Long reglaId,
        String codigoRegla,
        String nombreRegla,
        String tipo,
        String patron,
        String severidad,
        Boolean cumple,
        String evidencia
) {
}
