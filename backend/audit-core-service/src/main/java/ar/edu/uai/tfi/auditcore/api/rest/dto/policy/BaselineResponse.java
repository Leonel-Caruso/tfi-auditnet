package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

/**
 * @param auditorias cantidad de auditorías que usaron esta versión (si es mayor a 0, sus reglas están congeladas)
 */
public record BaselineResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer version,
        Long tipoDispositivoId,
        Long organizacionId,
        String estado,
        long auditorias
) {
}
