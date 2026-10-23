package ar.edu.uai.tfi.management.infrastructure.i18n;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Catálogo de mensajes: los tres idiomas en cada entrada, partes variables y mensajes sin traducción. */
class CatalogoMensajesTest {

    private final CatalogoMensajes catalogo = CatalogoMensajes.desdeRecursos();

    @Test
    void traduceMensajesFijosYConPartesVariables() {
        assertEquals("Incorrect username or password.", catalogo.traducir("Usuario o contraseña incorrectos.", "en"));
        assertEquals("The password must be at least 10 characters long.",
                catalogo.traducir("La contraseña debe tener al menos 10 caracteres.", "en"));
        assertEquals("O plano Piloto permite até 5 usuários ativos e a organização já tem 5. Para adicionar mais, peça uma mudança de plano em Meu plano.",
                catalogo.traducir("El plan Piloto permite hasta 5 usuarios activos y la organización ya tiene 5. Para sumar más, pedí un cambio de plan desde Mi plan.", "pt"));
        assertEquals("The account is locked after 5 failed attempts. Try again in 1 minute or ask the administrator to unlock it.",
                catalogo.traducir("La cuenta está bloqueada por 5 intentos fallidos. Probá de nuevo en 1 minuto o pedile al administrador que la desbloquee.", "en"));
    }

    @Test
    void enEspanolOSinTraduccionDevuelveElMensajeOriginal() {
        assertEquals("Usuario o contraseña incorrectos.", catalogo.traducir("Usuario o contraseña incorrectos.", "es"));
        assertEquals("Mensaje que no está en el catálogo.", catalogo.traducir("Mensaje que no está en el catálogo.", "en"));
        assertEquals("Usuario o contraseña incorrectos.", catalogo.traducir("Usuario o contraseña incorrectos.", "fr"));
    }

    @Test
    void elIdiomaSaleDelPrimeroDeAcceptLanguage() {
        assertEquals("en", CatalogoMensajes.idiomaPedido("en-US,en;q=0.9,es;q=0.8"));
        assertEquals("pt", CatalogoMensajes.idiomaPedido("pt-BR"));
        assertEquals("es", CatalogoMensajes.idiomaPedido("fr-FR"));
        assertEquals("es", CatalogoMensajes.idiomaPedido(null));
    }

    @Test
    void rechazaEntradasIncompletasOConVariablesDistintas() {
        assertThrows(IllegalStateException.class, () -> leer("es: Hola {0}.\nen: Hello {0}.\n"));
        assertThrows(IllegalStateException.class, () -> leer("es: Hola {0}.\nen: Hello {1}.\npt: Olá {0}.\n"));
        assertEquals("Hello Ana.", leer("es: Hola {0}.\nen: Hello {0}.\npt: Olá {0}.\n").traducir("Hola Ana.", "en"));
    }

    private static CatalogoMensajes leer(String texto) {
        try {
            return CatalogoMensajes.leer(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
