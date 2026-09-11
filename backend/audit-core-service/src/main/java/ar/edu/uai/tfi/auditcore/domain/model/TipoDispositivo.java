package ar.edu.uai.tfi.auditcore.domain.model;

public record TipoDispositivo(
        Long id,
        String nombre,
        String fabricante,
        String familia,
        boolean activo
) {
}
