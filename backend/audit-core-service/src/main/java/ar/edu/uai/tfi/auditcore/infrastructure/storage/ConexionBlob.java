package ar.edu.uai.tfi.auditcore.infrastructure.storage;

import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Datos de conexión a Blob Storage a partir de una cadena de conexión de Azure:
 * {@code DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net}
 * o, para el emulador, {@code ...;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1}.
 *
 * @param endpoint URL base de la cuenta, sin barra final (ej. https://cuenta.blob.core.windows.net)
 */
public record ConexionBlob(String cuenta, byte[] clave, URI endpoint) {

    /** Cuenta y clave públicas del emulador Azurite (documentadas por Microsoft; no son un secreto). */
    public static final String AZURITE = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
            + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
            + "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1";

    public static ConexionBlob desde(String cadena) {
        if (cadena == null || cadena.isBlank()) {
            throw new IllegalArgumentException("La cadena de conexión del almacenamiento está vacía.");
        }
        Map<String, String> partes = new LinkedHashMap<>();
        for (String parte : cadena.trim().split(";")) {
            int igual = parte.indexOf('=');
            if (igual > 0) {
                partes.put(parte.substring(0, igual).trim(), parte.substring(igual + 1).trim());
            }
        }
        String cuenta = partes.get("AccountName");
        String clave = partes.get("AccountKey");
        if (cuenta == null || cuenta.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La cadena de conexión debe incluir AccountName y AccountKey.");
        }
        String endpoint = partes.get("BlobEndpoint");
        if (endpoint == null || endpoint.isBlank()) {
            String protocolo = partes.getOrDefault("DefaultEndpointsProtocol", "https");
            String sufijo = partes.getOrDefault("EndpointSuffix", "core.windows.net");
            endpoint = protocolo + "://" + cuenta + ".blob." + sufijo;
        }
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        try {
            return new ConexionBlob(cuenta, Base64.getDecoder().decode(clave), URI.create(endpoint));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La AccountKey de la cadena de conexión no es Base64 válido.");
        }
    }

    /** Dirección de un recurso: {@code endpoint/contenedor[/ruta]}. */
    public URI uri(String contenedor, String ruta, String consulta) {
        String camino = endpoint.getRawPath() + "/" + contenedor + (ruta == null ? "" : "/" + ruta);
        return URI.create(endpoint.getScheme() + "://" + endpoint.getRawAuthority() + camino
                + (consulta == null ? "" : "?" + consulta));
    }
}
