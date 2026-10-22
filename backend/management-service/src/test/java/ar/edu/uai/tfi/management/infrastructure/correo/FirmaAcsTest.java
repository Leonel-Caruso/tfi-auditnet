package ar.edu.uai.tfi.management.infrastructure.correo;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Firma de los pedidos a Azure Communication Services, comparada con un cálculo hecho aparte. */
class FirmaAcsTest {

    private static final String CLAVE = "Y2xhdmUtZGUtcHJ1ZWJhLWF1ZGl0bmV0LTAxMjM0NTY3ODk=";

    @Test
    void firmaMetodoRutaFechaHostYHashDelCuerpo() {
        URI uri = URI.create("https://auditnet-correo.unitedstates.communication.azure.com"
                + EnvioCorreoAdapter.API_ACS);
        // Hora argentina: la fecha firmada se pasa a GMT.
        ZonedDateTime ahora = ZonedDateTime.of(2026, 10, 22, 9, 5, 9, 0, ZoneId.of("America/Argentina/Buenos_Aires"));

        Map<String, String> encabezados = FirmaAcs.encabezados("POST", uri,
                "{\"senderAddress\":\"DoNotReply@ejemplo.azurecomm.net\"}", CLAVE, ahora);

        assertEquals("Thu, 22 Oct 2026 12:05:09 GMT", encabezados.get("x-ms-date"));
        assertEquals("UOsLnwSMkRZQ3sxdOnoQ/ikJgInkOM6bQezGJzD2W6o=", encabezados.get("x-ms-content-sha256"));
        assertEquals("HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256"
                + "&Signature=/LnPl1LOMCg49gIme3YLKSpDgq2pf0rCVI3j4AV63e4=", encabezados.get("Authorization"));
    }

    @Test
    void laCadenaDeConexionSeCortaEnElPrimerIgual() {
        Map<String, String> partes = ConexionAcs.leer("endpoint=https://acs.communication.azure.com/;accesskey=" + CLAVE);

        assertEquals("https://acs.communication.azure.com/", partes.get("endpoint"));
        assertEquals(CLAVE, partes.get("accesskey"));
        assertThrows(IllegalStateException.class, () -> ConexionAcs.leer("endpoint=https://acs/"));
    }
}
