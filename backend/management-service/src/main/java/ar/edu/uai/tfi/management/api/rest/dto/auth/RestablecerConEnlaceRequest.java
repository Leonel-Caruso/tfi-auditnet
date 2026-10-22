package ar.edu.uai.tfi.management.api.rest.dto.auth;

/** POST /api/auth/password-reset: token recibido por correo y la contraseña nueva. */
public record RestablecerConEnlaceRequest(String token, String password) {
}
