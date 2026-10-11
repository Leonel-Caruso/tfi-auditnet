package ar.edu.uai.tfi.management.application.admin;

/**
 * Resultado de verificar un componente del que depende el servicio (base de datos, MongoDB).
 *
 * @param estado    UP o DOWN
 * @param latenciaMs tiempo de respuesta de la verificación
 * @param detalle   versión del esquema, base usada o el error (sin datos sensibles)
 */
public record ComponenteSalud(String nombre, String estado, long latenciaMs, String detalle) {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    public boolean disponible() {
        return UP.equals(estado);
    }
}
