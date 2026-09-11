package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.api.rest.dto.common.SystemStatusResponse;
import ar.edu.uai.tfi.management.application.SystemStatus;
import ar.edu.uai.tfi.management.application.SystemStatusService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/management/status")
@Produces(MediaType.APPLICATION_JSON)
public class SystemStatusResource {
    private final SystemStatusService service;

    public SystemStatusResource(SystemStatusService service) {
        this.service = service;
    }

    @GET
    @PermitAll
    public SystemStatusResponse status() {
        SystemStatus status = service.getStatus();
        return new SystemStatusResponse(status.service(), status.status(), status.delivery());
    }
}
