package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.ResultadoReglaAuditoria;

import java.util.List;

/** Auditoría con sus evaluaciones, hallazgos, la configuración evaluada y la baseline usada. */
public record AuditoriaDetalle(
        AuditoriaConfiguracion auditoria,
        List<ResultadoReglaAuditoria> evaluaciones,
        List<HallazgoAuditoria> hallazgos,
        ConfiguracionDispositivo configuracion,
        BaselineConfiguracion baseline
) {
}
