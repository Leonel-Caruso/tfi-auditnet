package ar.edu.uai.tfi.auditcore.api.rest.dto.dashboard;

import ar.edu.uai.tfi.auditcore.api.rest.dto.report.ReporteResumenResponse;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.EstadoCliente;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.RiesgoDispositivo;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.RiesgoSede;

import java.util.List;

/**
 * GET /api/client-organizations/{organizationId}/audit-status. Los nombres de la organización y de las sedes
 * los da management-service; acá van sus ids.
 */
public record EstadoClienteResponse(
        Long organizacionId,
        ResumenTableroResponse resumen,
        List<RiesgoSede> sedes,
        List<RiesgoDispositivo> dispositivos,
        List<ReporteResumenResponse> reportes
) {

    public static EstadoClienteResponse de(EstadoCliente estado) {
        return new EstadoClienteResponse(
                estado.organizacionId(),
                ResumenTableroResponse.de(estado.resumen()),
                estado.sedes(),
                estado.dispositivos(),
                estado.reportes().stream().map(ReporteResumenResponse::de).toList()
        );
    }
}
