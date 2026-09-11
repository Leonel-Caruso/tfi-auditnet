package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.api.rest.dto.SystemStatusResponse;
import ar.edu.uai.tfi.auditcore.application.SystemStatus;
import ar.edu.uai.tfi.auditcore.application.SystemStatusService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/audit-core/status")
@Produces(MediaType.APPLICATION_JSON)
public class SystemStatusResource {

    @Inject
    SystemStatusService statusService;

    @GET
    public SystemStatusResponse getStatus() {
        SystemStatus status = statusService.getStatus();

        return new SystemStatusResponse(
                status.service(),
                status.status(),
                status.delivery()
        );
    }
}
