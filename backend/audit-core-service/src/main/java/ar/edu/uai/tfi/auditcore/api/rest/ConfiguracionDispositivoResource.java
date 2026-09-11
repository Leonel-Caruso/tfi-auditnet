package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.config.ConfiguracionResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.config.ImportarConfiguracionRequest;
import ar.edu.uai.tfi.auditcore.application.config.ConfiguracionService;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("/api/devices/{deviceId}/configurations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ANALISTA_RED",
        "AUDITOR_TECNICO"
})
public class ConfiguracionDispositivoResource {

    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private final ConfiguracionService service;
    private final JsonWebToken jwt;

    public ConfiguracionDispositivoResource(ConfiguracionService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @POST
    public Response importar(
            @PathParam("deviceId") Long deviceId,
            ImportarConfiguracionRequest request
    ) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }

        try {
            ConfiguracionDispositivo creada = esAdministrador()
                    ? service.importar(
                            deviceId,
                            request.contenido(),
                            request.formato(),
                            request.nombreFuente(),
                            jwt.getName()
                    )
                    : service.importarParaOrganizacion(
                            deviceId,
                            organizationIdActual(),
                            request.contenido(),
                            request.formato(),
                            request.nombreFuente(),
                            jwt.getName()
                    );

            return Response.created(URI.create("/api/configurations/" + creada.id()))
                    .entity(aResponse(creada))
                    .build();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    private boolean esAdministrador() {
        return jwt.getGroups() != null && jwt.getGroups().contains(ADMIN);
    }

    private Long organizationIdActual() {
        JsonNumber organizationId = jwt.getClaim("organizationId");
        if (organizationId == null) {
            throw new ForbiddenException("El usuario autenticado no tiene una organización asociada.");
        }
        return organizationId.longValue();
    }

    private ConfiguracionResponse aResponse(ConfiguracionDispositivo configuracion) {
        return new ConfiguracionResponse(
                configuracion.id(),
                configuracion.dispositivoId(),
                configuracion.organizacionId(),
                configuracion.version(),
                configuracion.formato().name(),
                configuracion.nombreFuente(),
                configuracion.contenidoOriginal(),
                configuracion.contenidoNormalizado(),
                configuracion.fechaImportacion(),
                configuracion.usuarioResponsable()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(
                Response.status(status)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("message", message))
                        .build()
        );
    }
}
