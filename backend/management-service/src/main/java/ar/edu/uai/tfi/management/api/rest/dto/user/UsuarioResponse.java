package ar.edu.uai.tfi.management.api.rest.dto.user;

import java.util.Set;

public record UsuarioResponse(
        Long id,
        Long organizationId,
        String nombre,
        String email,
        String nombreUsuario,
        String estado,
        Set<String> roles
) {
}
