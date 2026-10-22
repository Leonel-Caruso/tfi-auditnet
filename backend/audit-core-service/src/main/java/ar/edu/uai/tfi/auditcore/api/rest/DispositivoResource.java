package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.CambiarEstadoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.device.CrearDispositivoRequest;
import ar.edu.uai.tfi.auditcore.api.rest.dto.device.DispositivoResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.device.ModificarDispositivoRequest;
import ar.edu.uai.tfi.auditcore.application.device.DispositivoService;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

@Path("/api/devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({
        "ADMINISTRADOR_SISTEMA",
        "ADMINISTRADOR_ORGANIZACION",
        "ANALISTA_RED",
        "AUDITOR_TECNICO",
        "RESPONSABLE_GESTION_IT"
})
public class DispositivoResource {
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    private static final String ADMIN_ORGANIZACION = "ADMINISTRADOR_ORGANIZACION";

    private final DispositivoService service;
    private final JsonWebToken jwt;

    public DispositivoResource(DispositivoService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    public List<DispositivoResponse> listar() {
        List<DispositivoRed> dispositivos = esAdministrador()
                ? service.listar()
                : service.listarPorOrganizacion(organizationIdActual());

        return dispositivos.stream()
                .map(this::aResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public DispositivoResponse buscar(@PathParam("id") Long id) {
        try {
            DispositivoRed dispositivo = esAdministrador()
                    ? service.buscarPorId(id)
                    : service.buscarPorIdYOrganizacion(id, organizationIdActual());
            return aResponse(dispositivo);
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        }
    }

    /** El administrador de una organización registra dispositivos solo en la suya. */
    @POST
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION})
    public Response crear(CrearDispositivoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        Long alcance = UsuarioAutenticado.organizacion(jwt);
        if (alcance != null && !alcance.equals(request.organizacionId())) {
            throw new ForbiddenException("Solo podés registrar dispositivos en tu organización.");
        }

        try {
            DispositivoRed creado = service.crear(
                    request.nombre(),
                    request.identificador(),
                    request.tipoDispositivoId(),
                    request.fabricante(),
                    request.organizacionId(),
                    request.sedeId(),
                    request.criticidad(),
                    jwt.getName()
            );

            return Response.created(URI.create("/api/devices/" + creado.id()))
                    .entity(aResponse(creado))
                    .build();
        } catch (IllegalStateException exception) {
            throw error(Response.Status.CONFLICT, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    /** Modifica los datos del dispositivo. Registra MODIFICACION con valor anterior y nuevo. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION})
    public DispositivoResponse modificar(@PathParam("id") Long id, ModificarDispositivoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        verificarAlcance(id);
        return ejecutar(() -> service.modificar(
                id,
                request.nombre(),
                request.identificador(),
                request.tipoDispositivoId(),
                request.fabricante(),
                request.sedeId(),
                request.criticidad(),
                jwt.getName()
        ));
    }

    /** Baja lógica ({"estado":"INACTIVO"}) o reactivación ({"estado":"ACTIVO"}). */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({ADMIN, ADMIN_ORGANIZACION})
    public DispositivoResponse cambiarEstado(@PathParam("id") Long id, CambiarEstadoRequest request) {
        if (request == null) {
            throw error(Response.Status.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        verificarAlcance(id);
        return ejecutar(() -> service.cambiarEstado(id, request.estado(), jwt.getName()));
    }

    /** Un dispositivo de otra organización responde 404, igual que en la consulta. */
    private void verificarAlcance(Long id) {
        Long alcance = UsuarioAutenticado.organizacion(jwt);
        if (alcance != null) {
            ejecutar(() -> service.buscarPorIdYOrganizacion(id, alcance));
        }
    }

    private DispositivoResponse ejecutar(Supplier<DispositivoRed> operacion) {
        try {
            return aResponse(operacion.get());
        } catch (IllegalStateException exception) {
            throw error(Response.Status.CONFLICT, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw error(Response.Status.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw error(Response.Status.BAD_REQUEST, exception.getMessage());
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

    private DispositivoResponse aResponse(DispositivoRed dispositivo) {
        return new DispositivoResponse(
                dispositivo.id(),
                dispositivo.nombre(),
                dispositivo.identificador(),
                dispositivo.tipoDispositivoId(),
                dispositivo.fabricante(),
                dispositivo.organizacionId(),
                dispositivo.sedeId(),
                dispositivo.criticidad().name(),
                dispositivo.estado().name()
        );
    }

    private WebApplicationException error(Response.Status status, String message) {
        Response response = Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build();
        return new WebApplicationException(response);
    }
}
