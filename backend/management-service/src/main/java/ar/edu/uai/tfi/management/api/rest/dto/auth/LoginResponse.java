package ar.edu.uai.tfi.management.api.rest.dto.auth;

import java.util.Set;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        Long organizationId,
        String nombre,
        String username,
        String email,
        Set<String> roles
) {
}
