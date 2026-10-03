package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/**
 * Criterios de consulta del historial de auditorías (CU-005-002). Todos son opcionales.
 *
 * @param severidadMaxima NINGUNA, BAJA, MEDIA, ALTA o CRITICA
 * @param resultado       CUMPLE o CON_HALLAZGOS
 * @param ejecutadoPor    usuario que ejecutó la auditoría (coincidencia exacta, sin distinguir mayúsculas)
 */
public record FiltroAuditorias(
        Long organizacionId,
        Long dispositivoId,
        Long baselineId,
        String severidadMaxima,
        String resultado,
        String ejecutadoPor,
        Instant desde,
        Instant hasta
) {
    public static FiltroAuditorias delDispositivo(Long organizacionId, Long dispositivoId) {
        return new FiltroAuditorias(organizacionId, dispositivoId, null, null, null, null, null, null);
    }
}
