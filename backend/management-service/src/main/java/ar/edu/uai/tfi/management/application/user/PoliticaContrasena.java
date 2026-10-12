package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;

import java.util.Locale;

/**
 * Al menos 10 caracteres, con letras y números, y sin el nombre de usuario. Se aplica al crear un usuario, al
 * restablecer y al cambiar la contraseña.
 */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 10;
    public static final int LONGITUD_MAXIMA = 72; // límite de BCrypt

    private PoliticaContrasena() {
    }

    public static void validar(String password, String nombreUsuario) {
        if (password == null || password.isBlank()) {
            throw invalida("La contraseña es obligatoria.");
        }
        if (password.length() < LONGITUD_MINIMA) {
            throw invalida("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        if (password.length() > LONGITUD_MAXIMA) {
            throw invalida("La contraseña no puede superar los " + LONGITUD_MAXIMA + " caracteres.");
        }
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            throw invalida("La contraseña debe combinar letras y números.");
        }
        if (nombreUsuario != null && !nombreUsuario.isBlank()
                && password.toLowerCase(Locale.ROOT).contains(nombreUsuario.trim().toLowerCase(Locale.ROOT))) {
            throw invalida("La contraseña no puede contener el nombre de usuario.");
        }
    }

    private static ExcepcionAplicacion invalida(String mensaje) {
        return new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, mensaje);
    }
}
