package ar.edu.uai.tfi.auditcore.application;

public record SystemStatus(
        String service,
        String status,
        String delivery
) {
}
