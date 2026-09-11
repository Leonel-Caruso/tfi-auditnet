package ar.edu.uai.tfi.auditcore.api.rest.dto.config;

public record ImportarConfiguracionRequest(
        String contenido,
        String formato,
        String nombreFuente
) {
}
