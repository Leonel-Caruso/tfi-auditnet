package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

/** Cuerpo de PUT /api/rules/{id}. El código y la baseline no se modifican. */
public record ModificarReglaRequest(
        String nombre,
        String descripcion,
        String tipo,
        String patron,
        String valorEsperado,
        String severidad,
        String impacto,
        String recomendacion
) {
}
