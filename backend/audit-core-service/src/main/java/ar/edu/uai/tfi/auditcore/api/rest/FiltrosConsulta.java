package ar.edu.uai.tfi.auditcore.api.rest;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/** Conversión y validación de los filtros que llegan por query string (400 si son inválidos). */
final class FiltrosConsulta {

    private FiltrosConsulta() {
    }

    static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El valor '" + valor + "' no es válido para el filtro '" + campo + "'.");
        }
    }

    static String opcion(String valor, Set<String> permitidos, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(normalizado)) {
            throw new IllegalArgumentException("El filtro '" + campo + "' admite: " + String.join(", ", permitidos) + ".");
        }
        return normalizado;
    }

    static Instant fecha(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("El filtro '" + campo
                    + "' debe tener formato ISO-8601, por ejemplo 2026-10-01T00:00:00Z.");
        }
    }

    static Long id(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(valor.trim());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException exception) {
            // se informa abajo
        }
        throw new IllegalArgumentException("El filtro '" + campo + "' debe ser un identificador válido.");
    }

    static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
