package ar.edu.uai.tfi.management.infrastructure.correo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Firma HMAC-SHA256 de Azure Communication Services: se firma el método, la ruta con la consulta, la fecha, el host y
 * el hash del cuerpo con la clave de acceso (en Base64). Así no hace falta registrar una aplicación en Entra ID.
 */
final class FirmaAcs {

    // Día siempre con dos dígitos ("Thu, 22 Oct 2026 12:00:00 GMT").
    private static final DateTimeFormatter FECHA_HTTP =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);

    private FirmaAcs() {
    }

    /** Encabezados x-ms-date, x-ms-content-sha256 y Authorization para el pedido. */
    static Map<String, String> encabezados(String metodo, URI uri, String cuerpo, String claveBase64,
                                           ZonedDateTime ahora) {
        String fecha = FECHA_HTTP.format(ahora.withZoneSameInstant(ZoneOffset.UTC));
        String hash = Base64.getEncoder().encodeToString(sha256(cuerpo.getBytes(StandardCharsets.UTF_8)));
        String host = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
        String ruta = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        String aFirmar = metodo + "\n" + ruta + "\n" + fecha + ";" + host + ";" + hash;
        Map<String, String> encabezados = new LinkedHashMap<>();
        encabezados.put("x-ms-date", fecha);
        encabezados.put("x-ms-content-sha256", hash);
        encabezados.put("Authorization", "HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256&Signature="
                + firmar(aFirmar, claveBase64));
        return encabezados;
    }

    static String firmar(String texto, String claveBase64) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(claveBase64), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo firmar el pedido a Azure Communication Services.", exception);
        }
    }

    private static byte[] sha256(byte[] datos) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(datos);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", exception);
        }
    }
}
