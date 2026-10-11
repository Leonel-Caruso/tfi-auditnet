package ar.edu.uai.tfi.auditcore.api.rest.dto.report;

import java.util.List;

/**
 * POST /api/reports/audits. Fechas en ISO-8601. Obligatorio: desde y hasta, o auditorías específicas.
 *
 * @param organizacionId solo el administrador puede elegirla (sin ella, todas las organizaciones)
 */
public record GenerarReporteRequest(
        Long organizacionId,
        String desde,
        String hasta,
        List<Long> dispositivoIds,
        List<String> severidades,
        List<String> estados,
        List<Long> auditoriaIds
) {
}
