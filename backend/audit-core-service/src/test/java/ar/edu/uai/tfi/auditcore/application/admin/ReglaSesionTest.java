package ar.edu.uai.tfi.auditcore.application.admin;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** audit-core aplica la misma verificación de sesión que management. */
class ReglaSesionTest {

    private static final Instant CAMBIO = Instant.parse("2026-10-11T12:00:00Z");

    @Test
    void sesionValidaCuandoUsuarioYOrganizacionEstanActivos() {
        assertTrue(ReglaSesion.evaluar(cuenta("ACTIVO", "ACTIVO", null), false, 1).isEmpty());
        assertTrue(ReglaSesion.evaluar(cuenta("ACTIVO", "ACTIVO", CAMBIO), false, CAMBIO.getEpochSecond()).isEmpty());
    }

    @Test
    void rechazaUsuarioInexistenteODeshabilitado() {
        assertEquals("El usuario fue deshabilitado.", ReglaSesion.evaluar(null, true, 1).orElseThrow());
        assertEquals("El usuario fue deshabilitado.",
                ReglaSesion.evaluar(cuenta("DESHABILITADO", "ACTIVO", null), true, 1).orElseThrow());
    }

    @Test
    void organizacionDeshabilitadaSoloAfectaAlUsuarioDelCliente() {
        assertTrue(ReglaSesion.evaluar(cuenta("ACTIVO", "DESHABILITADO", null), false, 1).isPresent());
        assertTrue(ReglaSesion.evaluar(cuenta("ACTIVO", "DESHABILITADO", null), true, 1).isEmpty());
    }

    @Test
    void tokenEmitidoAntesDeUnCambioDeCuentaQuedaSinEfecto() {
        assertEquals("La sesión se cerró por un cambio en la cuenta.",
                ReglaSesion.evaluar(cuenta("ACTIVO", "ACTIVO", CAMBIO), true, CAMBIO.getEpochSecond() - 1).orElseThrow());
    }

    private static ReglaSesion.DatosCuenta cuenta(String usuario, String organizacion, Instant desde) {
        return new ReglaSesion.DatosCuenta(usuario, organizacion, desde);
    }
}
