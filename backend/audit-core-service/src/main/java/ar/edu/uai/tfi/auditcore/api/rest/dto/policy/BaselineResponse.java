package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

public record BaselineResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer version,
        Long tipoDispositivoId,
        Long organizacionId,
        String estado
) {
}
