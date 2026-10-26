package ar.edu.uai.tfi.management.domain.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Tema de la consola que eligió el usuario. */
public enum Tema {
    CLARO("claro"),
    OSCURO("oscuro");

    private final String codigo;

    Tema(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<Tema> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String codigo = valor.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(tema -> tema.codigo.equals(codigo)).findFirst();
    }
}
