package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.ResultadoReglaAuditoria;

import java.util.List;

public record AuditoriaDetalle(
        AuditoriaConfiguracion auditoria,
        List<ResultadoReglaAuditoria> evaluaciones,
        List<HallazgoAuditoria> hallazgos
) {
}
