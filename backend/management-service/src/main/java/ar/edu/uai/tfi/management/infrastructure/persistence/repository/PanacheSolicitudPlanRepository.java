package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.EstadoSolicitudPlan;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;
import ar.edu.uai.tfi.management.domain.repository.SolicitudPlanRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.PlanEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.SolicitudPlanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheSolicitudPlanRepository
        implements PanacheRepository<SolicitudPlanEntity>, SolicitudPlanRepository {

    @Override
    public SolicitudCambioPlan guardar(SolicitudCambioPlan solicitud) {
        SolicitudPlanEntity entity = new SolicitudPlanEntity();
        entity.organizacionId = solicitud.organizacionId();
        entity.planActual = plan(solicitud.planActual());
        entity.planSolicitado = plan(solicitud.planSolicitado());
        entity.modalidad = solicitud.modalidad();
        entity.comentario = solicitud.comentario();
        entity.solicitadoPor = solicitud.solicitadoPor();
        entity.fechaSolicitud = solicitud.fechaSolicitud();
        copiarResolucion(solicitud, entity);
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public SolicitudCambioPlan actualizar(SolicitudCambioPlan solicitud) {
        SolicitudPlanEntity entity = findByIdOptional(solicitud.id())
                .orElseThrow(() -> new NoSuchElementException("No existe la solicitud."));
        copiarResolucion(solicitud, entity);
        flush();
        return aDominio(entity);
    }

    @Override
    public Optional<SolicitudCambioPlan> buscarPorId(Long id) {
        return findByIdOptional(id).map(PanacheSolicitudPlanRepository::aDominio);
    }

    @Override
    public Optional<SolicitudCambioPlan> buscarParaResolver(Long id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE).map(PanacheSolicitudPlanRepository::aDominio);
    }

    @Override
    public List<SolicitudCambioPlan> listarPorOrganizacion(Long organizacionId) {
        return find("organizacionId = ?1 order by fechaSolicitud desc, id desc", organizacionId).list().stream()
                .map(PanacheSolicitudPlanRepository::aDominio).toList();
    }

    @Override
    public List<SolicitudCambioPlan> listarPorEstado(EstadoSolicitudPlan estado) {
        List<SolicitudPlanEntity> entidades = estado == null
                ? find("order by fechaSolicitud, id").list()
                : find("estado = ?1 order by fechaSolicitud, id", estado).list();
        return entidades.stream().map(PanacheSolicitudPlanRepository::aDominio).toList();
    }

    @Override
    public Optional<SolicitudCambioPlan> buscarPendiente(Long organizacionId) {
        return find("organizacionId = ?1 and estado = ?2", organizacionId, EstadoSolicitudPlan.PENDIENTE)
                .firstResultOptional().map(PanacheSolicitudPlanRepository::aDominio);
    }

    private PlanEntity plan(String codigo) {
        return getEntityManager().createQuery("from PlanEntity where codigo = :codigo", PlanEntity.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    private static void copiarResolucion(SolicitudCambioPlan solicitud, SolicitudPlanEntity entity) {
        entity.estado = solicitud.estado();
        entity.resueltoPor = solicitud.resueltoPor();
        entity.fechaResolucion = solicitud.fechaResolucion();
        entity.respuesta = solicitud.respuesta();
    }

    private static SolicitudCambioPlan aDominio(SolicitudPlanEntity entity) {
        return new SolicitudCambioPlan(entity.id, entity.organizacionId, entity.planActual.codigo,
                entity.planSolicitado.codigo, entity.modalidad, entity.estado, entity.comentario,
                entity.solicitadoPor, entity.fechaSolicitud, entity.resueltoPor, entity.fechaResolucion,
                entity.respuesta);
    }
}
