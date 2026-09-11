package ar.edu.uai.tfi.management.domain.model;

import java.util.Set;

public record Usuario(
        Long id,
        Long organizacionId,
        String nombre,
        String email,
        String nombreUsuario,
        String passwordHash,
        EstadoRegistro estado,
        Set<String> roles
) {
}
