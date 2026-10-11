package ar.edu.uai.tfi.auditcore.api.rest.dto.report;

import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte;
import ar.edu.uai.tfi.auditcore.domain.model.CriteriosReporte;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;

import java.time.Instant;

/** Reporte completo: criterios con que se generó y contenido (foto fija). */
public record ReporteResponse(
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

    public static ReporteResponse de(ReporteAuditoria reporte) {
        return new ReporteResponse(reporte.id(), reporte.organizacionId(), reporte.titulo(), reporte.criterios(),
                reporte.contenido(), reporte.totalAuditorias(), reporte.totalHallazgos(), reporte.generadoPor(),
                reporte.fechaGeneracion());
    }
}
