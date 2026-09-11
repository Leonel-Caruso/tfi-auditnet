package ar.edu.uai.tfi.auditcore.api.rest.dto.device;

public record DispositivoResponse(
        Long id,
        String nombre,
        String identificador,
        Long tipoDispositivoId,
        String fabricante,
        Long organizacionId,
        Long sedeId,
        String criticidad,
        String estado
) {
}
