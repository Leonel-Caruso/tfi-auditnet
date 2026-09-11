package ar.edu.uai.tfi.auditcore.api.rest.dto.device;

public record TipoDispositivoResponse(
        Long id,
        String nombre,
        String fabricante,
        String familia,
        boolean activo
) {
}
