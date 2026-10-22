package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.EstadoSolicitudPlan;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;

import java.util.List;
import java.util.Optional;

public interface SolicitudPlanRepository {

    SolicitudCambioPlan guardar(SolicitudCambioPlan solicitud);

    /** Actualiza el estado y los datos de la resolución. */
    SolicitudCambioPlan actualizar(SolicitudCambioPlan solicitud);

    Optional<SolicitudCambioPlan> buscarPorId(Long id);

    /** Igual que {@link #buscarPorId} pero bloquea la solicitud: aprobar, rechazar y cancelar no se pisan. */
    default Optional<SolicitudCambioPlan> buscarParaResolver(Long id) {
        return buscarPorId(id);
    }

    /** Las más recientes primero. */
    List<SolicitudCambioPlan> listarPorOrganizacion(Long organizacionId);

    /** Las más antiguas primero (orden de atención); con estado null devuelve todas. */
    List<SolicitudCambioPlan> listarPorEstado(EstadoSolicitudPlan estado);

    Optional<SolicitudCambioPlan> buscarPendiente(Long organizacionId);
}
