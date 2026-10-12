package ar.edu.uai.tfi.auditcore.application.admin;

import java.time.Instant;
import java.util.Optional;

/**
 * Invalida en el acto un token todavía vigente si el usuario o su organización fueron deshabilitados (la
 * organización no aplica al administrador de la plataforma), o si se emitió antes de un cambio de contraseña, roles
 * u organización. Es copia de la regla de management-service: no hay un módulo común entre servicios.
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
