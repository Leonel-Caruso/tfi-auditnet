package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aviso en la bandeja de salida. Guarda el tipo y los datos; el asunto y el cuerpo se arman al enviarlo.
 *
 * @param origen       servicio que lo guardó
 * @param datos        valores del aviso (se guardan como JSON)
 * @param idEnvio      identificador que devolvió el proveedor al aceptarlo
 */
public record CorreoSaliente(
        Long id,
        String origen,
        TipoCorreo tipo,
        Long organizacionId,
        String destinatario,
        String nombreDestinatario,
        Map<String, Object> datos,
        EstadoCorreo estado,
        int intentos,
        Instant proximoIntento,
        Instant creadoEn,
        Instant enviadoEn,
        String ultimoError,
        String idEnvio
) {

    /** Datos que no se conservan después del envío (el token de recuperación solo viaja en el correo). */
    public static final String DATO_SENSIBLE = "token";
    /** Idioma elegido en la pantalla desde donde se pidió el correo; si no está, se usa el de la cuenta. */
    public static final String DATO_IDIOMA = "idioma";

    public CorreoSaliente enviado(Instant fecha, String id) {
        return new CorreoSaliente(this.id, origen, tipo, organizacionId, destinatario, nombreDestinatario,
                sinDatosSensibles(), EstadoCorreo.ENVIADO, intentos + 1, proximoIntento, creadoEn, fecha, null, id);
    }

    public CorreoSaliente reintentar(Instant proximo, String error) {
        return new CorreoSaliente(id, origen, tipo, organizacionId, destinatario, nombreDestinatario, datos,
                EstadoCorreo.PENDIENTE, intentos + 1, proximo, creadoEn, null, recortar(error), null);
    }

    public CorreoSaliente fallido(String error) {
        return new CorreoSaliente(id, origen, tipo, organizacionId, destinatario, nombreDestinatario,
                sinDatosSensibles(), EstadoCorreo.FALLIDO, intentos + 1, proximoIntento, creadoEn, null,
                recortar(error), null);
    }

    private Map<String, Object> sinDatosSensibles() {
        if (datos == null || !datos.containsKey(DATO_SENSIBLE)) {
            return datos;
        }
        Map<String, Object> copia = new LinkedHashMap<>(datos);
        copia.remove(DATO_SENSIBLE);
        return copia;
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }
}
