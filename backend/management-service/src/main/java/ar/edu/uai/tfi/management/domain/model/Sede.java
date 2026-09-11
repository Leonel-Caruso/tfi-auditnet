package ar.edu.uai.tfi.management.domain.model;

public record Sede(
        Long id,
        Long organizacionId,
        String nombre,
        String ubicacion,
        EstadoRegistro estado
) {
}
