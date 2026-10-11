package ar.edu.uai.tfi.auditcore.api.rest.dto.dashboard;

import ar.edu.uai.tfi.auditcore.api.rest.dto.audit.AuditoriaResumenResponse;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.IndicadoresAlertas;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.IndicadoresAuditorias;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.IndicadoresDispositivos;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.IndicadoresHallazgos;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.PuntoEvolucion;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.ResumenTablero;
import ar.edu.uai.tfi.auditcore.application.dashboard.TableroService.RiesgoDispositivo;

import java.time.Instant;
import java.util.List;

/** GET /api/dashboard/summary (CU-006-003). Los indicadores se calculan en el momento; nada se guarda. */
public record ResumenTableroResponse(
        Instant generado,
        String nivelRiesgo,
        IndicadoresDispositivos dispositivos,
        IndicadoresHallazgos hallazgos,
        IndicadoresAlertas alertas,
        IndicadoresAuditorias auditorias,
        List<AuditoriaResumenResponse> auditoriasRecientes,
        List<RiesgoDispositivo> dispositivosEnRiesgo,
        List<PuntoEvolucion> evolucion
) {

    public static ResumenTableroResponse de(ResumenTablero resumen) {
        return new ResumenTableroResponse(
                resumen.generado(),
                resumen.nivelRiesgo().name(),
                resumen.dispositivos(),
                resumen.hallazgos(),
                resumen.alertas(),
                resumen.auditorias(),
                resumen.auditoriasRecientes().stream().map(AuditoriaResumenResponse::de).toList(),
                resumen.dispositivosEnRiesgo(),
                resumen.evolucion()
        );
    }
}
