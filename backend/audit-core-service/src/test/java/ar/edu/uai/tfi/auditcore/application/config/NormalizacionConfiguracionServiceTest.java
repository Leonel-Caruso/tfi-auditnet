package ar.edu.uai.tfi.auditcore.application.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NormalizacionConfiguracionServiceTest {

    private final NormalizacionConfiguracionService service = new NormalizacionConfiguracionService();

    @Test
    void normalizaSaltosYEspaciosFinalesSinPerderContenido() {
        String original = "\r\nhostname RTR-CORE-01   \r\n ip ssh version 2\r\n\r\n";

        String normalizada = service.normalizar(original);

        assertEquals("hostname RTR-CORE-01\n ip ssh version 2", normalizada);
    }

    @Test
    void rechazaConfiguracionVacia() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizar("   \r\n  "));
    }
}
