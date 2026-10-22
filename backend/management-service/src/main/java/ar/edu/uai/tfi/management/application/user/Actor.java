package ar.edu.uai.tfi.management.application.user;

import java.util.Set;

/**
 * Usuario autenticado que hace la operación, tomado del token.
 *
 * @param id             id del usuario
 * @param organizacionId organización a la que pertenece
 * @param roles          perfiles del token
 */
public record Actor(Long id, Long organizacionId, Set<String> roles) {

    public Actor {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean administradorSistema() {
        return roles.contains(MatrizPermisos.ADMIN);
    }

    public boolean administradorOrganizacion() {
        return roles.contains(MatrizPermisos.ADMIN_ORGANIZACION);
    }

    /** Organización a la que se limita lo que puede ver y modificar; null si alcanza a toda la plataforma. */
    public Long alcance() {
        return administradorSistema() ? null : organizacionId;
    }
}
