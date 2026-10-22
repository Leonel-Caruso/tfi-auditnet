package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.correo.CorreoResponse;
import ar.edu.uai.tfi.management.api.rest.dto.correo.EstadoCorreoResponse;
import ar.edu.uai.tfi.management.application.correo.CorreoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Bandeja de salida de correo: estado y envío manual de los pendientes. */
@Path("/api/admin/mail")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class CorreoAdminResource {

    private final CorreoService correos;

    public CorreoAdminResource(CorreoService correos) {
        this.correos = correos;
    }

    @GET
    public EstadoCorreoResponse estado() {
        CorreoService.EstadoBandeja estado = correos.estado();
        return new EstadoCorreoResponse(estado.proveedor(), estado.pendientes(), estado.enviados24h(),
                estado.fallidos24h(), estado.ultimos().stream().map(CorreoResponse::de).toList());
    }

    @POST
    @Path("/dispatch")
    public CorreoService.ResultadoDespacho despachar() {
        return correos.despachar();
    }
}
