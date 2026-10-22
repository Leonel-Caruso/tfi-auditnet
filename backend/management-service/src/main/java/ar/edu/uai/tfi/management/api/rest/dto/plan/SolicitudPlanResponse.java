package ar.edu.uai.tfi.management.api.rest.dto.plan;

import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;

import java.time.Instant;

public record SolicitudPlanResponse(Long id, Long organizationId, String organizacion, String planActual,
                                    String planSolicitado, String modalidad, String estado, String comentario,
                                    String solicitadoPor, Instant fechaSolicitud, String resueltoPor,
                                    Instant fechaResolucion, String respuesta) {

    public static SolicitudPlanResponse de(SolicitudCambioPlan solicitud, String organizacion) {
        return new SolicitudPlanResponse(solicitud.id(), solicitud.organizacionId(), organizacion,
                solicitud.planActual(), solicitud.planSolicitado(), solicitud.modalidad().name(),
                solicitud.estado().name(), solicitud.comentario(), solicitud.solicitadoPor(),
                solicitud.fechaSolicitud(), solicitud.resueltoPor(), solicitud.fechaResolucion(),
                solicitud.respuesta());
    }
}
