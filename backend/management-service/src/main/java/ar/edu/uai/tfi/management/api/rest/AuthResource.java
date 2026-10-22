package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.auth.AvisosCuentaDto;
import ar.edu.uai.tfi.management.api.rest.dto.auth.CambiarPasswordRequest;
import ar.edu.uai.tfi.management.api.rest.dto.auth.LoginRequest;
import ar.edu.uai.tfi.management.api.rest.dto.auth.LoginResponse;
import ar.edu.uai.tfi.management.api.rest.dto.auth.RecuperarPasswordRequest;
import ar.edu.uai.tfi.management.api.rest.dto.auth.RestablecerConEnlaceRequest;
import ar.edu.uai.tfi.management.api.rest.dto.auth.UsuarioActualResponse;
import ar.edu.uai.tfi.management.application.auth.AuthResult;
import ar.edu.uai.tfi.management.application.auth.AuthService;
import ar.edu.uai.tfi.management.application.auth.RecuperacionContrasenaService;
import ar.edu.uai.tfi.management.domain.model.PreferenciasAviso;
import io.quarkus.security.Authenticated;
import jakarta.json.JsonNumber;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.Set;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    static final String MENSAJE_RECUPERACION = "Si los datos corresponden a una cuenta activa, en unos minutos vas a "
            + "recibir un correo con el enlace para crear una contraseña nueva.";

    private final AuthService authService;
    private final RecuperacionContrasenaService recuperacion;
    private final JsonWebToken jwt;

    public AuthResource(AuthService authService, RecuperacionContrasenaService recuperacion, JsonWebToken jwt) {
        this.authService = authService;
        this.recuperacion = recuperacion;
        this.jwt = jwt;
    }

    @POST
    @Path("/login")
    @PermitAll
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aLoginResponse(authService.login(request.identidad(), request.password()));
    }

    private static LoginResponse aLoginResponse(AuthResult result) {
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

    /** El usuario cambia su contraseña. Responde un token nuevo: las demás sesiones abiertas quedan sin efecto. */
    @POST
    @Path("/password")
    @Authenticated
    public LoginResponse cambiarPassword(CambiarPasswordRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        return aLoginResponse(authService.cambiarPasswordPropia(usuarioActual(), request.actual(), request.nueva()));
    }

    /** Responde lo mismo exista o no la cuenta, para no revelar qué usuarios hay. */
    @POST
    @Path("/password-recovery")
    @PermitAll
    public Response solicitarRecuperacion(RecuperarPasswordRequest request) {
        recuperacion.solicitar(request == null ? null : request.identidad());
        return Response.status(Response.Status.ACCEPTED).entity(Map.of("mensaje", MENSAJE_RECUPERACION)).build();
    }

    @POST
    @Path("/password-reset")
    @PermitAll
    public Response restablecer(RestablecerConEnlaceRequest request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud es obligatorio.");
        }
        recuperacion.restablecer(request.token(), request.password());
        return Response.noContent().build();
    }

    @GET
    @Path("/me/notifications")
    @Authenticated
    public AvisosCuentaDto avisos() {
        return AvisosCuentaDto.de(authService.preferencias(usuarioActual()));
    }

    @PUT
    @Path("/me/notifications")
    @Authenticated
    public AvisosCuentaDto guardarAvisos(AvisosCuentaDto request) {
        if (request == null || request.alertas() == null || request.vencimientos() == null
                || request.resumen() == null) {
            throw new BadRequestException("Indicá si querés recibir cada tipo de aviso.");
        }
        return AvisosCuentaDto.de(authService.guardarPreferencias(usuarioActual(),
                new PreferenciasAviso(request.alertas(), request.vencimientos(), request.resumen())));
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

    private Long usuarioActual() {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ForbiddenException("La sesión no identifica a un usuario válido.");
        }
    }
}
