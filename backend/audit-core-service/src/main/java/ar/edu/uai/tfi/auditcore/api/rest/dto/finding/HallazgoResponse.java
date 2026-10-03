package ar.edu.uai.tfi.auditcore.api.rest.dto.finding;

import java.time.Instant;

/**
 * @param fechaEstado           último cambio de estado (null si nunca cambió)
 * @param usuarioEstado         quién hizo el último cambio de estado
 * @param criticidadDispositivo criticidad actual del dispositivo, usada para priorizar
 */
public record HallazgoResponse(
        Long id,
        Long auditoriaId,
        Long reglaId,
        Long dispositivoId,
        Long baselineId,
        Long organizacionId,
        String codigoRegla,
        String nombreRegla,
        String tipoRegla,
        String patron,
        String severidad,
        String evidencia,
        String recomendacion,
        String impacto,
        String estado,
        Instant fechaDeteccion,
        Instant fechaEstado,
        String usuarioEstado,
        String criticidadDispositivo
) {
}
