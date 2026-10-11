package ar.edu.uai.tfi.management.api.rest.dto.user;

/** POST /api/users/{id}/password: contraseña nueva asignada por el administrador. */
public record RestablecerPasswordRequest(String password) {
}
