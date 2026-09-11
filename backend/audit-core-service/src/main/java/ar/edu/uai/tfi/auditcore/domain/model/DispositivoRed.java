package ar.edu.uai.tfi.auditcore.domain.model;

public record DispositivoRed(
        Long id,
        String nombre,
        String identificador,
        Long tipoDispositivoId,
        String fabricante,
        Long organizacionId,
        Long sedeId,
        CriticidadDispositivo criticidad,
        EstadoDispositivo estado
) {
}
