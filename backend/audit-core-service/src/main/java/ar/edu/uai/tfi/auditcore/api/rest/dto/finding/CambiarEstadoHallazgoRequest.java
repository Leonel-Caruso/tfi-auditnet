package ar.edu.uai.tfi.auditcore.api.rest.dto.finding;

/**
 * Cuerpo de PATCH /api/findings/{id}/status. Ejemplo: {"estado":"RESUELTO","comentario":"Se aplicó ssh v2"}.
 * El comentario es obligatorio para RESUELTO, ACEPTADO y para reabrir (ABIERTO).
 */
public record CambiarEstadoHallazgoRequest(String estado, String comentario) {
}
