package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;
import ar.edu.uai.tfi.management.domain.model.EstadoCorreo;
import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Asunto, enlaces, marca y escapado de los datos en el HTML. */
class PlantillasCorreoTest {

    private final PlantillasCorreo plantillas = new PlantillasCorreo();

    @Test
    void alertasEscapaLosDatosYLlevaLaMarca() {
        MensajeCorreo mensaje = armar(TipoCorreo.ALERTAS_CRITICAS, Map.of("dispositivo", "RTR-01",
                "dispositivoNombre", "Router <b>core</b>", "alertas", 2, "criticas", 1,
                "reglas", List.of(Map.of("codigo", "SEC-SSH-01", "nombre", "<script>x</script>", "severidad", "CRITICA"))));

        assertEquals("2 alertas nuevas en RTR-01", mensaje.asunto());
        assertTrue(mensaje.html().contains("&lt;script&gt;x&lt;/script&gt;"));
        assertFalse(mensaje.html().contains("<script>"));
        assertTrue(mensaje.html().contains("#f57c00") && mensaje.html().contains("#171a1f"), "Colores de AuditNet");
        assertTrue(mensaje.html().contains("https://auditnet.test/alerts"));
        assertTrue(mensaje.texto().contains("severidad crítica"));
        assertTrue(mensaje.texto().contains("ACME S.A."), "El pie dice por qué llega el aviso");
    }

    @Test
    void recuperacionLlevaElEnlaceConElToken() {
        MensajeCorreo mensaje = armar(TipoCorreo.RECUPERACION_CONTRASENA,
                Map.of("usuario", "ana", "token", "a+b/c", "minutos", 30));

        assertTrue(mensaje.texto().contains("https://auditnet.test/reset-password?token=a%2Bb%2Fc"), mensaje.texto());
        assertTrue(mensaje.texto().contains("30 minutos"));
        assertTrue(mensaje.texto().startsWith("Restablecer tu contraseña"));
    }

    @Test
    void elAltaNoMuestraCodigosDePerfil() {
        MensajeCorreo mensaje = armar(TipoCorreo.ALTA_USUARIO,
                Map.of("usuario", "ana", "perfiles", List.of("ANALISTA_RED")));

        assertTrue(mensaje.texto().contains("Perfil: Analista de redes"));
        assertFalse(mensaje.texto().contains("ANALISTA_RED"));
    }

    @Test
    void elResumenMensualListaLosIndicadores() {
        MensajeCorreo mensaje = armar(TipoCorreo.RESUMEN_MENSUAL, Map.of("periodo", "2026-09",
                "auditorias", 12, "detectados", 30, "cerrados", 18, "pendientes", 12, "vencidos", 3,
                "alertasVigentes", 2, "nivelRiesgo", "MEDIO"));

        assertEquals("Resumen de septiembre de 2026 · ACME S.A.", mensaje.asunto());
        assertTrue(mensaje.texto().contains("Hallazgos pendientes al cierre: 12 (vencidos: 3)"));
        assertTrue(mensaje.texto().contains("Nivel de riesgo actual: medio"));
    }

    @Test
    void enInglesYPortuguesCambianLosTextosPeroNoLosDatos() {
        Map<String, Object> datos = Map.of("dispositivo", "RTR-01", "dispositivoNombre", "Router", "alertas", 2,
                "criticas", 1, "reglas", List.of(Map.of("codigo", "SEC-SSH-01", "nombre", "SSH v2", "severidad", "ALTA")));

        MensajeCorreo ingles = armar(TipoCorreo.ALERTAS_CRITICAS, datos, Idioma.EN);
        assertEquals("2 new alerts on RTR-01", ingles.asunto());
        assertTrue(ingles.texto().contains("2 alerts, 1 of critical severity"), ingles.texto());
        assertTrue(ingles.texto().contains("SEC-SSH-01 · SSH v2 (high severity)"));
        assertTrue(ingles.html().startsWith("<!doctype html>\n<html lang=\"en\">"));

        MensajeCorreo portugues = armar(TipoCorreo.ALERTAS_CRITICAS, datos, Idioma.PT);
        assertEquals("2 novos alertas em RTR-01", portugues.asunto());
        assertTrue(portugues.texto().contains("severidade alta"));
    }

    @Test
    void elPeriodoDelResumenSaleEnElIdiomaDelDestinatario() {
        Map<String, Object> datos = Map.of("periodo", "2026-09", "nivelRiesgo", "ALTO");

        assertEquals("September 2026 summary · ACME S.A.", armar(TipoCorreo.RESUMEN_MENSUAL, datos, Idioma.EN).asunto());
        assertEquals("Resumo de setembro de 2026 · ACME S.A.",
                armar(TipoCorreo.RESUMEN_MENSUAL, datos, Idioma.PT).asunto());
        assertTrue(armar(TipoCorreo.RESUMEN_MENSUAL, datos, Idioma.PT).texto().contains("Nível de risco atual: alto"));
    }

    @Test
    void losTresIdiomasTienenLosMismosTextos() {
        assertEquals(TextosCorreo.de(Idioma.ES).claves(), TextosCorreo.de(Idioma.EN).claves());
        assertEquals(TextosCorreo.de(Idioma.ES).claves(), TextosCorreo.de(Idioma.PT).claves());
    }

    @Test
    void unDatoConLlavesNoSeReemplazaDosVeces() {
        MensajeCorreo mensaje = armar(TipoCorreo.ALTA_USUARIO, Map.of("usuario", "{organizacion}"));
        assertTrue(mensaje.texto().contains("Se creó tu usuario {organizacion} en ACME S.A."), mensaje.texto());
    }

    private MensajeCorreo armar(TipoCorreo tipo, Map<String, Object> datos) {
        return armar(tipo, datos, Idioma.ES);
    }

    private MensajeCorreo armar(TipoCorreo tipo, Map<String, Object> datos, Idioma idioma) {
        CorreoSaliente correo = new CorreoSaliente(1L, "prueba", tipo, 2L, "ana@acme.test", "Ana", datos,
                EstadoCorreo.PENDIENTE, 0, Instant.EPOCH, Instant.EPOCH, null, null, null);
        return plantillas.armar(correo, "ACME S.A.", "https://auditnet.test", idioma);
    }
}
