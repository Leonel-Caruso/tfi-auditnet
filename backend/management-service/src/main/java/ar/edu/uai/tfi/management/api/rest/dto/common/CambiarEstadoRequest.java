package ar.edu.uai.tfi.management.api.rest.dto.common;

/** PATCH .../status de usuarios, organizaciones y sedes: ACTIVO o DESHABILITADO (baja lógica). */
public record CambiarEstadoRequest(String estado) {
}
