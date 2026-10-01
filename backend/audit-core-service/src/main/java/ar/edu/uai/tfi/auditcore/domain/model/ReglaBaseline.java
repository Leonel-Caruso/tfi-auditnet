package ar.edu.uai.tfi.auditcore.domain.model;

/**
 * Regla de auditoría de una baseline (RF-006).
 *
 * @param patron        texto buscado (DEBE_CONTENER / NO_DEBE_CONTENER) o parámetro evaluado (VALOR_ESPERADO)
 * @param valorEsperado valor que debe tener el parámetro; solo para VALOR_ESPERADO (null en los demás tipos)
 * @param impacto       consecuencia técnica u organizacional de no cumplir la regla
 */
public record ReglaBaseline(
        Long id,
        Long baselineId,
        String codigo,
        String nombre,
        String descripcion,
        TipoReglaConfiguracion tipo,
        String patron,
        String valorEsperado,
        SeveridadRegla severidad,
        String impacto,
        String recomendacion,
        EstadoRegla estado
) {
}
