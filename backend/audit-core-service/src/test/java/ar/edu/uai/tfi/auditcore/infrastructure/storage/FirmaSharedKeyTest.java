package ar.edu.uai.tfi.auditcore.infrastructure.storage;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Firma Shared Key de Blob Storage y lectura de la cadena de conexión. */
class FirmaSharedKeyTest {

    @Test
    void armaElTextoAFirmarSegunLaEspecificacion() {
        Map<String, String> encabezados = new LinkedHashMap<>();
        encabezados.put("Content-Type", "text/plain; charset=utf-8");
        encabezados.put("Content-Length", "12");
        encabezados.put("If-None-Match", "*");
        encabezados.put("x-ms-version", "2021-08-06");
        encabezados.put("x-ms-date", "Sun, 11 Oct 2026 22:00:00 GMT");
        encabezados.put("x-ms-blob-type", "BlockBlob");
        encabezados.put("x-ms-meta-sha256", " abc ");

        String texto = FirmaSharedKey.textoAFirmar("cuenta", "PUT",
                URI.create("https://cuenta.blob.core.windows.net/auditnet-archivos/configuraciones/CFG-001-v1.cfg"), encabezados);

        assertEquals("PUT\n\n\n12\n\ntext/plain; charset=utf-8\n\n\n\n*\n\n\n"
                + "x-ms-blob-type:BlockBlob\nx-ms-date:Sun, 11 Oct 2026 22:00:00 GMT\nx-ms-meta-sha256:abc\nx-ms-version:2021-08-06\n"
                + "/cuenta/auditnet-archivos/configuraciones/CFG-001-v1.cfg", texto);
    }

    @Test
    void largoCeroVaVacioYLosParametrosSeOrdenan() {
        Map<String, String> encabezados = new LinkedHashMap<>();
        encabezados.put("Content-Length", "0");
        encabezados.put("x-ms-date", "d");

        String texto = FirmaSharedKey.textoAFirmar("devstoreaccount1", "GET",
                URI.create("http://127.0.0.1:10000/devstoreaccount1/auditnet-archivos?restype=container&comp=list"), encabezados);

        assertTrue(texto.startsWith("GET\n\n\n\n"), texto);
        assertTrue(texto.endsWith("/devstoreaccount1/devstoreaccount1/auditnet-archivos\ncomp:list\nrestype:container"), texto);
    }

    @Test
    void laFirmaUsaLaCuentaYEsDeterminista() {
        byte[] clave = new byte[32];
        Map<String, String> encabezados = Map.of("x-ms-date", "d", "x-ms-version", "v");
        URI uri = URI.create("https://c.blob.core.windows.net/x/y");

        String firma = FirmaSharedKey.autorizacion("c", clave, "GET", uri, encabezados);

        assertTrue(firma.startsWith("SharedKey c:"));
        assertEquals(firma, FirmaSharedKey.autorizacion("c", clave, "GET", uri, encabezados));
        assertNotEquals(firma, FirmaSharedKey.autorizacion("c", new byte[]{1}, "GET", uri, encabezados));
    }

    @Test
    void leeLaCadenaDeConexionDeAzureYLaDelEmulador() {
        ConexionBlob azure = ConexionBlob.desde("DefaultEndpointsProtocol=https;AccountName=stauditnet;AccountKey=AAAA;EndpointSuffix=core.windows.net");
        assertEquals("stauditnet", azure.cuenta());
        assertEquals("https://stauditnet.blob.core.windows.net/auditnet-archivos/reportes/REP-001.csv",
                azure.uri("auditnet-archivos", "reportes/REP-001.csv", null).toString());

        ConexionBlob azurite = ConexionBlob.desde(ConexionBlob.AZURITE);
        assertEquals("http://127.0.0.1:10000/devstoreaccount1/auditnet-archivos?restype=container",
                azurite.uri("auditnet-archivos", null, "restype=container").toString());
        assertEquals(64, azurite.clave().length);

        assertThrows(IllegalArgumentException.class, () -> ConexionBlob.desde("AccountName=x"));
        assertThrows(IllegalArgumentException.class, () -> ConexionBlob.desde("AccountName=x;AccountKey=%%%"));
    }
}
