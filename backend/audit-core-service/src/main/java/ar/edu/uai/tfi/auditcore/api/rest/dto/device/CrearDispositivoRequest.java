package ar.edu.uai.tfi.auditcore.api.rest.dto.device;

public record CrearDispositivoRequest(
        String nombre,
        String identificador,
        Long tipoDispositivoId,
        String fabricante,
        Long organizacionId,
        Long sedeId,
        String criticidad
) {
}
