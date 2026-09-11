package ar.edu.uai.tfi.management.domain.model;

public record Rol(
        Long id,
        String nombre,
        String descripcion,
        EstadoRegistro estado
) {
}
