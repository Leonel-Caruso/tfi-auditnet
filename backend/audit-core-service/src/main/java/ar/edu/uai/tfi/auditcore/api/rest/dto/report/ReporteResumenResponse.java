package ar.edu.uai.tfi.auditcore.api.rest.dto.report;

import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;

import java.time.Instant;

/** Reporte en un listado (sin contenido). */
public record ReporteResumenResponse(
        Long id,
        Long organizacionId,
        String titulo,
        int totalAuditorias,
        int totalHallazgos,
        String generadoPor,
        Instant fechaGeneracion
) {

    public static ReporteResumenResponse de(ReporteAuditoria reporte) {
        return new ReporteResumenResponse(reporte.id(), reporte.organizacionId(), reporte.titulo(),
                reporte.totalAuditorias(), reporte.totalHallazgos(), reporte.generadoPor(), reporte.fechaGeneracion());
    }
}
