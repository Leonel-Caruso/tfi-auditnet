package ar.edu.uai.tfi.auditcore.domain.model;

public record BaselineConfiguracion(
        Long id,
        String nombre,
        String descripcion,
        Integer version,
        Long tipoDispositivoId,
        Long organizacionId,
        EstadoBaseline estado
) {
}
