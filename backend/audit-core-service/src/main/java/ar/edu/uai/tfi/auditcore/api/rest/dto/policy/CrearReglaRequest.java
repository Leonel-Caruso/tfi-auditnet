package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

/**
 * Cuerpo de POST /api/baselines/{baselineId}/rules.
 *
 * @param patron        texto buscado, o parámetro evaluado en reglas VALOR_ESPERADO
 * @param valorEsperado solo para VALOR_ESPERADO
 */
public record CrearReglaRequest(
        String codigo,
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
