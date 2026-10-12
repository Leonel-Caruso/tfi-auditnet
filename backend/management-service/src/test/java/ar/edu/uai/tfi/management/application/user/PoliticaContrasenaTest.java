package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticaContrasenaTest {

    @Test
    void aceptaUnaContrasenaQueCumpleLaPolitica() {
        PoliticaContrasena.validar("Auditoria2026", "ana");
    }

    @Test
    void rechazaLasQueNoCumplen() {
        assertTrue(rechazo(null, "ana").contains("obligatoria"));
        assertTrue(rechazo("Corta1", "ana").contains("10 caracteres"));
        assertTrue(rechazo("SoloLetrasLargas", "ana").contains("letras y números"));
        assertTrue(rechazo("1234567890", "ana").contains("letras y números"));
        assertTrue(rechazo("x".repeat(72) + "1", "ana").contains("72"));
        assertTrue(rechazo("MiClaveANA2026", "ana").contains("nombre de usuario"));
    }

    private static String rechazo(String password, String usuario) {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> PoliticaContrasena.validar(password, usuario));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
        return error.getMessage();
    }
}
