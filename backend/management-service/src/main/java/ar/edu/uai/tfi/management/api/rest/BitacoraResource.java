package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.bitacora.RegistroBitacoraSistemaResponse;
import ar.edu.uai.tfi.management.application.bitacora.BitacoraSistemaService;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Consulta de bitácoras para el panel de administración. Solo lectura y solo administradores.
 * Ejemplo: GET /api/admin/bitacoras/sistema?evento=LOGIN_RECHAZADO&desde=2026-09-28T00:00:00Z&limite=50
 */
@Path("/api/admin/bitacoras")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class BitacoraResource {

    private final BitacoraSistemaService bitacoraSistema;

    public BitacoraResource(BitacoraSistemaService bitacoraSistema) {
        this.bitacoraSistema = bitacoraSistema;
    }

    @GET
    @Path("/sistema")
    public List<RegistroBitacoraSistemaResponse> sistema(@QueryParam("evento") String evento,
                                                         @QueryParam("resultado") String resultado,
                                                         @QueryParam("actor") String actor,
                                                         @QueryParam("desde") String desde,
                                                         @QueryParam("hasta") String hasta,
                                                         @QueryParam("limite") Integer limite) {
        return bitacoraSistema.consultar(evento, resultado, actor, desde, hasta, limite)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    private RegistroBitacoraSistemaResponse aResponse(RegistroBitacoraSistema registro) {
        return new RegistroBitacoraSistemaResponse(
                registro.id(),
                registro.fecha(),
                registro.tipo().name(),
                registro.resultado().name(),
                registro.actor(),
                registro.usuarioAfectadoId(),
                registro.organizacionId(),
                registro.origenIp(),
                registro.userAgent(),
                registro.correlacion(),
                registro.detalle()
        );
    }
}
