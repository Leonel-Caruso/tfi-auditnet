package ar.edu.uai.tfi.auditcore.api.rest.dto;

public record SystemStatusResponse(
        String service,
        String status,
        String delivery
) {
}
