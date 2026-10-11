package ar.edu.uai.tfi.management.api.rest.dto.user;

/** PUT /api/roles/{id}: solo la descripción (los perfiles son fijos). */
public record ModificarRolRequest(String descripcion) {
}
