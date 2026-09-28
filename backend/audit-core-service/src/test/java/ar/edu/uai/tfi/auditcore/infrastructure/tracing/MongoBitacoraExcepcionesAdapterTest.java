package ar.edu.uai.tfi.auditcore.infrastructure.tracing;

import ar.edu.uai.tfi.auditcore.domain.model.NivelExcepcion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bloque 2.3c · Nivel configurable de la bitácora de excepciones.
 */
class MongoBitacoraExcepcionesAdapterTest {

    private MongoBitacoraExcepcionesAdapter conNivel(String nivel) {
        return new MongoBitacoraExcepcionesAdapter(null, null, null, null, nivel);
    }

    @Test
    void nivelErrorGuardaSoloFallasInesperadas() {
        MongoBitacoraExcepcionesAdapter adapter = conNivel("ERROR");
        assertTrue(adapter.debePersistir(NivelExcepcion.ERROR));
        assertFalse(adapter.debePersistir(NivelExcepcion.WARN));
    }

    @Test
    void nivelWarnGuardaTambienErroresDeUso() {
        MongoBitacoraExcepcionesAdapter adapter = conNivel(" warn ");
        assertTrue(adapter.debePersistir(NivelExcepcion.ERROR));
        assertTrue(adapter.debePersistir(NivelExcepcion.WARN));
    }

    @Test
    void nivelOffNoGuardaNada() {
        MongoBitacoraExcepcionesAdapter adapter = conNivel("OFF");
        assertFalse(adapter.debePersistir(NivelExcepcion.ERROR));
        assertFalse(adapter.debePersistir(NivelExcepcion.WARN));
    }
}
