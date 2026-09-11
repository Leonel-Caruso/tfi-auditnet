package ar.edu.uai.tfi.auditcore.domain.model;

public record ReglaBaseline(
        Long id,
        Long baselineId,
        String codigo,
        String nombre,
        String descripcion,
        TipoReglaConfiguracion tipo,
        String patron,
        SeveridadRegla severidad,
        String recomendacion,
        EstadoRegla estado
) {
}
