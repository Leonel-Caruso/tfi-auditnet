package ar.edu.uai.tfi.auditcore.domain.model;

public record ResultadoReglaAuditoria(
        Long id,
        Long auditoriaId,
        Long reglaId,
        String codigoRegla,
        String nombreRegla,
        TipoReglaConfiguracion tipo,
        String patron,
        SeveridadRegla severidad,
        Boolean cumple,
        String evidencia
) {
}
