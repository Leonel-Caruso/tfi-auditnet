package ar.edu.uai.tfi.management.api.rest.dto.user;

import java.util.Set;

/** PUT /api/users/{id}. El nombre de usuario no se modifica (identidad en las bitácoras). */
public record ModificarUsuarioRequest(Long organizationId, String nombre, String email, Set<String> roles) {
}
