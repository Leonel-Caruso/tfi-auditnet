package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/**
 * Desvío detectado en una auditoría. Regla, severidad, evidencia e impacto son una copia del momento de la
 * detección y no se modifican. Solo cambia el estado de seguimiento ({@code fechaEstado} y
 * {@code usuarioEstado} registran el último cambio; null si nunca cambió).
 */
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
        Instant fechaDeteccion,
        Instant fechaEstado,
        String usuarioEstado
) {
}
