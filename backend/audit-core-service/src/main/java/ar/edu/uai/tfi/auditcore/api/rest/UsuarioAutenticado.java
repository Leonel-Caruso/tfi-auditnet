package ar.edu.uai.tfi.auditcore.api.rest;

import jakarta.json.JsonNumber;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Alcance del usuario autenticado según su token (perfiles y organización). */
final class UsuarioAutenticado {

    static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private UsuarioAutenticado() {
    }

    static boolean esAdministrador(JsonWebToken jwt) {
        return jwt.getGroups() != null && jwt.getGroups().contains(ADMIN);
    }

    /** Organización del usuario; null para el administrador (ve todas). */
    static Long organizacion(JsonWebToken jwt) {
        if (esAdministrador(jwt)) {
            return null;
        }
        JsonNumber organizationId = jwt.getClaim("organizationId");
        if (organizationId == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return organizationId.longValue();
    }

    /**
     * Organización a consultar: el administrador puede elegir cualquiera (o null = todas); el resto solo la
     * propia, y pedir otra responde 403.
     */
    static Long organizacionSolicitada(JsonWebToken jwt, Long solicitada) {
        Long propia = organizacion(jwt);
        if (propia == null) {
            return solicitada;
        }
        if (solicitada != null && !solicitada.equals(propia)) {
            throw new ForbiddenException("Solo podés consultar la información de tu organización.");
        }
        return propia;
    }
}
