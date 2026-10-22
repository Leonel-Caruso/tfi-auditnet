package ar.edu.uai.tfi.management.infrastructure.correo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Lee la cadena de conexión de Azure Communication Services: "endpoint=https://…/;accesskey=…". */
final class ConexionAcs {

    private ConexionAcs() {
    }

    static Map<String, String> leer(String conexion) {
        Map<String, String> partes = new HashMap<>();
        for (String parte : conexion.split(";")) {
            int igual = parte.indexOf('=');
            if (igual > 0) {
                // La clave en Base64 puede terminar en "=": se corta solo en el primero.
                partes.put(parte.substring(0, igual).trim().toLowerCase(Locale.ROOT), parte.substring(igual + 1).trim());
            }
        }
        if (!partes.containsKey("endpoint") || !partes.containsKey("accesskey")) {
            throw new IllegalStateException("La cadena de conexión de correo no tiene endpoint y accesskey.");
        }
        return partes;
    }
}
