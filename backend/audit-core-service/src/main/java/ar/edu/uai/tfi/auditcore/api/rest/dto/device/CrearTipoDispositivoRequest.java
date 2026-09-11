package ar.edu.uai.tfi.auditcore.api.rest.dto.device;

public record CrearTipoDispositivoRequest(
        String nombre,
        String fabricante,
        String familia
) {
}
