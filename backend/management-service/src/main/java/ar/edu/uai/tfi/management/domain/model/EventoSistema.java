package ar.edu.uai.tfi.management.domain.model;

/**
 * Evento de seguridad que los servicios informan a la bitácora de auditoría de sistema.
 *
 * @param tipo              qué ocurrió
 * @param resultado         si la operación se concretó o fue rechazada
 * @param actor             quién la realizó; en un login, la identidad ingresada.
 *                          Si es null se toma el usuario autenticado de la solicitud.
 * @param usuarioAfectadoId usuario sobre el que se actuó (puede ser null)
 * @param organizacionId    organización involucrada (puede ser null)
 * @param detalle           información adicional legible; nunca contraseñas ni tokens
 */
public record EventoSistema(
        TipoEventoSistema tipo,
        ResultadoEvento resultado,
        String actor,
        Long usuarioAfectadoId,
        Long organizacionId,
        String detalle
) {

    public static EventoSistema exito(TipoEventoSistema tipo, String actor, Long usuarioAfectadoId,
                                      Long organizacionId, String detalle) {
        return new EventoSistema(tipo, ResultadoEvento.EXITO, actor, usuarioAfectadoId, organizacionId, detalle);
    }

    public static EventoSistema fallo(TipoEventoSistema tipo, String actor, Long usuarioAfectadoId,
                                      Long organizacionId, String detalle) {
        return new EventoSistema(tipo, ResultadoEvento.FALLO, actor, usuarioAfectadoId, organizacionId, detalle);
    }
}
