package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.storage.ArchivoResponse;
import ar.edu.uai.tfi.auditcore.api.rest.dto.storage.EstadoRepositorioResponse;
import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService;
import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService.EstadoRepositorio;
import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService.ResultadoSincronizacion;
import ar.edu.uai.tfi.auditcore.application.storage.RespaldoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Repositorio de archivos y respaldos para el panel de administración. Las rutas usan /api/system porque
 * el gateway deriva /api/admin a management-service.
 */
@Path("/api/system")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class RepositorioArchivosResource {

    private final RepositorioArchivosService repositorio;
    private final RespaldoService respaldos;
    private final JsonWebToken jwt;

    public RepositorioArchivosResource(RepositorioArchivosService repositorio, RespaldoService respaldos,
                                       JsonWebToken jwt) {
        this.repositorio = repositorio;
        this.respaldos = respaldos;
        this.jwt = jwt;
    }

    @GET
    @Path("/storage")
    public EstadoRepositorioResponse estado() {
        EstadoRepositorio estado = repositorio.estado();
        Map<String, EstadoRepositorioResponse.Totales> archivos = new LinkedHashMap<>();
        estado.totales().forEach((tipo, totales) ->
                archivos.put(tipo.name(), new EstadoRepositorioResponse.Totales(totales[0], totales[1])));
        return new EstadoRepositorioResponse(estado.configurado(), estado.contenedor(), archivos,
                estado.configuracionesPendientes(), estado.reportesPendientes());
    }

    /** Sube los archivos de configuraciones y reportes que todavía no están en el repositorio. */
    @POST
    @Path("/storage/sync")
    public ResultadoSincronizacion sincronizar() {
        return repositorio.sincronizarPendientes(jwt.getName());
    }

    @GET
    @Path("/backups")
    public List<ArchivoResponse> listarRespaldos() {
        return respaldos.listar().stream().map(ArchivoResponse::de).toList();
    }

    @POST
    @Path("/backups")
    public Response generarRespaldo() {
        ArchivoResponse respaldo = ArchivoResponse.de(respaldos.generar(jwt.getName()));
        return Response.created(URI.create("/api/system/backups/" + respaldo.id())).entity(respaldo).build();
    }

    @GET
    @Path("/backups/{id}/download")
    @Produces({"application/zip", MediaType.APPLICATION_JSON})
    public Response descargarRespaldo(@PathParam("id") Long id) {
        try {
            RepositorioArchivosService.ArchivoVerificado archivo = respaldos.descargar(id);
            return Response.ok(archivo.contenido())
                    .type("application/zip")
                    .header("Content-Disposition", "attachment; filename=\"" + archivo.archivo().nombre() + "\"")
                    .header("X-AuditNet-SHA256", archivo.archivo().sha256())
                    .header("X-AuditNet-Integridad", "VERIFICADO")
                    .build();
        } catch (NoSuchElementException exception) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("message", exception.getMessage()))
                    .build());
        }
    }
}
