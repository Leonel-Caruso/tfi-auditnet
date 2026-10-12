package ar.edu.uai.tfi.management.application.auth;

import java.time.Instant;
import java.util.Optional;

/**
 * Un token firmado vale hasta que vence; esta regla lo anula en el acto si el usuario ya no existe o fue
 * deshabilitado, si su organización fue deshabilitada (salvo para el administrador) o si se emitió antes de un
 * cambio de contraseña, roles u organización. audit-core-service tiene una copia de esta clase.
 */
public final class ReglaSesion {

    private ReglaSesion() {
    }

    /** Datos de la cuenta leídos de la base; {@code null} si el usuario no existe. */
    public record DatosCuenta(String estadoUsuario, String estadoOrganizacion, Instant sesionesValidasDesde) {
    }

    /** @return el motivo del rechazo, o vacío si la sesión sigue siendo válida */
    public static Optional<String> evaluar(DatosCuenta cuenta, boolean administrador, long emitidoEnSegundos) {
        if (cuenta == null || !"ACTIVO".equals(cuenta.estadoUsuario())) {
            return Optional.of("El usuario fue deshabilitado.");
        }
        if (!administrador && !"ACTIVO".equals(cuenta.estadoOrganizacion())) {
            return Optional.of("La organización del usuario fue deshabilitada.");
        }
        if (cuenta.sesionesValidasDesde() != null && emitidoEnSegundos < cuenta.sesionesValidasDesde().getEpochSecond()) {
            return Optional.of("La sesión se cerró por un cambio en la cuenta.");
        }
        return Optional.empty();
    }
}
