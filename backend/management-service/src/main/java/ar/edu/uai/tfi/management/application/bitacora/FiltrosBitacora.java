package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Validaciones comunes a las consultas de bitácoras (fechas, enumerados y límite). */
final class FiltrosBitacora {

    static final int LIMITE_POR_DEFECTO = 100;
    static final int LIMITE_MAXIMO = 500;

    private FiltrosBitacora() {
    }

    static int limite(Integer limite) {
        if (limite == null) {
            return LIMITE_POR_DEFECTO;
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw invalido("El límite debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }
        return limite;
    }

    static Instant fecha(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor.trim());
        } catch (DateTimeParseException exception) {
            throw invalido("El campo '" + campo + "' debe tener formato ISO-8601, por ejemplo 2026-09-28T00:00:00Z.");
        }
    }

    static void rango(Instant desde, Instant hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw invalido("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
    }

    static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalido("El valor '" + valor + "' no es válido para el campo '" + campo + "'.");
        }
    }

    static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static ExcepcionAplicacion invalido(String mensaje) {
        return new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, mensaje);
    }
}
