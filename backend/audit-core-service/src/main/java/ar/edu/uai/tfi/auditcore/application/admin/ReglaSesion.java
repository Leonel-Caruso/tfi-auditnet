package ar.edu.uai.tfi.auditcore.application.admin;

import java.time.Instant;
import java.util.Optional;

/**
 * Verificación de la sesión en cada solicitud (Bloque 6, D-31). Un token firmado sigue siendo válido hasta que
 * vence; esta regla lo deja sin efecto en el acto si:
 * <ul>
 *   <li>el usuario ya no existe o fue deshabilitado;</li>
 *   <li>su organización fue deshabilitada (salvo para el administrador de la plataforma);</li>
 *   <li>el token se emitió antes de un cambio de contraseña, roles u organización.</li>
 * </ul>
 * Copia de la regla de management-service: los dos servicios la aplican sobre la base compartida (no hay un
 * módulo común entre servicios).
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
