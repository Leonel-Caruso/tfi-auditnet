package ar.edu.uai.tfi.management.api.rest.dto.user;

import java.time.Instant;
import java.util.Set;

/**
 * @param bloqueadoHasta fin del bloqueo por intentos fallidos (null si no está bloqueado)
 * @param ultimoAcceso   último inicio de sesión exitoso (null si nunca ingresó)
 */
public record UsuarioResponse(
        Long id,
        Long organizationId,
        String nombre,
        String email,
        String nombreUsuario,
        String estado,
        Set<String> roles,
        Instant bloqueadoHasta,
        Instant ultimoAcceso
) {
}
