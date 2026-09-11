package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

public record CrearBaselineRequest(
        String nombre,
        String descripcion,
        Long tipoDispositivoId,
        Long organizacionId
) {
}
