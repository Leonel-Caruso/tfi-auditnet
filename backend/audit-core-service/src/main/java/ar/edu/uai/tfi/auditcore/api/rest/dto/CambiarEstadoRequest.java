package ar.edu.uai.tfi.auditcore.api.rest.dto;

/** Cuerpo de PATCH .../{id}/status. Ejemplo: {"estado":"INACTIVO"}. */
public record CambiarEstadoRequest(String estado) {
}
