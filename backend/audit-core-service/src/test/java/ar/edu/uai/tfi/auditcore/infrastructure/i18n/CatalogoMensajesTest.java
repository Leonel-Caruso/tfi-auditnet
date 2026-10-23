package ar.edu.uai.tfi.auditcore.infrastructure.i18n;

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
        assertEquals("The requested finding does not exist.", catalogo.traducir("No existe el hallazgo solicitado.", "en"));
        assertEquals("The baseline Hardening v2 was already used in 3 audits: its rules are not modified to preserve the evidence. Create a new version of the baseline and modify the rules there.",
                catalogo.traducir("La baseline Hardening v2 ya se usó en 3 auditorías: sus reglas no se modifican para conservar la evidencia. Generá una nueva versión de la baseline y modificá las reglas allí.", "en"));
        assertEquals("Não é possível passar de RESUELTO para EN_REVISION. Um achado RESUELTO só pode passar para [ABIERTO].",
                catalogo.traducir("No se puede pasar de RESUELTO a EN_REVISION. Un hallazgo RESUELTO solo puede pasar a [ABIERTO].", "pt"));
        assertEquals("The file could not be saved in the repository (HTTP 503).",
                catalogo.traducir("No se pudo guardar el archivo en el repositorio (HTTP 503).", "en"));
    }

    @Test
    void enEspanolOSinTraduccionDevuelveElMensajeOriginal() {
        assertEquals("No existe el hallazgo solicitado.", catalogo.traducir("No existe el hallazgo solicitado.", "es"));
        assertEquals("Mensaje que no está en el catálogo.", catalogo.traducir("Mensaje que no está en el catálogo.", "en"));
        assertEquals("No existe el hallazgo solicitado.", catalogo.traducir("No existe el hallazgo solicitado.", "fr"));
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
