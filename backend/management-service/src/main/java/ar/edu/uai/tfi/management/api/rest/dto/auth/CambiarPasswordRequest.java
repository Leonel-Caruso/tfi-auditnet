package ar.edu.uai.tfi.management.api.rest.dto.auth;

/** POST /api/auth/password: el usuario autenticado cambia su contraseña. */
public record CambiarPasswordRequest(String actual, String nueva) {
}
