package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/**
 * Criterios de consulta de hallazgos (CU-005-001). Todos son opcionales (null = sin filtrar).
 *
 * @param organizacionId limita a una organización (usuarios no administradores)
 */
public record FiltroHallazgos(
        Long organizacionId,
        EstadoHallazgo estado,
        SeveridadRegla severidad,
        Long dispositivoId,
        Long baselineId,
        Long auditoriaId,
        Instant desde,
        Instant hasta
) {
    public static FiltroHallazgos todos(Long organizacionId) {
        return new FiltroHallazgos(organizacionId, null, null, null, null, null, null, null);
    }
}
