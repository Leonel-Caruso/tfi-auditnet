package ar.edu.uai.tfi.management.api.rest.dto.auth;

import java.util.Set;

public record UsuarioActualResponse(String username, String subject, Long organizationId, Set<String> roles) {
}
