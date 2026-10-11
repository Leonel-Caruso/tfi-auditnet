package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.bitacora.RegistroBitacoraSistemaResponse;
import ar.edu.uai.tfi.management.api.rest.dto.common.ResumenSeguridadResponse;
import ar.edu.uai.tfi.management.application.admin.SaludService;
import ar.edu.uai.tfi.management.application.admin.SaludService.SaludServicio;
import ar.edu.uai.tfi.management.application.admin.SeguridadResumenService;
import ar.edu.uai.tfi.management.application.admin.SeguridadResumenService.ResumenSeguridad;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Panel de administración (Puntos Mandatorios: "panel de administrador web: seguridad, bitácoras, salud de
 * servicios"). La salud de audit-core-service se consulta en ese servicio (GET /api/system/health).
 */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class AdminResource {

    private final SaludService saludService;
    private final SeguridadResumenService seguridadService;

    public AdminResource(SaludService saludService, SeguridadResumenService seguridadService) {
        this.saludService = saludService;
        this.seguridadService = seguridadService;
    }

    @GET
    @Path("/health")
    public SaludServicio salud() {
        return saludService.verificar();
    }

    @GET
    @Path("/security")
    public ResumenSeguridadResponse seguridad() {
        ResumenSeguridad resumen = seguridadService.resumen(Instant.now());
        Map<String, Long> eventos = new LinkedHashMap<>();
        resumen.eventosUltimas24h().forEach((tipo, cantidad) -> eventos.put(tipo.name(), cantidad));
        return new ResumenSeguridadResponse(
                resumen.usuariosActivos(),
                resumen.usuariosDeshabilitados(),
                resumen.cuentasBloqueadas(),
                eventos,
                resumen.ultimosRechazos().stream().map(RegistroBitacoraSistemaResponse::de).toList(),
                resumen.desde());
    }
}
