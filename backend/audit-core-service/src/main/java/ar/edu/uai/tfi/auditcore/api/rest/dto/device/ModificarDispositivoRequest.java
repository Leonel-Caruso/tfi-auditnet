package ar.edu.uai.tfi.auditcore.api.rest.dto.device;

/** Cuerpo de PUT /api/devices/{id}. La organización y el estado no se modifican por esta vía. */
public record ModificarDispositivoRequest(
        String nombre,
        String identificador,
        Long tipoDispositivoId,
        String fabricante,
        Long sedeId,
        String criticidad
) {
}
