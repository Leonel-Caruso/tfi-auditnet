package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.application.user.Actor;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

/** Datos del usuario autenticado según su token (id, organización y perfiles). */
final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    static Actor actor(JsonWebToken jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ForbiddenException("La sesión no identifica a un usuario válido.");
        }
        JsonNumber organizacion = jwt.getClaim("organizationId");
        Set<String> roles = jwt.getGroups() == null ? Set.of() : jwt.getGroups();
        return new Actor(id, organizacion == null ? null : organizacion.longValue(), roles);
    }

    /**
     * Organización a la que se limita el usuario; null para el administrador del sistema (ve todas).
     * Sin organización en el token, responde 403.
     */
    static Long alcance(JsonWebToken jwt) {
        Actor actor = actor(jwt);
        if (actor.administradorSistema()) {
            return null;
        }
        if (actor.organizacionId() == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return actor.organizacionId();
    }

    /** Rechaza con 403 si la organización pedida no es la del usuario (el administrador del sistema accede a todas). */
    static void verificarOrganizacion(JsonWebToken jwt, Long organizacionId) {
        Long alcance = alcance(jwt);
        if (alcance != null && !alcance.equals(organizacionId)) {
            throw new ForbiddenException("Solo podés acceder a la información de tu organización.");
        }
    }
}
