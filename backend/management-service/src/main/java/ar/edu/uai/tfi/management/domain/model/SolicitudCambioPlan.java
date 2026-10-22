package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/**
 * Pedido de cambio de plan de una organización. Lo resuelve el administrador del sistema; no genera cobros.
 *
 * @param planActual     código del plan al momento de pedir el cambio
 * @param planSolicitado código del plan pedido
 * @param respuesta      comentario de quien la resolvió (obligatorio al rechazar)
 */
public record SolicitudCambioPlan(
        Long id,
        Long organizacionId,
        String planActual,
        String planSolicitado,
        ModalidadContratacion modalidad,
        EstadoSolicitudPlan estado,
        String comentario,
        String solicitadoPor,
        Instant fechaSolicitud,
        String resueltoPor,
        Instant fechaResolucion,
        String respuesta
) {

    public SolicitudCambioPlan resolver(EstadoSolicitudPlan nuevoEstado, String usuario, Instant fecha,
                                        String texto) {
        return new SolicitudCambioPlan(id, organizacionId, planActual, planSolicitado, modalidad, nuevoEstado,
                comentario, solicitadoPor, fechaSolicitud, usuario, fecha, texto);
    }
}
