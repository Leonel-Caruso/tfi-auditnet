package ar.edu.uai.tfi.management.api.rest.dto.auth;

/** POST /api/auth/password-recovery: usuario o correo de la cuenta. */
public record RecuperarPasswordRequest(String identidad) {
}
