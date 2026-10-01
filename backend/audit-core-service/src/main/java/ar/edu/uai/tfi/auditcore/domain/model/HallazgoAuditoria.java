package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

public record HallazgoAuditoria(
        Long id,
        Long auditoriaId,
        Long reglaId,
        Long dispositivoId,
        Long baselineId,
        Long organizacionId,
        String codigoRegla,
        String nombreRegla,
        TipoReglaConfiguracion tipoRegla,
        String patron,
        SeveridadRegla severidad,
        String evidencia,
        String recomendacion,
        String impacto,
        EstadoHallazgo estado,
        Instant fechaDeteccion
) {
}
