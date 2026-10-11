package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/**
 * Reporte de auditoría generado (RF-011, CU-006-001). Se guarda una sola vez y no se modifica.
 *
 * @param organizacionId null si el administrador lo generó para todas las organizaciones
 * @param contenido      null en los listados (solo se carga al consultar un reporte)
 */
public record ReporteAuditoria(
        Long id,
        Long organizacionId,
        String titulo,
        CriteriosReporte criterios,
        ContenidoReporte contenido,
        int totalAuditorias,
        int totalHallazgos,
        String generadoPor,
        Instant fechaGeneracion
) {
}
