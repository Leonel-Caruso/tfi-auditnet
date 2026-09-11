package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.auth.LoginRequest;
import ar.edu.uai.tfi.management.api.rest.dto.auth.LoginResponse;
import ar.edu.uai.tfi.management.api.rest.dto.auth.UsuarioActualResponse;
import ar.edu.uai.tfi.management.application.auth.AuthResult;
import ar.edu.uai.tfi.management.application.auth.AuthService;
import io.quarkus.security.Authenticated;
import jakarta.json.JsonNumber;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private final AuthService authService;
    private final JsonWebToken jwt;

    public AuthResource(AuthService authService, JsonWebToken jwt) {
        this.authService = authService;
        this.jwt = jwt;
    }

    @POST
    @Path("/login")
    @PermitAll
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        AuthResult result = authService.login(request.identidad(), request.password());
        var usuario = result.usuario();
        return new LoginResponse(
                result.token(),
                "Bearer",
                result.expiresIn(),
                usuario.id(),
                usuario.organizacionId(),
                usuario.nombre(),
                usuario.nombreUsuario(),
                usuario.email(),
                usuario.roles());
    }

    @GET
    @Path("/me")
    @Authenticated
    public UsuarioActualResponse me() {
        JsonNumber organizationId = jwt.getClaim("organizationId");
        Set<String> roles = jwt.getGroups();
        return new UsuarioActualResponse(
                jwt.getName(),
                jwt.getSubject(),
                organizationId == null ? null : organizationId.longValue(),
                roles);
    }
}
