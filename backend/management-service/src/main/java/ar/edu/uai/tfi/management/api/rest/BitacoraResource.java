package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.bitacora.RegistroBitacoraSistemaResponse;
import ar.edu.uai.tfi.management.api.rest.dto.bitacora.RegistroExcepcionResponse;
import ar.edu.uai.tfi.management.api.rest.dto.bitacora.RegistroTransaccionResponse;
import ar.edu.uai.tfi.management.application.bitacora.BitacoraExcepcionesService;
import ar.edu.uai.tfi.management.application.bitacora.BitacoraSistemaService;
import ar.edu.uai.tfi.management.application.bitacora.BitacoraTransaccionesService;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroExcepcion;
import ar.edu.uai.tfi.management.domain.model.RegistroTransaccion;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Consulta de bitácoras para el panel de administración. Solo lectura y solo administradores.
 * Ejemplos:
 *   GET /api/admin/bitacoras/sistema?evento=LOGIN_RECHAZADO&desde=2026-09-28T00:00:00Z&limite=50
 *   GET /api/admin/bitacoras/transacciones?entidad=DISPOSITIVO&entidadId=4
 *   GET /api/admin/bitacoras/excepciones?servicio=audit-core-service&nivel=ERROR
 */
@Path("/api/admin/bitacoras")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class BitacoraResource {

    private final BitacoraSistemaService bitacoraSistema;
    private final BitacoraTransaccionesService bitacoraTransacciones;
    private final BitacoraExcepcionesService bitacoraExcepciones;

    public BitacoraResource(BitacoraSistemaService bitacoraSistema,
                            BitacoraTransaccionesService bitacoraTransacciones,
                            BitacoraExcepcionesService bitacoraExcepciones) {
        this.bitacoraSistema = bitacoraSistema;
        this.bitacoraTransacciones = bitacoraTransacciones;
        this.bitacoraExcepciones = bitacoraExcepciones;
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

    @GET
    @Path("/transacciones")
    public List<RegistroTransaccionResponse> transacciones(@QueryParam("entidad") String entidad,
                                                           @QueryParam("entidadId") Long entidadId,
                                                           @QueryParam("operacion") String operacion,
                                                           @QueryParam("actor") String actor,
                                                           @QueryParam("organizacionId") Long organizacionId,
                                                           @QueryParam("desde") String desde,
                                                           @QueryParam("hasta") String hasta,
                                                           @QueryParam("limite") Integer limite) {
        return bitacoraTransacciones
                .consultar(entidad, entidadId, operacion, actor, organizacionId, desde, hasta, limite)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    @GET
    @Path("/excepciones")
    public List<RegistroExcepcionResponse> excepciones(@QueryParam("servicio") String servicio,
                                                       @QueryParam("nivel") String nivel,
                                                       @QueryParam("desde") String desde,
                                                       @QueryParam("hasta") String hasta,
                                                       @QueryParam("limite") Integer limite) {
        return bitacoraExcepciones.consultar(servicio, nivel, desde, hasta, limite)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    private RegistroExcepcionResponse aResponse(RegistroExcepcion registro) {
        return new RegistroExcepcionResponse(
                registro.fecha(),
                registro.servicio(),
                registro.nivel().name(),
                registro.tipo(),
                registro.mensaje(),
                registro.traza(),
                registro.metodoHttp(),
                registro.ruta(),
                registro.estadoHttp(),
                registro.usuario(),
                registro.correlacion()
        );
    }

    private RegistroTransaccionResponse aResponse(RegistroTransaccion registro) {
        return new RegistroTransaccionResponse(
                registro.id(),
                registro.fecha(),
                registro.servicio(),
                registro.entidad(),
                registro.entidadId(),
                registro.operacion().name(),
                registro.actor(),
                registro.organizacionId(),
                registro.valorAnterior(),
                registro.valorNuevo(),
                registro.correlacion(),
                registro.detalle()
        );
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
