package ar.edu.uai.tfi.management.domain.model;

/**
 * Operación de negocio que los servicios informan a la bitácora de transacciones.
 *
 * @param entidad        entidad afectada, por ejemplo ORGANIZACION o SEDE
 * @param entidadId      id de la entidad afectada
 * @param operacion      qué se hizo
 * @param organizacionId organización cliente a la que pertenece el dato
 * @param valorAnterior  estado antes de la operación (null en un alta); se guarda como JSON
 * @param valorNuevo     estado después de la operación; se guarda como JSON
 * @param actor          quién la realizó; si es null se toma el usuario autenticado
 * @param detalle        texto breve adicional
 */
public record Transaccion(
        String entidad,
        Long entidadId,
        OperacionTransaccion operacion,
        Long organizacionId,
        Object valorAnterior,
        Object valorNuevo,
        String actor,
        String detalle
) {

    public static Transaccion alta(String entidad, Long entidadId, Long organizacionId, Object valorNuevo) {
        return new Transaccion(entidad, entidadId, OperacionTransaccion.ALTA, organizacionId,
                null, valorNuevo, null, null);
    }
}
