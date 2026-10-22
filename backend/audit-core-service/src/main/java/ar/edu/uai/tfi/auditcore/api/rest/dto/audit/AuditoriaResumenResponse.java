package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;

import java.time.Instant;

public record AuditoriaResumenResponse(
        Long id,
        Long configuracionId,
        Long dispositivoId,
        Long baselineId,
        Long organizacionId,
        Instant fechaEjecucion,
        String ejecutadoPor,
        Integer totalReglas,
        Integer reglasCumplidas,
        Integer totalHallazgos,
        String severidadMaxima,
        String resultado,
        String estado,
        String origen
) {

    public static AuditoriaResumenResponse de(AuditoriaConfiguracion auditoria) {
        return new AuditoriaResumenResponse(
                auditoria.id(),
                auditoria.configuracionId(),
                auditoria.dispositivoId(),
                auditoria.baselineId(),
                auditoria.organizacionId(),
                auditoria.fechaEjecucion(),
                auditoria.ejecutadoPor(),
                auditoria.totalReglas(),
                auditoria.reglasCumplidas(),
                auditoria.totalHallazgos(),
                auditoria.severidadMaxima(),
                auditoria.resultado(),
                auditoria.estado().name(),
                auditoria.origen() == null ? "MANUAL" : auditoria.origen().name()
        );
    }
}
