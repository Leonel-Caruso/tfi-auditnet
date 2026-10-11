package ar.edu.uai.tfi.auditcore.infrastructure.storage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Firma "Shared Key" de las solicitudes REST a Blob Storage (documentación de Azure: "Authorize with Shared Key").
 * Se implementa directamente para no agregar el SDK de Azure (y sus dependencias) por cuatro operaciones.
 */
public final class FirmaSharedKey {

    private FirmaSharedKey() {
    }

    /**
     * @param encabezados encabezados de la solicitud (Content-Type, Content-Length y todos los x-ms-*)
     * @return el valor del encabezado Authorization
     */
    public static String autorizacion(String cuenta, byte[] clave, String verbo, URI uri, Map<String, String> encabezados) {
        String texto = textoAFirmar(cuenta, verbo, uri, encabezados);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            String firma = Base64.getEncoder().encodeToString(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
            return "SharedKey " + cuenta + ":" + firma;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo firmar la solicitud al almacenamiento.", exception);
        }
    }

    static String textoAFirmar(String cuenta, String verbo, URI uri, Map<String, String> encabezados) {
        Map<String, String> h = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        h.putAll(encabezados);
        String largo = h.getOrDefault("Content-Length", "");
        StringBuilder texto = new StringBuilder()
                .append(verbo).append('\n')
                .append(h.getOrDefault("Content-Encoding", "")).append('\n')
                .append(h.getOrDefault("Content-Language", "")).append('\n')
                .append("0".equals(largo) ? "" : largo).append('\n')
                .append(h.getOrDefault("Content-MD5", "")).append('\n')
                .append(h.getOrDefault("Content-Type", "")).append('\n')
                .append("").append('\n') // Date: se usa x-ms-date
                .append(h.getOrDefault("If-Modified-Since", "")).append('\n')
                .append(h.getOrDefault("If-Match", "")).append('\n')
                .append(h.getOrDefault("If-None-Match", "")).append('\n')
                .append(h.getOrDefault("If-Unmodified-Since", "")).append('\n')
                .append(h.getOrDefault("Range", "")).append('\n');

        Map<String, String> ms = new TreeMap<>();
        encabezados.forEach((nombre, valor) -> {
            String clave = nombre.toLowerCase(Locale.ROOT);
            if (clave.startsWith("x-ms-")) {
                ms.put(clave, valor.trim());
            }
        });
        ms.forEach((nombre, valor) -> texto.append(nombre).append(':').append(valor).append('\n'));

        texto.append('/').append(cuenta).append(uri.getRawPath());
        Map<String, List<String>> parametros = new TreeMap<>();
        String consulta = uri.getRawQuery();
        if (consulta != null && !consulta.isBlank()) {
            for (String par : consulta.split("&")) {
                int igual = par.indexOf('=');
                String nombre = URLDecoder.decode(igual < 0 ? par : par.substring(0, igual), StandardCharsets.UTF_8)
                        .toLowerCase(Locale.ROOT);
                String valor = igual < 0 ? "" : URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8);
                parametros.computeIfAbsent(nombre, k -> new java.util.ArrayList<>()).add(valor);
            }
        }
        parametros.forEach((nombre, valores) -> texto.append('\n').append(nombre).append(':')
                .append(String.join(",", valores.stream().sorted().toList())));
        return texto.toString();
    }
}
