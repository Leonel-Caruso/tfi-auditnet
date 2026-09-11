package ar.edu.uai.tfi.management.api.rest.dto.user;

import java.util.Set;

public record CrearUsuarioRequest(
        Long organizationId,
        String nombre,
        String email,
        String nombreUsuario,
        String password,
        Set<String> roles
) {
}
