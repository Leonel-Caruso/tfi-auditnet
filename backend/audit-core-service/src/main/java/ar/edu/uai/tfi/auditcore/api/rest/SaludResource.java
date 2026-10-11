package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.application.admin.SaludService;
import ar.edu.uai.tfi.auditcore.application.admin.SaludService.SaludServicio;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Salud de audit-core-service para el panel de administración: versión, tiempo en línea, memoria, PostgreSQL
 * (versión del esquema) y MongoDB. La ruta no empieza con /api/admin porque el gateway deriva esas rutas a
 * management-service.
 */
@Path("/api/system/health")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class SaludResource {

    private final SaludService service;

    public SaludResource(SaludService service) {
        this.service = service;
    }

    @GET
    public SaludServicio salud() {
        return service.verificar();
    }
}
